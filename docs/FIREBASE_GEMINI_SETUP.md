# Enable Firebase + Gemini identification

CoinLens identifies coins with Gemini through **Firebase AI Logic** (Gemini Developer API backend, `GenerativeBackend.googleAI()`). Without a Firebase config the app runs in **fake mode** on bundled sample coins. This guide switches it to live mode.

## How the app decides

`app/build.gradle.kts` picks the mode at build time:

| Condition | `BuildConfig.AI_MODE` |
|---|---|
| `app/google-services.json` exists **and** `-Pcoinlens.ai=fake` not passed | `firebase` (live Gemini) |
| file missing, or `-Pcoinlens.ai=fake` passed | `fake` (sample catalogue) |

Settings → "AI mode" shows the active mode and model name, so you can confirm at a glance.

## 1. Firebase project

1. Open the [Firebase console](https://console.firebase.google.com) → create or select a project.
2. **Project settings → Your apps → Add app → Android**.
   - Package name: `app.novushq.coinlens`
   - Add your debug SHA-1 (`./gradlew signingReport`) and later your Play upload / app-signing SHA-1.
3. Download `google-services.json` and place it at `app/google-services.json`. The file is git-ignored; never commit it.

## 2. Enable Firebase AI Logic (Gemini Developer API)

1. Console → **Build → AI Logic** → **Get started**.
2. Choose **Gemini Developer API** (not Vertex AI; the code uses `GenerativeBackend.googleAI()`).
3. Let the console create the Gemini API key. Firebase keeps it server-side and adds it to the project's restricted-keys list; it is **not** put in the app.
4. The Gemini Developer API has a free tier with rate limits. For production traffic upgrade the project to the **Blaze** plan.

## 3. Model name (Remote Config)

The model is not hard-coded at call time. `RemoteModelConfig` reads Remote Config key `model_name`:

- Bundled default: `core/ai/src/main/res/xml/remote_config_defaults.xml` and `DEFAULT_MODEL_NAME` in `ModelConfig.kt`.
- Override live: Console → **Run → Remote Config** → add parameter `model_name` (string) → publish.
- Debug builds refetch every launch; release builds cache for 1 hour.

**Current model:** `gemini-3.5-flash-lite`, a stable, cost-efficient multimodal model that supports this app's image input and structured JSON output. It is the bundled default and is published as the Firebase Remote Config `model_name` value. On 2026-09-29, Google lists input at $0.30 versus $0.75 and output at $2.50 versus $3.75 per million tokens compared with Gemini 3.8 Flash. Check [current pricing](https://ai.google.dev/gemini-api/docs/pricing) before estimating spend, since rates can change. The model ID can be changed in Remote Config without a new app release.

## 4. App Check

The app installs App Check at startup in live mode.

**Debug builds** (debug provider):
1. Run the debug app once and scan a coin.
2. In Logcat filter `DebugAppCheckProvider`; copy the printed debug token.
3. Console → **Build → App Check → Apps → your app → ⋮ → Manage debug tokens** → add it.

**Release builds** (Play Integrity):
1. Link the Firebase project to Google Play (Console → Project settings → Integrations → Google Play).
2. Console → **App Check → Apps →** register the app with **Play Integrity**.
3. Play Integrity only attests apps installed from Play (internal testing track works). Sideloaded release APKs will be rejected once enforcement is on.

Start with App Check in **metrics only** mode, confirm traffic shows as verified, then click **Enforce** for AI Logic.

## 5. Build and run

```sh
./gradlew :app:installDebug
```

Open **Settings → AI mode**: it should read live/Firebase with the model name. Then scan a coin.

To force sample mode with a config file present: `./gradlew :app:installDebug -Pcoinlens.ai=fake`.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| AI mode shows fake | `google-services.json` missing at `app/` or `-Pcoinlens.ai=fake` set | Add the file, do a clean rebuild |
| "identification service is unavailable" (`ServerException`) | AI Logic not enabled, wrong model ID, or App Check rejecting | Do step 2, check `model_name`, check Logcat for `403` / App Check messages |
| App Check 403 in debug | Debug token not registered | Step 4, debug builds |
| Quota / "try again later" (`429`, `RESOURCE_EXHAUSTED`) | Free-tier rate limit | Wait, or move to Blaze |
| "returned no result" / parse error | Model ignored the JSON schema, or safety block | Use a current Flash-class model; retry with a clearer photo |
| Works in debug, fails in release | Play Integrity not set up or app not installed from Play | Step 4, release builds |

## Before publishing

- Use Play Integrity, not the debug provider, in release (already how the build variants are split).
- Set a Firebase budget alert and an App Check enforced state.
- Never ship with `-Pcoinlens.ai=fake`.
