package app.novushq.coinlens.feature.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.novushq.coinlens.ai.AiInfoProvider
import app.novushq.coinlens.ai.AiMode
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.model.Persona
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the settings screen renders; wrapped in [UiState] for loading/error/success. */
data class SettingsContent(
    val isPro: Boolean = false,
    val scansLeft: Int = 0,
    val persona: Persona? = null,
    val aiMode: AiMode = AiMode.FAKE,
    val modelName: String = "",
    val restoring: Boolean = false,
    @StringRes val message: Int? = null,
    val termsUrl: String = "",
    val privacyUrl: String = "",
)

sealed interface SettingsAction {
    data object GoPro : SettingsAction
    data object Restore : SettingsAction
    data object ManageSubscription : SettingsAction
    data object OpenTerms : SettingsAction
    data object OpenPrivacy : SettingsAction
    data class SelectPersona(val persona: Persona) : SettingsAction
    data object ResetOnboarding : SettingsAction
    data object LinkFailed : SettingsAction
    data object ClearMessage : SettingsAction
    data object Retry : SettingsAction
}

sealed interface SettingsEvent {
    data object NavigatePaywall : SettingsEvent
    data object NavigateOnboarding : SettingsEvent
    data class OpenUrl(val url: String) : SettingsEvent
}

class SettingsViewModel(
    private val entitlements: EntitlementRepository,
    private val allowance: ObserveScanAllowanceUseCase,
    private val preferences: PreferencesRepository,
    private val aiInfo: AiInfoProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val content = MutableStateFlow(SettingsContent(termsUrl = BuildConfig.TERMS_URL, privacyUrl = BuildConfig.PRIVACY_URL))

    private val _uiState = MutableStateFlow<UiState<SettingsContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<SettingsContent>> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.GoPro -> emit(SettingsEvent.NavigatePaywall)
            SettingsAction.Restore -> restore()
            SettingsAction.ManageSubscription -> emit(SettingsEvent.OpenUrl(PLAY_SUBSCRIPTIONS_URL))
            SettingsAction.OpenTerms -> openUrl(content.value.termsUrl)
            SettingsAction.OpenPrivacy -> openUrl(content.value.privacyUrl)
            is SettingsAction.SelectPersona -> selectPersona(action.persona)
            SettingsAction.ResetOnboarding -> resetOnboarding()
            SettingsAction.LinkFailed -> {
                content.update { it.copy(message = R.string.settings_open_link_error) }
                render()
            }
            SettingsAction.ClearMessage -> {
                content.update { it.copy(message = null) }
                render()
            }
            SettingsAction.Retry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = UiState.Loading
        loadJob = viewModelScope.launch(dispatchers.io) {
            when (val refreshed = entitlements.refresh()) {
                is AppResult.Failure -> {
                    _uiState.value = UiState.Error(refreshed.error)
                    return@launch
                }
                is AppResult.Success -> Unit
            }
            combine(
                entitlements.isPro,
                allowance(),
                preferences.persona,
                aiInfo.info,
            ) { isPro, scanAllowance, persona, info ->
                SettingsContent(
                    isPro = isPro,
                    scansLeft = scanAllowance.freeRemaining + scanAllowance.bonusRemaining,
                    persona = persona,
                    aiMode = info.mode,
                    modelName = info.modelName,
                    restoring = content.value.restoring,
                    message = content.value.message,
                    termsUrl = content.value.termsUrl,
                    privacyUrl = content.value.privacyUrl,
                )
            }.collect {
                content.value = it
                _uiState.value = UiState.Success(it)
            }
        }
    }

    private fun restore() {
        if (content.value.restoring) return
        content.update { it.copy(restoring = true, message = null) }
        render()
        viewModelScope.launch(dispatchers.io) {
            val message = when (val result = entitlements.restore()) {
                is AppResult.Success -> if (result.data) {
                    R.string.settings_restore_success
                } else {
                    R.string.settings_restore_empty
                }
                is AppResult.Failure -> R.string.settings_restore_error
            }
            content.update { it.copy(restoring = false, message = message) }
            render()
        }
    }

    private fun selectPersona(persona: Persona) {
        viewModelScope.launch(dispatchers.io) {
            preferences.setPersona(persona)
        }
    }

    private fun resetOnboarding() {
        viewModelScope.launch(dispatchers.io) {
            preferences.setOnboardingDone(false)
            _events.emit(SettingsEvent.NavigateOnboarding)
        }
    }

    private fun openUrl(url: String) {
        if (url.isNotBlank()) emit(SettingsEvent.OpenUrl(url))
    }

    private fun emit(event: SettingsEvent) {
        viewModelScope.launch(dispatchers.main) {
            _events.emit(event)
        }
    }

    private fun render() {
        val snapshot = content.value
        if (_uiState.value is UiState.Success) {
            _uiState.value = UiState.Success(snapshot)
        }
    }

    companion object {
        const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
    }
}
