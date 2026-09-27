package app.novushq.coinlens.navigation

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigatorTest {

    @Test
    fun `commands are delivered in order`() = runTest {
        val navigator = DefaultAppNavigator()
        navigator.navigate(Route.Capture)
        navigator.navigate(Route.Result(), popUpTo = Route.Home)
        navigator.replaceAll(Route.Home)
        navigator.back()

        assertEquals(
            listOf(
                NavCommand.Navigate(Route.Capture),
                NavCommand.Navigate(Route.Result(), popUpTo = Route.Home),
                NavCommand.ReplaceAll(Route.Home),
                NavCommand.Back,
            ),
            navigator.commands.take(4).toList(),
        )
    }

    @Test
    fun `commands sent before collection are not lost`() = runTest {
        val navigator = DefaultAppNavigator()
        navigator.navigate(Route.Paywall(PaywallSource.SETTINGS))
        assertEquals(NavCommand.Navigate(Route.Paywall("settings")), navigator.commands.first())
    }

    @Test
    fun `route args survive serialization`() {
        val routes = listOf(Route.Result("s1"), Route.Result(), Route.Folder("f"), Route.Item("i"), Route.Share("s"))
        routes.forEach { route ->
            val json = Json.encodeToString(Route.serializer(), route)
            assertEquals(route, Json.decodeFromString(Route.serializer(), json))
        }
    }
}
