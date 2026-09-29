# CoinLens — design

**Mood:** a museum label under a loupe — warm, precise, trustworthy. Numbers are the hero.

## Colour
- Seed **#8C6A2F** (aged brass). Chosen because every coin photo sits well against warm metal tones, and gold reads as "value" without looking like a finance app.
- Light: surfaces warm off-white `#FBF8F2`-ish from the tonal palette (`surface`, `surfaceContainerLow`), text near-black warm neutral. Dark: deep warm charcoal surfaces (tone 6–12 of the neutral palette, not grey), brass primary at tone 80.
- Accent (primary) is used once per screen: the scan/shutter action or the primary CTA.
- Confidence colours come from the scheme: HIGH → `primary`, MEDIUM → `tertiary`, LOW → `error`; always paired with an icon (verified / help / warning) and the word.
- Brand extensions (`coinExtras`, light + dark): `goldBright`/`goldDeep` metallic stops for the coin ring, hero and value-band gradients; `success`/`successContainer` green pair for success states (tertiary is reserved for MEDIUM confidence).
- Dynamic colour supported but off by default (the brass identity matters in screenshots).

## Type
- Display/headline: `FontFamily.Serif` (system serif), weights 600; used for coin names and the big total value — the "museum label" feel.
- Body/labels: default sans (`FontFamily.Default`).
- Scale: displayLarge 52/60, displayMedium 44/52, displaySmall 36/44 w600 serif; headlineMedium 28/36 w600 serif; titleLarge 22/28 w600 sans; titleMedium 16/24 w600; bodyLarge 16/24 w400; bodyMedium 14/20; labelLarge 14/20 w600; labelSmall 11/16 w600 letterSpacing 0.5. Display/headline carry −0.25 tracking.
- All money uses **tabular figures** (`fontFeatureSettings = "tnum"`) via `CoinTextStyles.money`.

## Spacing, shape, elevation
- Spacing tokens: 4, 8, 12, 16, 24, 32, 48 (`Spacing.xs … xxl`). Screen gutter 16dp (24dp on ≥600dp width).
- Radii: small 8dp (chips), medium 16dp (cards), large 28dp (sheets, result card top). Coin photos are always **circles**. Buttons are full pills (`CircleShape`).
- Elevation: tonal first (surfaceContainer levels); hairline `outlineVariant` borders lift cards off flat backgrounds. Small shadows (`Elevations`) reserved for buttons, hero cards, the coin frame and the shutter.

## Motion
- 200–280ms with emphasized easing; container transform from capture thumbnail into result card; value numbers count up once (≤600ms) on first reveal, disabled under reduce-motion. Skeleton loading uses a travelling sheen (1200ms).
- Shutter press: scale 0.92 spring + haptic `CONFIRM`.

## Signature details (recognisable in a screenshot)
1. **`CoinFrame`** — circular photo with a metallic gold gradient ring, outer hairline, soft shadow and a subtle inner shadow, obverse/reverse flip on tap.
2. **`ValueRangeBar`** — a horizontal track showing circulated band (tonal) and uncirculated band (gold gradient) on one scale with low/high tabular labels; confidence pill at the right end.
3. **Museum label result card** — serif name, a small-caps metadata row (COUNTRY · YEAR · MINT), `BrassDivider` (hairlines + brass diamond), hairline detail rows.
4. **`HeroCard`** — warm gradient feature card with faint concentric loupe rings; home for the scan invitation and paywall pitch.
5. **`ShutterButton`** — 72dp brass-ringed camera button with spring press and haptic.

## Components in `:core:designsystem`
`CoinLensTheme` (+ `coinExtras`), `Spacing`, `Stroke`, `Elevations`, `CoinFrame`, `ValueRangeBar`, `ConfidencePill`, `PossibleHintChip` ("Possible doubled die"), `MoneyText`, `UiStateSurface` (shimmer skeleton/empty/error/success), `PrimaryButton` (the one accent action), `SecondaryButton` (tonal second action), `TertiaryButton` (quiet outlined third action), `ShutterButton`, `HeroCard`, `BrassDivider`, `SelectableCard` (selected-state option card), `BackIconButton` / `CloseIconButton` (top-bar navigation), `SectionHeader`, `DisclaimerText`.

## Navigation
Top-level destinations (Home, Cabinet, Settings) live in a `NavigationBar` owned by `:app`. Pushed screens use M3 top app bars with a back icon; the fresh result is terminal and closes to Home. Screen transitions are fade + 1/4-width slide at `Motion.MEDIUM`.
