# Assistente local — prova de conceito

## Testar no S24 (8 GB)

1. Compile/instale o APK debug e abra **Carro → Assistente de manutenção**.
2. Toque em **Baixar modelo · 1,1 GB** (preferencialmente no Wi-Fi). Mantenha a tela aberta. O download é opcional e cancelável.
3. Como alternativa, baixe o [arquivo exato Qwen3-1.7B-Q4_K_M.gguf](https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/d7f544eead698dbd1f15126ef60b45a1e1933222/Qwen3-1.7B-Q4_K_M.gguf) no navegador e use **Importar arquivo do modelo**.
4. Desative a internet e teste **Explicar próximas manutenções**, **Resumir histórico** e uma pergunta escrita.
5. Teste **Cancelar**, sair da tela durante a resposta e girar o aparelho. Girar mantém a operação; sair cancela e libera o modelo quando o processamento termina.
6. **Remover modelo** libera o armazenamento sem apagar veículos ou manutenções.

O APK contém o motor, não os pesos de 1,1 GB. Esta versão aceita somente o arquivo acima, validado por tamanho e SHA-256. Downloads interrompidos recomeçam; não há retomada nem serviço em segundo plano. O cancelamento durante uma leitura de rede pode aguardar o timeout de até 15 segundos.

## Implementação

- Java/XML e MVVM, integrada pelo `AppContainer`; fila própria para não bloquear o banco nem a interface.
- llama.cpp **b6900**, revisão `c22473b580807929fd9e3a3344a48e8cfbe6c88f`, via CMake/NDK e JNI. Fonte e checksum fixos; primeiro build precisa de internet.
- Android NDK `28.2.13676358`, CMake `3.22.1`; ABIs `arm64-v8a` (S24) e `x86_64` (emulador). Baseline CPU com 4 threads, sem depender de GPU/NPU específica. Bibliotecas alinhadas para páginas de 16 KB.
- Modelo Qwen3 1.7B Q4_K_M distribuído por Unsloth, revisão `d7f544eead698dbd1f15126ef60b45a1e1933222`.
- Arquivo: **1.107.409.472 bytes**, SHA-256 `b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897`.
- Contexto nativo: 4096 tokens; saída: até 320 tokens; ChatML Qwen3 com modo de raciocínio prolongado desativado.
- Cada pergunta é independente. O modelo é carregado por pergunta e liberado ao terminar, inclusive em cancelamentos/erros; isso aumenta a latência inicial, mas evita memória retida quando o assistente não está em uso.
- Contexto inclui os dados técnicos do veículo solicitado, até 8 registros recentes e até 8 itens do cronograma já calculado. Exclui placa, apelido, oficina e notas pessoais. Descrições livres ainda podem conter dados inseridos pelo usuário, mas são processadas localmente.
- Sem escrita de respostas no Room e sem chamada à IA remota. O download usa HTTPS; os dados do carro não integram essa requisição.
- Modelo em `no_backup/ai/`, excluído de backups. Arquivo temporário e substituição atômica após verificação; falhas não substituem um modelo válido.

Os intervalos do app podem incluir referências genéricas do histórico, mesmo quando há plano salvo; o prompt informa essa limitação. Não foi alterado o cálculo do cronograma. O modelo pode errar, ignorar instruções ou responder fora do escopo: o prompt não é uma garantia de precisão nem uma validação mecânica. Esta prova de conceito não busca manuais, não é diagnóstico e não usa RAG/fine-tuning.

## Verificação

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

`LocalAiTest` cobre arquivos corrompidos, truncados, maiores que o esperado, cancelamento, preservação do modelo anterior, limites de contexto e exclusão de campos pessoais. `LocalAiNativeTest` verifica JNI, erros e recriação da tela. A geração com modelo real é opcional; depois de baixar/importar no aparelho:

```sh
adb shell am instrument -w \
  -e class com.example.myapplication.LocalAiNativeTest \
  -e localAiModelTest true \
  com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner
```

Para avaliar o S24, compare tempo até o primeiro texto, tempo total, memória durante a resposta e aquecimento após perguntas repetidas. Os logs `AutoCareAI` registram tempos e contagem de tokens, sem perguntas ou respostas. A tela mostra o tempo total, incluindo carga do modelo. O teste instrumentado opcional registra apenas a resposta do cenário sintético.

```sh
adb logcat -s AutoCareAI
adb shell dumpsys meminfo com.example.myapplication
```

Números do emulador não representam o S24. Ainda é necessário medir no aparelho físico e avaliar respostas com casos reais, especialmente quando há pouco histórico.

## Resultado desta preparação

- APK debug e APK de testes compilados, incluindo bibliotecas nativas ARM64 e x86_64.
- 52 testes unitários passaram (incluindo 6 testes novos da IA local).
- `lintDebug` detectou um erro preexistente `MissingPermission` em `MaintenanceNotifier.java:120`; nenhum erro de lint foi apontado nos arquivos novos.
- Testes instrumentados e inferência real **não foram concluídos**. O emulador foi encerrado a pedido do usuário; o teste no S24 permanece pendente.

## Licenças

[llama.cpp](https://github.com/ggml-org/llama.cpp/tree/c22473b580807929fd9e3a3344a48e8cfbe6c88f) usa MIT; o aviso acompanha o APK em `assets/licenses/llama.cpp-MIT.txt`. O [Qwen3 1.7B](https://huggingface.co/Qwen/Qwen3-1.7B) usa Apache-2.0; a [distribuição GGUF](https://huggingface.co/unsloth/Qwen3-1.7B-GGUF) e sua licença estão acessíveis na tela do assistente. Os pesos são baixados separadamente.
