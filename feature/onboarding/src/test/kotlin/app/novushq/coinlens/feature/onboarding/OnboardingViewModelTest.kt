package app.novushq.coinlens.feature.onboarding

import androidx.lifecycle.ViewModelStore
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.CompleteOnboardingUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.testing.FakePreferences
import app.novushq.coinlens.testing.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FailOncePreferences(
    private val delegate: FakePreferences,
    var failPersona: Boolean,
) : PreferencesRepository by delegate {
    override suspend fun setPersona(persona: Persona?): AppResult<Unit> =
        if (failPersona) {
            AppResult.Failure(AppError.Storage("boom"))
        } else {
            delegate.setPersona(persona)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @Test
    fun `starts on the promise page with collector selected`() {
        val viewModel = OnboardingViewModel(
            CompleteOnboardingUseCase(FakePreferences()),
            TestDispatcherProvider(StandardTestDispatcher()),
        )
        val store = ViewModelStore().apply { put("onboarding", viewModel) }
        try {
            val state = viewModel.state.value
            assertTrue(state is UiState.Success)
            val data = (state as UiState.Success).data
            assertEquals(0, data.page)
            assertEquals(Persona.COLLECTOR, data.persona)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `next advances through quiz to camera priming and clamps there`() {
        val viewModel = OnboardingViewModel(
            CompleteOnboardingUseCase(FakePreferences()),
            TestDispatcherProvider(StandardTestDispatcher()),
        )
        val store = ViewModelStore().apply { put("onboarding", viewModel) }
        try {
            viewModel.onAction(OnboardingAction.Next)
            viewModel.onAction(OnboardingAction.Next)
            viewModel.onAction(OnboardingAction.Next)
            val data = (viewModel.state.value as UiState.Success).data
            assertEquals(2, data.page)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `completing after camera permission persists the chosen persona and navigates`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val prefs = FakePreferences()
        val viewModel = OnboardingViewModel(
            CompleteOnboardingUseCase(prefs),
            TestDispatcherProvider(dispatcher),
        )
        val store = ViewModelStore().apply { put("onboarding", viewModel) }
        try {
            viewModel.onAction(OnboardingAction.SelectPersona(Persona.INHERITED))
            viewModel.onAction(OnboardingAction.PermissionResult(granted = true))
            runCurrent()
            assertEquals(Persona.INHERITED, prefs.personaState.value)
            assertTrue(prefs.doneState.value)
            val data = (viewModel.state.value as UiState.Success).data
            assertTrue(data.navigateToCapture)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `denied camera permission still completes onboarding`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val prefs = FakePreferences()
        val viewModel = OnboardingViewModel(
            CompleteOnboardingUseCase(prefs),
            TestDispatcherProvider(dispatcher),
        )
        val store = ViewModelStore().apply { put("onboarding", viewModel) }
        try {
            viewModel.onAction(OnboardingAction.SelectPersona(Persona.DETECTORIST))
            viewModel.onAction(OnboardingAction.PermissionResult(granted = false))
            runCurrent()
            assertEquals(Persona.DETECTORIST, prefs.personaState.value)
            assertTrue(prefs.doneState.value)
            val data = (viewModel.state.value as UiState.Success).data
            assertTrue(data.navigateToCapture)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `skip completes with the collector default`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val prefs = FakePreferences()
        val viewModel = OnboardingViewModel(
            CompleteOnboardingUseCase(prefs),
            TestDispatcherProvider(dispatcher),
        )
        val store = ViewModelStore().apply { put("onboarding", viewModel) }
        try {
            viewModel.onAction(OnboardingAction.SelectPersona(Persona.DETECTORIST))
            viewModel.onAction(OnboardingAction.Skip)
            runCurrent()
            assertEquals(Persona.COLLECTOR, prefs.personaState.value)
            assertTrue(prefs.doneState.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed completion surfaces error and retry succeeds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val prefs = FailOncePreferences(FakePreferences(), failPersona = true)
        val viewModel = OnboardingViewModel(
            CompleteOnboardingUseCase(prefs),
            TestDispatcherProvider(dispatcher),
        )
        val store = ViewModelStore().apply { put("onboarding", viewModel) }
        try {
            viewModel.onAction(OnboardingAction.PermissionResult(granted = true))
            runCurrent()
            assertTrue(viewModel.state.value is UiState.Error)
            prefs.failPersona = false
            viewModel.onAction(OnboardingAction.Retry)
            runCurrent()
            val state = viewModel.state.value
            assertTrue(state is UiState.Success)
            assertTrue((state as UiState.Success).data.navigateToCapture)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
