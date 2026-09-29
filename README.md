# CoinLens

CoinLens is an Android coin and banknote identifier for collectors, people sorting inherited collections, and metal-detecting hobbyists. It turns photos of a coin or note into a cautious identification, an estimated value range, and a record the user can keep in a personal collection.

## Purpose

CoinLens helps people answer “What am I holding?” and “What might it be worth?” without first learning numismatics. It aims to make that first check quick and useful while being clear about uncertainty: values are estimates, condition and authenticity matter, and possible varieties are never presented as confirmed facts.

## What it does

- Captures a coin’s front and optional reverse, or imports a photo.
- Identifies coins and banknotes, including country, denomination, year, mint details, composition, rarity, and short description when visible.
- Shows circulated and uncirculated value ranges with a confidence label and possible variety hints.
- Saves scans and photos to a local cabinet, with folders, grade notes, quantity, and purchase price for collection tracking.
- Shares a scan summary as an image.
- Uses onboarding preferences to tailor the experience for collectors, inherited collections, and detectorists.
- Provides a free scan allowance and paid plans; available plans and prices come from the store configuration.

CoinLens is not an appraisal service. It does not verify authenticity or guarantee market prices. Get an in-hand appraisal before buying or selling valuable items.

## How identification works

1. The camera flow collects a clear front image and optionally a reverse image. The app downsizes images and encodes them as JPEG before inference.
2. `:core:domain` builds a coin-identification prompt and JSON response schema. The prompt asks Gemini to use value ranges, lower confidence when uncertain, and mark variety clues as possible.
3. In Firebase mode, `:core:ai` sends the images and prompt through Firebase AI Logic to the Gemini Developer API. The app parses the structured response and reports network, quota, safety, and parse errors through the normal result flow.
4. Successful scans and their photos are saved on-device. Room stores scan and collection records; DataStore holds preferences and scan allowance state. The photos are kept in app-private storage.
5. Home, cabinet, and share screens read the saved records. Collection totals use each item’s selected grade and quantity.

Photos used for live identification are sent to Firebase AI Logic/Gemini for inference. The app does not require an account or sync the collection to a CoinLens server.

## Gemini model and cost

The app uses Firebase Remote Config parameter `model_name`. Current published value and bundled fallback are both `gemini-3.5-flash-lite`. This is Google’s latest stable Gemini Flash-Lite model and is listed for cost-sensitive workloads. On 2026-09-29, Google lists input at $0.30 versus $0.75 and output at $2.50 versus $3.75 per million tokens compared with Gemini 3.8 Flash. Rates can change; see [current Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing).

The bundled default lives in `core/ai/src/main/kotlin/app/novushq/coinlens/ai/ModelConfig.kt` and `core/ai/src/main/res/xml/remote_config_defaults.xml`. Remote Config lets the project change model without releasing a new APK. Debug builds fetch each launch; release builds cache the result for up to an hour.

## App structure

CoinLens is a Kotlin Android app built with Jetpack Compose, Koin, coroutines and Flow.

| Module | Responsibility |
| --- | --- |
| `:app` | App startup, dependency injection, and navigation host |
| `:core:model`, `:core:common` | Shared domain types, result and error types |
| `:core:identify`, `:core:domain` | Reusable image-identification contracts, coin schema, repositories, and use cases |
| `:core:ai` | Firebase Gemini engine, fake engine, sample catalogue, and model configuration |
| `:core:data` | Room database, DataStore, local image storage, and repository implementations |
| `:core:designsystem`, `:core:navigation` | Shared Compose styling and typed navigation |
| `:feature:capture` | Camera and photo selection |
| `:feature:result` | Identification and value result |
| `:feature:home`, `:feature:collection`, `:feature:share` | Home, collection cabinet, and share card |
| `:feature:onboarding`, `:feature:settings`, `:feature:paywall` | Preferences, app settings, and plans |

## Build and run

Requirements: Android Studio or Android SDK, JDK configured for Gradle, and an Android device or emulator.

```sh
./gradlew :app:installDebug
```

For live Firebase/Gemini mode, add the Firebase Android configuration at `app/google-services.json` for package `app.novushq.coinlens`. The file is intentionally git-ignored and must not be committed. Firebase AI Logic, App Check, and the model configuration are described in [Firebase Gemini setup](docs/FIREBASE_GEMINI_SETUP.md).

Without Firebase configuration, CoinLens uses a deterministic sample-data engine. Force that mode even when Firebase is configured with:

```sh
./gradlew :app:installDebug -Pcoinlens.ai=fake
```

In Firebase mode, debug builds use the registered App Check debug token; release builds use Play Integrity. A release build installed directly from an APK needs the appropriate App Check setup. Play signing fingerprints are distinct from debug fingerprints.

## Project docs

- [Product requirements](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Modules](docs/MODULES.md)
- [Data flow](docs/DATA_FLOW.md)
- [Firebase and Gemini setup](docs/FIREBASE_GEMINI_SETUP.md)
- [Release setup](docs/RELEASE_SETUP.md)
