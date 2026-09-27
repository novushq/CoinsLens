package app.novushq.coinlens.navigation

import androidx.navigation.NavController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface NavCommand {
    /** Navigate to [route], optionally popping the back stack up to [popUpTo] first. */
    data class Navigate(
        val route: Route,
        val popUpTo: Route? = null,
        val inclusive: Boolean = false,
    ) : NavCommand

    /** Clear the whole back stack and make [route] the new root (e.g. onboarding → home). */
    data class ReplaceAll(val route: Route) : NavCommand

    data object Back : NavCommand
}

/**
 * Features and ViewModels navigate through this instead of holding a NavController.
 * The app's NavHost collects [commands] and applies them with [execute].
 */
interface AppNavigator {
    val commands: Flow<NavCommand>

    fun navigate(route: Route, popUpTo: Route? = null, inclusive: Boolean = false)

    fun replaceAll(route: Route)

    fun back()
}

class DefaultAppNavigator : AppNavigator {
    private val channel = Channel<NavCommand>(Channel.BUFFERED)

    override val commands: Flow<NavCommand> = channel.receiveAsFlow()

    override fun navigate(route: Route, popUpTo: Route?, inclusive: Boolean) {
        channel.trySend(NavCommand.Navigate(route, popUpTo, inclusive))
    }

    override fun replaceAll(route: Route) {
        channel.trySend(NavCommand.ReplaceAll(route))
    }

    override fun back() {
        channel.trySend(NavCommand.Back)
    }
}

/** Applies [command] to this controller. Navigation is single-top so double taps never stack duplicates. */
fun NavController.execute(command: NavCommand) {
    when (command) {
        is NavCommand.Navigate -> navigate(command.route) {
            launchSingleTop = true
            command.popUpTo?.let { target -> popUpTo(target) { inclusive = command.inclusive } }
        }
        is NavCommand.ReplaceAll -> navigate(command.route) {
            launchSingleTop = true
            popUpTo(graph.id) { inclusive = true }
        }
        NavCommand.Back -> popBackStack()
    }
}
