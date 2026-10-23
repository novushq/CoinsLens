package app.novushq.coinlens.feature.paywall

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.common.toUiState
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.GrantBonusScanUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.model.Persona
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Content behind the plans [UiState]; the screen renders loading/empty/error/success from it. */
data class PaywallContent(
    val plans: List<BillingPlan> = emptyList(),
    val selected: PlanType = PlanType.ANNUAL,
    val persona: Persona = Persona.COLLECTOR,
    val isPro: Boolean = false,
    val busy: Boolean = false,
    @StringRes val message: Int? = null,
    val demoBilling: Boolean = true,
    val demoAds: Boolean = true,
)

sealed interface PaywallAction {
    data class SelectPlan(val plan: PlanType) : PaywallAction
    data class Purchase(val activity: Activity) : PaywallAction
    data object Restore : PaywallAction
    data class WatchAd(val activity: Activity) : PaywallAction
    data object RetryPlans : PaywallAction
    data object ClearMessage : PaywallAction
}

sealed interface PaywallEvent {
    data object NavigateBack : PaywallEvent
}

class PaywallViewModel(
    savedStateHandle: SavedStateHandle,
    private val billing: BillingGateway,
    private val ads: RewardedAdGateway,
    private val entitlements: EntitlementRepository,
    private val grantBonus: GrantBonusScanUseCase,
    private val preferences: PreferencesRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    @Suppress("unused")
    val source: String? = savedStateHandle.get<String>("source")

    private val content = MutableStateFlow(PaywallContent(demoBilling = billing.isDemo, demoAds = ads.isDemo))
    private val plansState = MutableStateFlow<UiState<List<BillingPlan>>>(UiState.Loading)

    private val _uiState = MutableStateFlow<UiState<PaywallContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<PaywallContent>> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaywallEvent>()
    val events: SharedFlow<PaywallEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            combine(preferences.persona, entitlements.isPro) { persona, isPro -> persona to isPro }
                .collect { (persona, isPro) ->
                    content.update {
                        it.copy(persona = persona ?: Persona.COLLECTOR, isPro = isPro)
                    }
                    render()
                }
        }
        viewModelScope.launch(dispatchers.io) { ads.prepare() }
        loadPlans()
    }

    fun onAction(action: PaywallAction) {
        when (action) {
            is PaywallAction.SelectPlan -> {
                content.update { it.copy(selected = action.plan) }
                render()
            }
            is PaywallAction.Purchase -> purchase(action.activity)
            PaywallAction.Restore -> restore()
            is PaywallAction.WatchAd -> watchAd(action.activity)
            PaywallAction.RetryPlans -> loadPlans()
            PaywallAction.ClearMessage -> {
                content.update { it.copy(message = null) }
                render()
            }
        }
    }

    private fun loadPlans() {
        plansState.value = UiState.Loading
        render()
        viewModelScope.launch(dispatchers.io) {
            plansState.value = billing.loadPlans().toUiState(isEmpty = { it.isEmpty() })
            val plans = (plansState.value as? UiState.Success)?.data.orEmpty()
            content.update {
                it.copy(
                    plans = plans,
                    selected = plans.firstOrNull { plan -> plan.type == it.selected }?.type
                        ?: plans.firstOrNull()?.type
                        ?: PlanType.ANNUAL,
                )
            }
            render()
        }
    }

    private fun purchase(activity: Activity) {
        if (content.value.isPro || content.value.busy) return
        val plan = content.value.selected
        setBusy(true)
        viewModelScope.launch(dispatchers.io) {
            when (val result = billing.purchase(activity, plan)) {
                is AppResult.Success -> if (result.data) {
                    _events.emit(PaywallEvent.NavigateBack)
                }
                // Cancelled checkout stays quiet: not an error.
                is AppResult.Failure -> content.update { it.copy(message = R.string.paywall_purchase_error) }
            }
            setBusy(false)
            render()
        }
    }

    private fun restore() {
        if (content.value.busy) return
        setBusy(true)
        viewModelScope.launch(dispatchers.io) {
            content.update {
                it.copy(
                    message = when (val result = entitlements.restore()) {
                        is AppResult.Success -> if (result.data) {
                            if (billing.isDemo) R.string.paywall_restored else R.string.paywall_store_restored
                        } else {
                            R.string.paywall_nothing_to_restore
                        }
                        is AppResult.Failure -> R.string.paywall_restore_error
                    },
                )
            }
            setBusy(false)
            render()
        }
    }

    private fun watchAd(activity: Activity) {
        if (content.value.isPro || content.value.busy) return
        setBusy(true)
        viewModelScope.launch(dispatchers.io) {
            when (val result = ads.show(activity)) {
                is AppResult.Success -> if (result.data) {
                    if (grantBonus().isSuccess) {
                        content.update { it.copy(message = R.string.paywall_bonus_granted) }
                        _events.emit(PaywallEvent.NavigateBack)
                    } else {
                        content.update { it.copy(message = R.string.paywall_reward_error) }
                    }
                }
                // Dismissed before earning the reward: no scan, no error.
                is AppResult.Failure -> content.update { it.copy(message = R.string.paywall_reward_error) }
            }
            setBusy(false)
            render()
        }
    }

    private fun setBusy(busy: Boolean) {
        content.update { it.copy(busy = busy) }
        render()
    }

    private fun render() {
        val snapshot = content.value
        _uiState.value = when (val plans = plansState.value) {
            UiState.Loading -> UiState.Loading
            is UiState.Empty -> UiState.Empty(plans.message)
            is UiState.Error -> UiState.Error(plans.error, plans.retryable)
            is UiState.Success -> UiState.Success(snapshot.copy(plans = plans.data))
        }
    }
}
