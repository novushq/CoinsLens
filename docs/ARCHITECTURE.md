# CoinLens — architecture

Kotlin, Compose, Koin, coroutines/Flow, Room, DataStore. Package root `com.novushq.coinlens`. Business logic lives in pure-Kotlin core modules; features never import each other.

## Modules

| module | type | owns |
|---|---|---|
| `:core:model` | JVM | domain data classes & enums (below) |
| `:core:common` | JVM | `AppResult`, `AppError`, dispatchers |
| `:core:identify` | JVM | **reusable Identify Engine contracts** (no coin knowledge) |
| `:core:domain` | JVM | `CoinIdentificationSpec`, repository interfaces, use cases |
| `:core:ai` | Android lib | `FirebaseGeminiEngine`, `FakeIdentifyEngine` + sample catalogue, Remote Config model name, engine selection |
| `:core:data` | Android lib | Room DB, DataStore prefs, `ImageStore`, repository impls, fake entitlement default |
| `:core:designsystem` | Android lib | theme, tokens, shared components (`CoinFrame`, `ValueRangeBar`, `ConfidencePill`, `PossibleHintChip`, `UiStateSurface`) |
| `:core:navigation` | Android lib | typed `@Serializable` routes + `AppNavigator` |
| `:feature:*` | Android lib | one per task below |
| `:app` | app | Application, Koin start, NavHost |

`:core:identify`, `:core:ai` and `:core:database` style modules are created by the **core** step (not by the plan), and registered in `settings.gradle.kts` there.

## Identify Engine (shared with StoneID and ThriftScan)

```kotlin
// :core:identify — pure Kotlin
enum class ImageRole { PRIMARY, SECONDARY }            // coin: obverse, reverse
data class ImageInput(val bytes: ByteArray, val mimeType: String = "image/jpeg", val role: ImageRole)
sealed interface FieldSchema { val description: String?; val nullable: Boolean
  data class Obj(val properties: Map<String, FieldSchema>, val optional: List<String> = emptyList(), ...)
  data class Arr(val items: FieldSchema, ...) ; data class Str(...) ; data class Enum(val values: List<String>, ...)
  data class Integer(...) ; data class Number(...) ; data class Bool(...) }
interface IdentifySpec<T> {
  val systemInstruction: String
  fun userPrompt(images: List<ImageInput>): String
  val schema: FieldSchema.Obj
  fun parse(json: String): AppResult<T>              // kotlinx.serialization, lenient, validates ranges
}
interface IdentifyEngine { suspend fun <T> identify(spec: IdentifySpec<T>, images: List<ImageInput>): AppResult<T> }
```

- `:core:ai` `FirebaseGeminiEngine` maps `FieldSchema` → `com.google.firebase.ai.type.Schema` (`Schema.obj(properties, optionalProperties)`, `Schema.array`, `Schema.string`, `Schema.enumeration`, `Schema.integer`, `Schema.double`, `Schema.boolean`), builds `generationConfig { responseMimeType = "application/json"; responseSchema = … }`, sends `content { image(bitmap) …; text(prompt) }` via `Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(modelName, generationConfig, systemInstruction = content { text(...) })`. Images downscaled to ≤1600 px long edge, JPEG q85 before sending (20 MB request cap). Errors map to `AppError` (network, quota/429, blocked/safety, parse).
- Model name from Firebase Remote Config key `model_name`, default `gemini-3.8-flash` (defaults XML in `:core:ai` res/xml), fetched at startup, never blocking a scan.
- `FakeIdentifyEngine` returns a sample-catalogue entry chosen deterministically from the image bytes hash (so tests are stable), after a small configurable delay. Catalogue (≥8 entries, JSON asset in `:core:ai` or Kotlin constants): 1943 Lincoln steel cent, 1955 doubled-die Lincoln cent (hint: doubled die – possible), 1909-S VDB Lincoln cent, 2004-D Wisconsin quarter extra leaf (possible), 1921 Morgan dollar, 1964 Kennedy half (90% silver), UK 1983 2p "New Pence", India 2 rupee 2011, US $2 note 2003 (banknote), Canada 2012 penny, plus one `unrecognized` case.
- Engine selection: `BuildConfig.AI_MODE` = `"firebase"` only when `app/google-services.json` exists **and** gradle property `coinlens.ai` ≠ `fake`; otherwise `"fake"`. The google-services plugin is applied only when the file exists, so the project builds and runs with no Firebase project. In firebase mode, App Check debug provider in debug builds (`firebase-appcheck-debug`), Play Integrity in release.

## Core model (`:core:model`)
`ItemKind {COIN, BANKNOTE}`, `Rarity {COMMON, SCARCE, RARE, VERY_RARE, UNKNOWN}`, `Confidence {HIGH, MEDIUM, LOW}`, `Money(cents: Long, currency: String = "USD")`, `ValueRange(low: Money, high: Money)`, `ValueEstimate(circulated: ValueRange?, uncirculated: ValueRange?, confidence: Confidence, basis: String)`, `VarietyHint(name, description, whereToLook)` (always rendered as "Possible …"), `CoinIdentification(kind, name, country, denomination, year: Int?, yearText, mintMark?, mint?, composition?, weightGrams?, diameterMm?, rarity, description, value: ValueEstimate, hints: List<VarietyHint>, alternatives: List<String>, recognized: Boolean)`, `ScanRecord(id, createdAt: Instant, obversePath, reversePath?, identification)`, `Folder(id, name, createdAt, itemCount, totalValue)`, `Grade {POOR, FAIR, ABOUT_GOOD, GOOD, VERY_GOOD, FINE, VERY_FINE, EXTREMELY_FINE, ABOUT_UNCIRCULATED, MINT_STATE, PROOF}` with `isUncirculated`, `CollectionItem(id, scanId, folderId?, grade?, gradeNotes, purchasePrice: Money?, quantity, addedAt)`, `CollectionSummary(itemCount, totalLow, totalHigh, totalMid, totalCost, byFolder)`, `Persona {COLLECTOR, INHERITED, DETECTORIST}`, `ScanAllowance(isPro, freeRemaining, bonusRemaining) { canScan }`.

## Domain (`:core:domain`)
- `CoinIdentificationSpec : IdentifySpec<CoinIdentification>` — prompt tells the model to be conservative: ranges not points, `LOW` confidence when unsure, hints only when visible evidence exists, `recognized=false` for non-coins.
- Repositories: `ScanRepository` (observeRecent, get(id), save, delete), `CollectionRepository` (folders CRUD, items CRUD, observeSummary, observeFolder(id)), `ImageStore` (save(bytes): path, load, delete), `PreferencesRepository` (persona, onboardingDone, freeScansUsed, bonusScans), `EntitlementRepository` (`isPro: Flow<Boolean>`, `refresh()`, `restore(): AppResult<Boolean>`).
- Use cases: `ObserveScanAllowanceUseCase` (free scans = 1), `IdentifyCoinUseCase(images)` → checks allowance → saves photos → engine → persists `ScanRecord` → consumes free/bonus allowance **only on success** → returns scan id, `GrantBonusScanUseCase`, `ComputeCollectionValueUseCase` (grade-aware: uncirculated range for AU/MS/PR, circulated otherwise, midpoint × quantity), `AddToCollectionUseCase`, `CompleteOnboardingUseCase`.

## Data (`:core:data`)
Room `CoinLensDatabase` v1, `exportSchema = true` (schemas in `core/data/schemas`), tables `scans` (identification stored as JSON column via TypeConverter), `folders`, `collection_items` (FK scan, FK folder nullable, ON DELETE SET NULL for folder, CASCADE for scan). DataStore Preferences for prefs. Default Koin binding `EntitlementRepository` → `LocalEntitlementRepository` (isPro false, debug-only toggle); `:feature:paywall` overrides it with the RevenueCat implementation.

## Wiring rule that keeps parallel work conflict-free
The core step writes, **inside each scaffolded feature module**, a stub `fun NavGraphBuilder.<key>Graph(navigator: AppNavigator)` and `val <key>Module = module { }` in `feature/<key>/.../<Key>Entry.kt`, and `:app` calls every one of them (NavHost + `startKoin { modules(core…, captureModule, …) }`, feature modules after core so overrides win). Feature tasks then only edit files inside their own `feature/<key>/**` fence; they never touch `:app` or another feature. Cross-feature navigation goes only through `AppNavigator` and routes in `:core:navigation`.

## Data flow
Capture → `ScanDraft` (in-memory holder in `:core:data`, obverse/reverse bytes) → Result screen calls `IdentifyCoinUseCase` → Room → Home/Collection observe Flows → Share renders card from a `ScanRecord`.

## Dependencies (verified 2026-09-27)
Firebase BoM `34.19.0` (`firebase-ai`, `firebase-config`, `firebase-appcheck-debug`, `firebase-appcheck-playintegrity`), plugin `com.google.gms.google-services` `4.5.0`; CameraX `1.6.2` (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-compose`); RevenueCat `10.15.1` (`com.revenuecat.purchases:purchases`); Google Mobile Ads `25.5.0` (`play-services-ads`). All go in `gradle/libs.versions.toml`.
