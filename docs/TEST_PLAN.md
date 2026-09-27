# CoinLens — test plan

Every acceptance criterion below needs a test that fails before the task and passes after it.

### Camera capture with coin guide

- Capture flow takes a front photo and optionally a back photo, cropped to the circular guide and ≤1600px, and stores both in ScanDraft
- Glare and blur detectors are pure functions with unit tests covering bright, normal and blurry synthetic inputs
- A glare warning banner appears while the guide area is over-exposed and does not block the shutter
- Gallery import via Photo Picker produces the same ScanDraft as the camera path
- When the scan allowance is exhausted the screen routes to Paywall(source=quota) instead of opening the camera
- Camera permission denied and permanently denied states render with a clear next action

### Identification result card

- Identifying a draft with FakeIdentifyEngine shows the result card with name, country, year, mint mark, composition and rarity
- Circulated and uncirculated ranges render on one ValueRangeBar with a confidence label and the disclaimer always visible
- Every variety hint is rendered prefixed with 'Possible' and never as a certainty
- An unrecognized result and each AppError type render distinct states, and Retry does not consume a second scan
- Add to collection saves a CollectionItem with folder, grade, purchase price and notes
- ViewModel unit tests cover loading → success, unrecognized, error → retry

### Home dashboard with total worth

- Home shows total worth, low–high range and item count from CollectionSummary using tabular figures
- Scan CTA routes to Capture when allowance permits and to Paywall(source=quota) otherwise
- Allowance copy reflects free remaining, bonus scans and is hidden for Pro users
- Recent scans list opens Result for the tapped scan
- Empty, loading, error and success states render and are unit-tested in the ViewModel

### Collection folders and items

- Folders can be created, renamed and deleted; deleting a folder moves its items to Unsorted after a confirmation naming the consequence
- Folder screen lists items with name, year, grade and value and supports sorting by value, date and name
- Item edits to grade, notes, purchase price, quantity and folder persist in Room and update totals immediately
- Collection header shows total value, cost basis and gain/loss computed by ComputeCollectionValueUseCase
- ViewModel unit tests cover folder CRUD and item edit flows with fake repositories

### Shareable what's-it-worth card

- The card renders the coin photo, name, year, country, value range and confidence label from the ScanRecord
- Share produces a PNG via FileProvider and opens the system share sheet
- Save to Photos writes the PNG through MediaStore without requesting storage permission on API 29+
- Light and dark card variants can be toggled and both render correctly
- Card layout logic (text formatting of value range) is unit-tested

### Onboarding persona quiz

- Choosing a persona persists it via PreferencesRepository and is readable by the paywall
- Completing onboarding marks it done so the next launch starts at Home
- The flow ends on Capture for the first free scan without passing through the paywall or any ad
- Camera permission is requested only after the priming page explains why
- ViewModel unit tests cover persona selection, skip and completion

### Paywall, subscriptions and rewarded scan

- Paywall shows weekly, annual (with 3-day trial) and lifetime plans with prices from the billing gateway, annual preselected
- Headline and benefit copy differ for COLLECTOR, INHERITED and DETECTORIST personas
- A completed purchase makes EntitlementRepository.isPro emit true and returns to the source screen; a cancelled purchase shows no error
- Watching the rewarded ad grants exactly one bonus scan via GrantBonusScanUseCase and is never offered to Pro users
- Restore purchases is available and reports success or 'nothing to restore'
- All billing and ads symbols live only in feature/paywall, with fake gateways used when API keys are absent, and ViewModel tests cover purchase, cancel, restore and reward

### Settings and legal

- Restore purchases calls EntitlementRepository.restore and shows the outcome
- Plan status reflects Free (with remaining scans) or Pro from the allowance flow
- Manage subscription opens the Play Store subscriptions deep link
- The AI engine row shows whether Gemini or demo data is active
- Persona can be changed and persists; ViewModel unit tests cover restore success and failure
