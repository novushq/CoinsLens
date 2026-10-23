package app.novushq.coinlens.feature.paywall

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.GrantBonusScanUseCase
import app.novushq.coinlens.testing.FakeEntitlements
import app.novushq.coinlens.testing.FakePreferences
import app.novushq.coinlens.testing.MainDispatcherRule
import app.novushq.coinlens.testing.TestDispatcherProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

private val demoPlans = listOf(
    BillingPlan(PlanType.WEEKLY, localizedPrice = "$4.99 / week (test)"),
    BillingPlan(PlanType.ANNUAL, localizedPrice = "$29.99 / year (test)", trialDays = 3),
    BillingPlan(PlanType.LIFETIME, localizedPrice = "$59.99 once (test)"),
)

private class FakeBilling(
    private val plans: List<BillingPlan> = demoPlans,
    var purchaseResult: AppResult<Boolean> = AppResult.Success(true),
    private val entitlements: FakeEntitlements? = null,
    var loadResult: AppResult<List<BillingPlan>>? = null,
) : BillingGateway {
    override val isDemo = true
    var purchases = 0
    var loads = 0

    override suspend fun loadPlans(): AppResult<List<BillingPlan>> {
        loads++
        return loadResult ?: AppResult.Success(plans)
    }

    override suspend fun purchase(activity: Activity, plan: PlanType): AppResult<Boolean> {
        purchases++
        if (purchaseResult is AppResult.Success && (purchaseResult as AppResult.Success).data) {
            entitlements?.state?.value = true
        }
        return purchaseResult
    }
}

private class FakeAds(var result: AppResult<Boolean> = AppResult.Success(true)) : RewardedAdGateway {
    override val isDemo = true
    var shows = 0
    var prepared = 0

    override suspend fun prepare() {
        prepared++
    }

    override suspend fun show(activity: Activity): AppResult<Boolean> {
        shows++
        return result
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PaywallViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun viewModel(
        billing: FakeBilling,
        ads: FakeAds = FakeAds(),
        entitlements: FakeEntitlements = FakeEntitlements(),
        prefs: FakePreferences = FakePreferences(),
    ) = PaywallViewModel(
        savedStateHandle = SavedStateHandle(),
        billing = billing,
        ads = ads,
        entitlements = entitlements,
        grantBonus = GrantBonusScanUseCase(prefs),
        preferences = prefs,
        dispatchers = TestDispatcherProvider(mainRule.dispatcher),
    )

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).get()

    @Test
    fun `loads plans with annual preselected`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(FakeBilling())
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is UiState.Success)
        val content = (state as UiState.Success).data
        assertEquals(demoPlans, content.plans)
        assertEquals(PlanType.ANNUAL, content.selected)
    }

    @Test
    fun `purchase success flips isPro and navigates back`() = runTest(mainRule.dispatcher) {
        val entitlements = FakeEntitlements()
        val vm = viewModel(FakeBilling(entitlements = entitlements), entitlements = entitlements)
        advanceUntilIdle()
        vm.events.test {
            vm.onAction(PaywallAction.Purchase(activity()))
            assertEquals(PaywallEvent.NavigateBack, awaitItem())
        }
        advanceUntilIdle()
        assertTrue(entitlements.state.value)
        val content = (vm.uiState.value as UiState.Success).data
        assertTrue(content.isPro)
    }

    @Test
    fun `cancelled purchase is silent`() = runTest(mainRule.dispatcher) {
        val billing = FakeBilling(purchaseResult = AppResult.Success(false))
        val vm = viewModel(billing)
        advanceUntilIdle()
        vm.events.test {
            vm.onAction(PaywallAction.Purchase(activity()))
            advanceUntilIdle()
            expectNoEvents()
        }
        advanceUntilIdle()
        val content = (vm.uiState.value as UiState.Success).data
        assertEquals(null, content.message)
    }

    @Test
    fun `restore reports success or nothing to restore`() = runTest(mainRule.dispatcher) {
        val pro = FakeEntitlements(pro = true)
        val restored = viewModel(FakeBilling(), entitlements = pro)
        advanceUntilIdle()
        restored.onAction(PaywallAction.Restore)
        advanceUntilIdle()
        assertEquals(
            R.string.paywall_restored,
            ((restored.uiState.value as UiState.Success).data).message,
        )

        val free = FakeEntitlements(pro = false)
        val empty = viewModel(FakeBilling(), entitlements = free)
        advanceUntilIdle()
        empty.onAction(PaywallAction.Restore)
        advanceUntilIdle()
        assertEquals(
            R.string.paywall_nothing_to_restore,
            ((empty.uiState.value as UiState.Success).data).message,
        )
    }

    @Test
    fun `rewarded ad grants exactly one bonus scan and navigates back`() = runTest(mainRule.dispatcher) {
        val prefs = FakePreferences()
        val ads = FakeAds()
        val vm = viewModel(FakeBilling(), ads = ads, prefs = prefs)
        advanceUntilIdle()
        vm.events.test {
            vm.onAction(PaywallAction.WatchAd(activity()))
            assertEquals(PaywallEvent.NavigateBack, awaitItem())
        }
        advanceUntilIdle()
        assertEquals(1, ads.shows)
        assertEquals(1, prefs.bonusState.value)
    }

    @Test
    fun `rewarded ad is never shown to pro users`() = runTest(mainRule.dispatcher) {
        val ads = FakeAds()
        val vm = viewModel(FakeBilling(), ads = ads, entitlements = FakeEntitlements(pro = true))
        advanceUntilIdle()
        vm.onAction(PaywallAction.WatchAd(activity()))
        advanceUntilIdle()
        assertEquals(0, ads.shows)
    }

    @Test
    fun `plans failure shows error and retry reloads`() = runTest(mainRule.dispatcher) {
        val billing = FakeBilling(loadResult = AppResult.Failure(AppError.Network()))
        val vm = viewModel(billing)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is UiState.Error)
        billing.loadResult = null
        vm.onAction(PaywallAction.RetryPlans)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is UiState.Success)
        assertEquals(2, billing.loads)
    }
}
