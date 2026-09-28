# Local demo and Play release setup

Clean checkout runs with the bundled coin catalogue, local demo checkout, and a simulated rewarded ad. No credentials are needed to build or exercise the core flow.

## Enable live identification

1. Add the Firebase Android app config at `app/google-services.json` (file is git-ignored).
2. Set `coinlens.ai=firebase` as a Gradle property, or run Gradle with `-Pcoinlens.ai=firebase`.
3. Register the debug App Check token for development. Register Play Integrity for the production Firebase app.

Without that file, app stays on deterministic sample coins. `-Pcoinlens.ai=fake` forces sample mode even when Firebase config exists.

## Enable subscriptions and rewarded ads

Set these environment variables in the build environment, or equivalent Gradle properties:

| Environment variable | Gradle property | Purpose |
|---|---|---|
| `REVENUECAT_API_KEY` | `revenuecat.apiKey` | RevenueCat public Android SDK key |
| `ADMOB_APP_ID` | `admob.appId` | AdMob app ID |
| `ADMOB_REWARDED_UNIT_ID` | `admob.rewardedUnitId` | Rewarded ad unit ID |
| `COINLENS_TERMS_URL` | `coinlens.termsUrl` | Published terms of service URL |
| `COINLENS_PRIVACY_URL` | `coinlens.privacyUrl` | Published privacy policy URL |

Configure RevenueCat offering with weekly, annual, and lifetime packages and a `pro` entitlement. Configure the annual package with the intended trial in Play Console and RevenueCat; CoinLens reads its price and trial from the store package. AdMob app ID defaults to Google's test ID for local builds; supply your production ID and rewarded unit before release.

Without RevenueCat credentials, paywall uses clearly labelled sample prices and a local-only demo entitlement. Without a rewarded unit ID, its button simulates a short ad. Neither mode bills users or serves real ads.

## Sign and build release

Create a Play upload keystore outside the repository. Set all four signing variables in the release environment:

`COINLENS_STORE_FILE`, `COINLENS_STORE_PASSWORD`, `COINLENS_KEY_ALIAS`, `COINLENS_KEY_PASSWORD`.

Partial signing configuration fails during Gradle configuration. With the service setup above and signing variables set, build the Play bundle:

```sh
./gradlew testDebugUnitTest lintDebug bundleRelease
```

Keep service keys and keystore credentials out of Git. Do not use the demo checkout, sample AdMob ID, or fake identification mode for a public release.
