# CoinLens — product requirements

## Problem
People who hold coins or banknotes — hobby collectors, people who inherited a jar of coins, metal-detecting hobbyists — want to know *what it is* and *what it is worth* without learning numismatics. Rival apps (CoinSnap ≈ $400k/month on Android US) show several ads before the first result, which drives 1-star reviews.

## Wedge
- First scan free, **zero ads before the first result**.
- A clean result card with **honest value ranges** and an explicit **confidence label** plus disclaimer.
- A shareable "what's it worth" card for reels and social.

## Users (onboarding quiz personas)
| persona | who | what the paywall copy stresses |
|---|---|---|
| `COLLECTOR` | builds a collection deliberately | unlimited scans, folders, grade notes, collection value |
| `INHERITED` | has a jar / album from family | "find the valuable ones", honest ranges, share with family |
| `DETECTORIST` | finds coins in the field | ID worn/old coins, error & variety hints, find log |

## MVP features
1. **Camera capture** with obverse/reverse guide (circular overlay, two-step flow: front then back, back is skippable), crop to the guide circle, **glare warning** (over-exposed highlight ratio) and blur warning; gallery import as alternative.
2. **AI identification** (Firebase AI Logic, Gemini): kind (coin/banknote), country, denomination, year, mint mark and mint, composition, rarity, short description, up to 3 alternatives.
3. **Value range**: circulated low–high and uncirculated low–high in USD, with confidence `HIGH | MEDIUM | LOW` and a fixed disclaimer ("Estimates only. Condition, authenticity and market change the value. Get an in-hand appraisal before selling.").
4. **Error & variety hints** (doubled die, off-centre strike, repunched mint mark, clipped planchet, …) always phrased as **"Possible"**, never as a certainty.
5. **Collections** (Room): folders, add scan to folder, grade (Poor…Mint State), grade notes, purchase price, quantity; **total value dashboard** (sum of midpoint of the grade-appropriate range, plus low–high total).
6. **Share card** image (coin photo + name + year + value range + confidence + app mark) via the system share sheet.
7. **Onboarding quiz** (collector / inherited / detectorist) that tailors paywall copy; camera permission priming.

## Monetization (policy `subscription_ads`)
Hard paywall after **1 free scan**. Plans: **Weekly $4.99**, **Annual $29.99 with 3-day trial**, **Lifetime $59.99** (prices shown from the store package, never hardcoded). Payers see **no ads ever**. Free users may watch **one rewarded ad to unlock one extra scan** (opt-in button on the paywall; no banners, no interstitials). Restore purchases reachable from paywall and settings.

## AI & data
- Gemini via **Firebase AI Logic** (`com.google.firebase:firebase-ai`, Gemini Developer API backend), model name from **Remote Config** param `model_name`, default `gemini-3.8-flash`. JSON structured output with a response schema.
- **Dummy data for testing**: when `app/google-services.json` is absent or `-Pcoinlens.ai=fake`, a deterministic `FakeIdentifyEngine` returns entries from a bundled sample catalogue. All unit tests use fakes; no test hits the network.
- All user data local (Room + DataStore). Photos stored in app-private storage.

## Reuse
The **Identify Engine** (camera → AI → result card → collection) is built generic in `:core:identify` + `:core:ai` so StoneID and ThriftScan reuse it by supplying their own `IdentifySpec`.

## Out of scope (v2)
Banknote serial-number checks, melt-value calculator with live metal prices, CSV export, wishlist and price alerts, accounts/cloud sync.

## ASO keywords
coin identifier, coin value, coin scanner, error coins, banknote value, coin collection.
