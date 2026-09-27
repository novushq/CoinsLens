package app.novushq.coinlens.navigation

import kotlinx.serialization.Serializable

/** Every destination in the app (docs/NAVIGATION.md). Owners are noted per route. */
@Serializable
sealed interface Route {
    /** :feature:onboarding — first launch only. */
    @Serializable
    data object Onboarding : Route

    /** :feature:home — start destination once onboarding is done. */
    @Serializable
    data object Home : Route

    /** :feature:capture — obverse then optional reverse photo, written to ScanDraft. */
    @Serializable
    data object Capture : Route

    /** :feature:result — [scanId] null identifies the current ScanDraft; non-null shows a saved scan. */
    @Serializable
    data class Result(val scanId: String? = null) : Route

    /** :feature:collection — dashboard with folders and totals. */
    @Serializable
    data object Collection : Route

    /** :feature:collection */
    @Serializable
    data class Folder(val folderId: String) : Route

    /** :feature:collection */
    @Serializable
    data class Item(val itemId: String) : Route

    /** :feature:share */
    @Serializable
    data class Share(val scanId: String) : Route

    /** :feature:paywall — [source] is one of [PaywallSource]. */
    @Serializable
    data class Paywall(val source: String = PaywallSource.QUOTA) : Route

    /** :feature:settings */
    @Serializable
    data object Settings : Route
}

object PaywallSource {
    const val QUOTA = "quota"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
}
