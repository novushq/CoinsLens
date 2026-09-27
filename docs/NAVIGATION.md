# CoinLens — navigation

Typed `@Serializable` routes in `:core:navigation`. Start destination: `Onboarding` until onboarding is complete, then `Home`.

| route | args | owner | reached from |
|---|---|---|---|
| `Onboarding` | – | `:feature:onboarding` | first launch |
| `Home` | – | `:feature:home` | after onboarding; app launch |
| `Capture` | – | `:feature:capture` | Home "Scan" FAB, Collection empty state |
| `Result` | `scanId: String?` (null = identify current draft) | `:feature:result` | Capture done; Home recent scan tap |
| `Collection` | – | `:feature:collection` | Home "Collection" |
| `Folder` | `folderId: String` | `:feature:collection` | Collection folder tap |
| `Item` | `itemId: String` | `:feature:collection` | Folder/Collection item tap |
| `Share` | `scanId: String` | `:feature:share` | Result "Share", Item "Share" |
| `Paywall` | `source: String` (`quota`, `settings`, `onboarding`) | `:feature:paywall` | Capture/Home when `ScanAllowance.canScan` is false; Settings |
| `Settings` | – | `:feature:settings` | Home top bar |

Rules: Home is the only bottom-level destination (no bottom bar in MVP). Back from Result goes to Home, not Capture. Paywall is dismissible except after a completed purchase, which pops back to the source. The first scan never passes through the paywall.
