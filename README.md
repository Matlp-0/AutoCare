# AutoCare

App Android para acompanhar revisões, manutenções, abastecimentos e custos do carro. Os dados ficam no aparelho (Room/SQLite); a internet é usada apenas, e de forma opcional, para buscar o plano de revisões do fabricante.

- **Package:** `com.example.myapplication` · **versionName** 1.5 (`versionCode` 2)
- **minSdk** 33 · **targetSdk / compileSdk** 37 · **Java 11** (toolchain 25)
- Linguagem: Java, Views XML (sem Compose), arquitetura MVVM + repositórios

## Funcionalidades

| Área | O que faz |
|---|---|
| Onboarding | Cadastro do veículo (marca, modelo, ano, motorização, combustível, câmbio, km) e identificação do plano de revisões |
| Plano do fabricante | Busca a tabela pública de revisões na web a partir de marca/modelo; sem rede ou sem resultado, cai no plano local. Nada é inventado |
| Início | Saúde do veículo, próximas manutenções, atualização de quilometragem |
| Manutenção | Registro manual de serviços com itens, custo, oficina e data |
| Cronograma | Linha do tempo de revisões (feitas, próxima, atrasadas, sem registro) |
| Combustível | Abastecimentos, consumo (km/l) e custo — consumo medido só entre dois tanques cheios |
| Financeiro | Custo por km juntando manutenção + combustível, com gráfico mensal |
| Importar nota | NF-e XML (parser direto) ou PDF/foto (OCR ML Kit) → sugestão de itens → tela de confirmação. Nada é gravado sem o usuário confirmar |
| Exportar | Histórico completo de manutenções em PDF (A4) na pasta Downloads |
| Lembretes | Verificação diária via WorkManager, sobrevive a reboot e respeita Doze |

## Estrutura

```
app/src/main/java/com/example/myapplication/
├── AutoCareApp.java          # Application; cria o AppContainer
├── AppContainer.java         # service locator — trocar implementação só aqui
├── data/
│   ├── local/                # Room: AppDatabase (v5, autocare.db), entity/, dao/, relation/
│   ├── remote/HttpFetcher    # HTTP simples para o plano do fabricante
│   └── repository/           # Vehicle, Maintenance, Plan, Schedule, Fuel, Odometer
├── domain/                   # regras de negócio, sem dependência de Android (testáveis)
│   ├── scheduler/            # MaintenanceScheduler, VehicleHealthCalculator, RevisionTimelineBuilder
│   ├── fuel/                 # FuelStatsCalculator, UsageEstimator, KmReminderPolicy
│   ├── finance/              # CostCalculator (custo por km)
│   ├── document/             # OCR + NFe XML → ExtractedInvoice, MaintenanceInterpreter
│   ├── manual/               # providers do plano: Web → IA (não integrada) → local
│   ├── export/               # MaintenanceHistoryPdf
│   └── model/                # enums e modelos de apresentação
├── notification/             # WorkManager: ReminderScheduler, ReminderChecker, Notifier
├── ui/                       # main, home, maintenance, schedule, fuel, car, history,
│   │                         # finance, importinvoice, onboarding
│   ├── carbon/               # design system (tema, tabs, barra de saúde, divisores)
│   └── hud/                  # componentes visuais (painel chanfrado, tags, barras)
└── util/                     # AppExecutors, AppPreferences, DateUtils, Formatters, ImageStore
```

Navegação: `WelcomeActivity` → `VehicleFormActivity` → `VehicleIdentificationActivity` → `MainActivity` (bottom nav: Início, Manutenção, Cronograma, Combustível, Carro).

## Dependências principais

AndroidX (appcompat, activity, fragment, recyclerview, constraintlayout, lifecycle) · Material Components · **Room** 2.8.4 · **WorkManager** 2.10.5 · **ML Kit Text Recognition** 16.0.1 · **jsoup** 1.23.1 · JUnit 4 / Espresso.

Versões centralizadas em `gradle/libs.versions.toml`.

## Build

Requer JDK 17+ (o Gradle baixa a toolchain 25 via foojay) e o Android SDK. Ajuste `sdk.dir` em `local.properties`.

```bash
./gradlew assembleDebug        # APK debug
./gradlew assembleRelease      # APK release
./gradlew installDebug         # instala no dispositivo conectado
```

## Testes

```bash
./gradlew test                 # testes unitários (JVM) — domain/
./gradlew connectedAndroidTest # instrumentados — precisa de device/emulador
```

Cobertura unitária: scheduler, timeline de revisões, consumo, política de lembrete de km, custo, interpretador de manutenção e parser do plano.

## Permissões

| Permissão | Motivo |
|---|---|
| `POST_NOTIFICATIONS` | Lembretes de manutenção |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Buscar o plano do fabricante (opcional — o app funciona offline) |

Câmera é `required="false"`: usada só para fotografar notas na importação.

## Notas

- `AiVehicleManualProvider` é um espaço reservado e devolve `null`. Integrá-lo exige chave de API, que **não deve ser embutida no APK** — o caminho seguro é o app chamar um backend próprio que guarda a chave.
- Saída de OCR é sempre sugestão: passa pela tela de confirmação antes de virar registro.
- Fontes empacotadas: Barlow Condensed e JetBrains Mono (licenças OFL em `app/licenses/`).
