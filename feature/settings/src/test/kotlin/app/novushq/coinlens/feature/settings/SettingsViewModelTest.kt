package app.novushq.coinlens.feature.settings

import app.novushq.coinlens.ai.AiInfoProvider
import app.novushq.coinlens.ai.AiMode
import app.novushq.coinlens.ai.StaticModelConfig
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.testing.FakeEntitlements
import app.novushq.coinlens.testing.FakePreferences
import app.novushq.coinlens.testing.MainDispatcherRule
import app.novushq.coinlens.testing.TestDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FailingEntitlements(
    private val failRefresh: Boolean = true,
) : EntitlementRepository {
    val state = MutableStateFlow(false)
    override val isPro: Flow<Boolean> = state
    override suspend fun refresh(): AppResult<Boolean> =
        if (failRefresh) AppResult.Failure(AppError.Network()) else AppResult.Success(false)
    override suspend fun restore(): AppResult<Boolean> = AppResult.Failure(AppError.Network())
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun viewModel(
        entitlements: EntitlementRepository = FakeEntitlements(),
        prefs: FakePreferences = FakePreferences(),
        aiInfo: AiInfoProvider = AiInfoProvider(AiMode.FAKE, StaticModelConfig("test-model")),
    ) = SettingsViewModel(
        entitlements = entitlements,
        allowance = ObserveScanAllowanceUseCase(entitlements, prefs),
        preferences = prefs,
        aiInfo = aiInfo,
        dispatchers = TestDispatcherProvider(mainRule.dispatcher),
    )

    private fun success(vm: SettingsViewModel): SettingsContent {
        val state = vm.uiState.value
        assertTrue(state is UiState.Success)
        return (state as UiState.Success).data
    }

    @Test
    fun `free plan shows remaining scans and ai info`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(prefs = FakePreferences(used = 0, bonus = 0))
        advanceUntilIdle()
        val content = success(vm)
        assertEquals(false, content.isPro)
        assertEquals(1, content.scansLeft)
        assertEquals(AiMode.FAKE, content.aiMode)
        assertEquals("test-model", content.modelName)
    }

    @Test
    fun `bonus scans add to remaining scans`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(prefs = FakePreferences(used = 1, bonus = 2))
        advanceUntilIdle()
        assertEquals(2, success(vm).scansLeft)
    }

    @Test
    fun `pro plan reflects entitlement`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(entitlements = FakeEntitlements(pro = true))
        advanceUntilIdle()
        assertEquals(true, success(vm).isPro)
    }

    @Test
    fun `restore success with purchase shows restored message`() = runTest(mainRule.dispatcher) {
        val entitlements = FakeEntitlements(pro = true)
        val vm = viewModel(entitlements = entitlements)
        advanceUntilIdle()
        vm.onAction(SettingsAction.Restore)
        advanceUntilIdle()
        assertEquals(R.string.settings_restore_success, success(vm).message)
    }

    @Test
    fun `restore with nothing to restore shows empty message`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(entitlements = FakeEntitlements(pro = false))
        advanceUntilIdle()
        vm.onAction(SettingsAction.Restore)
        advanceUntilIdle()
        assertEquals(R.string.settings_restore_empty, success(vm).message)
    }

    @Test
    fun `restore failure shows error message`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(entitlements = FailingEntitlements(failRefresh = false))
        advanceUntilIdle()
        vm.onAction(SettingsAction.Restore)
        advanceUntilIdle()
        assertEquals(R.string.settings_restore_error, success(vm).message)
    }

    @Test
    fun `refresh failure shows error state`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(entitlements = FailingEntitlements())
        advanceUntilIdle()
        assertTrue(vm.uiState.value is UiState.Error)
    }

    @Test
    fun `selecting persona persists it`() = runTest(mainRule.dispatcher) {
        val prefs = FakePreferences()
        val vm = viewModel(prefs = prefs)
        advanceUntilIdle()
        vm.onAction(SettingsAction.SelectPersona(Persona.DETECTORIST))
        advanceUntilIdle()
        assertEquals(Persona.DETECTORIST, prefs.personaState.value)
        assertEquals(Persona.DETECTORIST, success(vm).persona)
    }

    @Test
    fun `reset onboarding clears the flag`() = runTest(mainRule.dispatcher) {
        val prefs = FakePreferences()
        prefs.doneState.value = true
        val vm = viewModel(prefs = prefs)
        advanceUntilIdle()
        vm.onAction(SettingsAction.ResetOnboarding)
        advanceUntilIdle()
        assertEquals(false, prefs.doneState.value)
    }
}
