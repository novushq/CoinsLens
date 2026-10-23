# CoinLens — Context log

Append-only record of *why* this app is the way it is. Every agent reads this
before touching the code and appends to it when it makes a non-obvious call.

## 2026-09-27 — App created

- App ID: `app_zecy7nd27tk8`
- Idea: Photograph a coin or banknote and get its name, country, denomination, year, mint mark, composition, rarity, possible error/variety hints and an honest circulated/uncirculated value range with a confidence label, then track the collection's total worth. Built on a reusable Identify Engine (camera -> Gemini via Firebase AI Logic -> result card -> Room collection) shared later with StoneID and ThriftScan. First scan free with zero ads; shareable 'what's it worth' card.
- Repo: `/Volumes/prafull2/APPS/COINLENS/COINLENS`

## 2026-10-23 — implement (C-6)

Replaced the 2-page onboarding stub with a 3-page flow (promise, persona quiz, camera priming) driven by OnboardingViewModel exposing StateFlow<UiState<OnboardingData>> plus onAction, rendered through UiStateSurface with previews for all four states. Quiz selection and skip persist via core CompleteOnboardingUseCase (skip defaults to COLLECTOR); CAMERA is requested only from the priming page and completion navigates straight to Capture with no paywall or ads.

## 2026-10-23 — implement (C-2)

Built the C-2 identification result card in feature/result: museum-label card with flippable CoinFrame, metadata row, rarity chip, ValueRangeBar with ConfidencePill plus always-visible disclaimer, PossibleHintChip list with detail sheet, alternatives, not-a-coin state, typed AppError states with safe retry, and an add-to-collection bottom sheet (folder pick/create, grade, price, notes) wired to AddToCollectionUseCase; loading shows a card skeleton with rotating hints. Full assembleDebug and test suite pass.

## 2026-10-23 — implement (C-7)

Built the paywall feature in feature/paywall: PaywallViewModel with plans/purchase/restore/rewarded-ad actions, RevenueCat billing gateway with store-driven prices and offline-cached pro entitlement, AdMob rewarded gateway with fake fallback, and a persona-tailored Compose paywall with previews and 7 passing ViewModel tests.

## 2026-10-23 — implement (C-1)

Built the C-1 CameraX capture flow: Front then skippable-Back steps under a 72% circular guide drawn over CameraXViewfinder, ImageCapture plus Photo Picker both feeding a shared crop (guide square, max 1600px, JPEG q85) pipeline, advisory glare (>6% luma ≥245) and blur (Laplacian variance) banners from a throttled ImageAnalysis analyzer, permission rationale with settings fallback, and quota-exhausted routing to Paywall(source=quota) with both sides written to ScanDraft on Done. Pure quality/crop math and the ViewModel are covered by 23 unit tests; full assembleDebug and test pass.

## 2026-10-23 — implement (C-3)

Rebuilt the Home dashboard to the C-3 spec: total-worth hero (totalMid in displaySmall serif tabular with one-time count-up, low-high range, item count), extended-FAB scan CTA routing to Capture or Paywall(source=quota), free/bonus allowance lines hidden for Pro, last-10 scans as a horizontal CoinFrame row into Result(scanId), Collection/Settings top-bar entries, and first-scan empty copy. Removed the banner ad from home to honor the rewarded-only monetization policy. ViewModel exposes StateFlow<UiState> plus onAction with full loading/empty/error/success coverage and previews.

## 2026-10-23 — implement (C-4)

Collection folders and items now meet the C-4 contract: the ViewModel dispatches all intents through a new onAction handler, screens were rewired stateless for previewability, ThemePreviews cover content/empty/loading/error states, and a new 8-test ViewModel suite covers folder CRUD, item edits, route handling and failure paths; module tests and full assembleDebug pass.

## 2026-10-23 — implement (C-8)

Rebuilt the Settings screen on a new SettingsViewModel exposing immutable StateFlow<UiState<SettingsContent>> + onAction with one-shot events; plan status from the allowance flow, restore via EntitlementRepository.restore() with outcome message, Play subscriptions deep link, Gemini/demo AI row, disclaimer, legal links, app version, debug-only onboarding reset, and persisted persona change. Screen renders all four UI states through UiStateSurface with a preview per state; 10 unit tests pass alongside :app:assembleDebug.

## 2026-10-23 — implement (C-5)

Built the C-5 shareable what's-it-worth card in feature/share: ShareViewModel now exposes StateFlow<UiState<ScanRecord>> with onAction(Retry), ShareCardRenderer gained pure unit-tested formatValueRange/formatMetadata helpers, ShareEntry wires retry, keyed lazy items, defaulted Modifiers, light/dark toggle, PNG share via the app FileProvider and MediaStore save on API 29+, plus Compose previews for all four UI states. 7/7 share tests and the full build pass.
