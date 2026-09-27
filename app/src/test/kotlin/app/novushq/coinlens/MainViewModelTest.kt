package app.novushq.coinlens

import app.novushq.coinlens.navigation.Route
import app.novushq.coinlens.testing.FakePreferences
import app.novushq.coinlens.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `first launch starts at onboarding`() = runTest {
        val vm = MainViewModel(FakePreferences())
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Route.Onboarding, vm.startRoute.value)
    }

    @Test
    fun `returning user starts at home`() = runTest {
        val prefs = FakePreferences().apply { setOnboardingDone(true) }
        val vm = MainViewModel(prefs)
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Route.Home, vm.startRoute.value)
    }
}
