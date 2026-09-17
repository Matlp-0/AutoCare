#include <jni.h>
#include <llama.h>
#include <atomic>
#include <algorithm>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
struct Request { std::atomic<bool> cancelled{false}; };
using Model = std::unique_ptr<llama_model, decltype(&llama_model_free)>;
using Context = std::unique_ptr<llama_context, decltype(&llama_free)>;
using Sampler = std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)>;
constexpr int CONTEXT = 4096;
constexpr int OUTPUT = 320;
constexpr int BATCH = 128;

bool aborted(void * data) { return static_cast<Request *>(data)->cancelled.load(); }
bool loading(float, void * data) { return !aborted(data); }
void check(Request * request) {
    if (aborted(request)) throw std::runtime_error("Operação cancelada.");
}
void fail(JNIEnv * env, const char * message) {
    if (!env->ExceptionCheck()) env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}
std::string utf8(JNIEnv * env, jbyteArray bytes) {
    const auto size = env->GetArrayLength(bytes);
    std::string value(size, '\0');
    env->GetByteArrayRegion(bytes, 0, size, reinterpret_cast<jbyte *>(value.data()));
    return value;
}
void publish(JNIEnv * env, jobject listener, jmethodID method, const std::string & text) {
    auto bytes = env->NewByteArray(static_cast<jsize>(text.size()));
    if (!bytes) throw std::runtime_error("Memória insuficiente para a resposta.");
    env->SetByteArrayRegion(bytes, 0, text.size(), reinterpret_cast<const jbyte *>(text.data()));
    env->CallVoidMethod(listener, method, bytes);
    env->DeleteLocalRef(bytes);
    if (env->ExceptionCheck()) throw std::runtime_error("Falha ao exibir a resposta.");
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_myapplication_data_ai_NativeLlama_create(JNIEnv * env, jclass) {
    try {
        static std::once_flag initialized;
        std::call_once(initialized, [] { llama_backend_init(); });
        return reinterpret_cast<jlong>(new Request());
    } catch (const std::exception & e) { fail(env, e.what()); return 0; }
}
extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_data_ai_NativeLlama_cancelNative(JNIEnv *, jclass, jlong handle) {
    reinterpret_cast<Request *>(handle)->cancelled.store(true);
}
extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_data_ai_NativeLlama_destroy(JNIEnv *, jclass, jlong handle) {
    delete reinterpret_cast<Request *>(handle);
}
extern "C" JNIEXPORT jint JNICALL
Java_com_example_myapplication_data_ai_NativeLlama_generateNative(
        JNIEnv * env, jclass, jlong handle, jbyteArray pathBytes, jbyteArray promptBytes, jobject listener) {
    auto request = reinterpret_cast<Request *>(handle);
    try {
        check(request);
        const auto path = utf8(env, pathBytes);
        const auto prompt = utf8(env, promptBytes);
        auto params = llama_model_default_params();
        params.n_gpu_layers = 0; // Portable CPU baseline for both S24 chip variants.
        params.progress_callback = loading;
        params.progress_callback_user_data = request;
        Model model(llama_model_load_from_file(path.c_str(), params), llama_model_free);
        check(request);
        if (!model) throw std::runtime_error("Não foi possível abrir o modelo. Verifique a memória disponível.");
        char architecture[64]{};
        llama_model_meta_val_str(model.get(), "general.architecture", architecture, sizeof(architecture));
        if (std::string(architecture) != "qwen3" || llama_model_n_params(model.get()) > 2000000000ULL)
            throw std::runtime_error("Use o modelo Qwen3 1.7B indicado pelo aplicativo.");
        const auto vocab = llama_model_get_vocab(model.get());
        int count = -llama_tokenize(vocab, prompt.data(), prompt.size(), nullptr, 0, true, true);
        if (count <= 0 || count > CONTEXT - OUTPUT)
            throw std::runtime_error("Contexto muito longo. Faça uma pergunta mais curta.");
        std::vector<llama_token> tokens(count);
        if (llama_tokenize(vocab, prompt.data(), prompt.size(), tokens.data(), count, true, true) != count)
            throw std::runtime_error("Não foi possível preparar a pergunta.");
        auto cp = llama_context_default_params();
        cp.n_ctx = CONTEXT;
        cp.n_batch = BATCH;
        cp.n_ubatch = BATCH;
        cp.n_threads = 4;
        cp.n_threads_batch = 4;
        cp.abort_callback = aborted;
        cp.abort_callback_data = request;
        Context context(llama_init_from_model(model.get(), cp), llama_free);
        if (!context) throw std::runtime_error("Memória insuficiente para iniciar a IA.");
        for (int position = 0; position < count; position += BATCH) {
            check(request);
            auto batch = llama_batch_get_one(tokens.data() + position, std::min(BATCH, count - position));
            int result = llama_decode(context.get(), batch);
            check(request);
            if (result != 0) throw std::runtime_error("Falha ao processar o contexto.");
        }
        Sampler sampler(llama_sampler_init_greedy(), llama_sampler_free);
        auto cls = env->GetObjectClass(listener);
        auto method = env->GetMethodID(cls, "onText", "([B)V");
        env->DeleteLocalRef(cls);
        if (!method) throw std::runtime_error("Falha na integração da IA.");
        std::string text;
        int generated = 0;
        for (; generated < OUTPUT; ++generated) {
            check(request);
            auto token = llama_sampler_sample(sampler.get(), context.get(), -1);
            if (llama_vocab_is_eog(vocab, token)) break;
            std::vector<char> piece(256);
            int size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
            if (size < 0) {
                piece.resize(-size);
                size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
            }
            if (size < 0) throw std::runtime_error("Falha ao decodificar a resposta.");
            text.append(piece.data(), size);
            // Pass UTF-8 bytes, not JNI modified UTF-8 (accents and emoji remain intact).
            if (generated % 8 == 0) publish(env, listener, method, text);
            auto batch = llama_batch_get_one(&token, 1);
            int result = llama_decode(context.get(), batch);
            check(request);
            if (result != 0) throw std::runtime_error("Falha durante a resposta.");
        }
        publish(env, listener, method, text);
        return generated;
        // Model, context and sampler are freed after each question, including errors/cancellation.
    } catch (const std::exception & e) { fail(env, e.what()); return 0; }
}
