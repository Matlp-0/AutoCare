package com.example.myapplication.ui.assistant;

import android.app.Application;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.data.ai.AssistantContextReader;
import com.example.myapplication.data.ai.LocalModelStore;
import com.example.myapplication.data.ai.NativeLlama;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AssistantViewModel extends AndroidViewModel {
    public static final class State {
        public final String status, answer;
        public final boolean busy, ready;
        State(String status, String answer, boolean busy, boolean ready) {
            this.status = status; this.answer = answer; this.busy = busy; this.ready = ready;
        }
    }
    private final AppContainer container;
    private final LocalModelStore store;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private AtomicBoolean cancellation = new AtomicBoolean();
    private volatile NativeLlama running;
    private volatile boolean cleared;
    private volatile String answer = "";

    public AssistantViewModel(@NonNull Application application) {
        super(application);
        container = AutoCareApp.container(application);
        store = container.localModelStore;
        state.setValue(new State(store.isReady() ? "IA pronta para uso offline." : "Baixe o modelo para ativar a IA local.",
                "", false, store.isReady()));
    }
    public LiveData<State> state() { return state; }
    public boolean isBusy() { return state.getValue() != null && state.getValue().busy; }

    private AtomicBoolean begin(String status) {
        cancellation = new AtomicBoolean();
        state.setValue(new State(status, answer, true, store.isReady()));
        return cancellation;
    }
    private void publish(String status, boolean busy) {
        if (!cleared) state.postValue(new State(status, answer, busy, store.isReady()));
    }
    public void transfer(Uri source) {
        if (isBusy()) return;
        AtomicBoolean cancelled = begin(source == null ? "Conectando para baixar o modelo…" : "Importando modelo…");
        container.assistantExecutor.execute(() -> {
            try {
                final long[] lastProgress = {0};
                java.util.function.LongConsumer progress = bytes -> {
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastProgress[0] > 300) {
                        lastProgress[0] = now;
                        publish((source == null ? "Baixando" : "Importando") + " modelo: "
                                + (bytes * 100 / LocalModelStore.BYTES) + "%", true);
                    }
                };
                if (cancelled.get()) return;
                if (source == null) store.download(cancelled, progress);
                else store.importModel(source, cancelled, progress);
                publish("IA pronta. As respostas funcionam sem internet.", false);
            } catch (Exception e) {
                publish(cancelled.get() ? "Transferência cancelada." : message(e), false);
            } finally {
                if (cancelled.get()) publish("Transferência cancelada.", false);
            }
        });
    }
    public void ask(long vehicleId, String question) {
        if (isBusy() || !store.isReady()) return;
        answer = "";
        AtomicBoolean cancelled = begin("Preparando os dados e carregando a IA…");
        container.assistantExecutor.execute(() -> {
            long start = SystemClock.elapsedRealtime();
            try {
                if (cancelled.get()) return;
                String prompt = new AssistantContextReader(container).prompt(vehicleId, question);
                try (NativeLlama engine = new NativeLlama()) {
                    running = engine;
                    if (cancelled.get()) { engine.cancel(); return; }
                    final long[] firstToken = {0};
                    int tokens = engine.generate(store.file().getAbsolutePath(), prompt, bytes -> {
                        if (cancelled.get()) return;
                        if (bytes.length > 0 && firstToken[0] == 0) firstToken[0] = SystemClock.elapsedRealtime();
                        answer = new String(bytes, StandardCharsets.UTF_8).trim();
                        publish("Respondendo no aparelho…", true);
                    });
                    if (cancelled.get()) return;
                    long elapsed = SystemClock.elapsedRealtime() - start;
                    Log.i("AutoCareAI", "total_ms=" + elapsed + " first_token_ms="
                            + (firstToken[0] == 0 ? -1 : firstToken[0] - start) + " tokens=" + tokens);
                    publish(answer.isEmpty() ? "O modelo não produziu uma resposta. Tente reformular a pergunta."
                            : String.format(Locale.forLanguageTag("pt-BR"), "Concluído em %.1f s.%s", elapsed / 1000d,
                            tokens >= 320 ? " Limite de resposta atingido." : ""), false);
                }
            } catch (LinkageError e) {
                publish("Motor local indisponível neste aparelho. Instale o APK com suporte à IA.", false);
            } catch (OutOfMemoryError e) {
                publish("Memória insuficiente. Feche outros aplicativos e tente novamente.", false);
            } catch (Exception e) {
                publish(cancelled.get() ? "Resposta cancelada." : message(e), false);
            } finally {
                running = null;
                if (cancelled.get()) publish("Resposta cancelada. O texto parcial pode estar incompleto.", false);
            }
        });
    }
    public void deleteModel() {
        if (isBusy()) return;
        begin("Removendo modelo…");
        container.assistantExecutor.execute(() -> {
            try { store.delete(); publish("Modelo removido. Você pode baixá-lo novamente.", false); }
            catch (Exception e) { publish(message(e), false); }
        });
    }
    public void cancel() {
        cancellation.set(true);
        NativeLlama engine = running;
        if (engine != null) engine.cancel();
        if (isBusy()) {
            State current = state.getValue();
            state.setValue(new State("Cancelando…", current.answer, true, current.ready));
        }
    }
    private static String message(Exception e) {
        return e.getMessage() == null ? "Não foi possível concluir. Tente novamente." : e.getMessage();
    }
    @Override protected void onCleared() {
        cleared = true;
        cancel();
    }
}
