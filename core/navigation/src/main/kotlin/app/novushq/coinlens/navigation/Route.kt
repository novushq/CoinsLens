package app.novushq.coinlens.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. Every destination in the app is declared here so
 * the navigation map in docs/NAVIGATION.md can be verified against the code.
 */
sealed interface Route {
    @Serializable
    data object Home : Route
}
