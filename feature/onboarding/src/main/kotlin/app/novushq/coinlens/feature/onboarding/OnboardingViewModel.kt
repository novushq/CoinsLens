package app.novushq.coinlens.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.CompleteOnboardingUseCase
import app.novushq.coinlens.model.Persona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Content for the single onboarding screen state. Page 0 = promise, 1 = quiz, 2 = camera priming. */
data class OnboardingData(
    val page: Int,
    val persona: Persona,
    val saving: Boolean,
    val navigateToCapture: Boolean,
)

sealed interface OnboardingAction {
    data class SelectPersona(val persona: Persona) : OnboardingAction
    data object Next : OnboardingAction
    data object Skip : OnboardingAction
    data class PermissionResult(val granted: Boolean) : OnboardingAction
    data object SkipPermission : OnboardingAction
    data object Retry : OnboardingAction
    data object NavigationConsumed : OnboardingAction
}

class OnboardingViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    companion object {
        const val PAGE_COUNT = 3
        const val QUIZ_PAGE = 1
    }

    private val _state =
        MutableStateFlow<UiState<OnboardingData>>(
            UiState.Success(OnboardingData(page = 0, persona = Persona.COLLECTOR, saving = false, navigateToCapture = false)),
        )
    val state = _state.asStateFlow()

    private var lastData: OnboardingData =
        (state.value as UiState.Success).data
    private var completionPersona: Persona = Persona.COLLECTOR

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.SelectPersona -> update { it.copy(persona = action.persona) }
            OnboardingAction.Next -> update { it.copy(page = (it.page + 1).coerceAtMost(PAGE_COUNT - 1)) }
            OnboardingAction.Skip -> complete(Persona.COLLECTOR)
            is OnboardingAction.PermissionResult -> complete(lastData.persona)
            OnboardingAction.SkipPermission -> complete(lastData.persona)
            OnboardingAction.Retry -> {
                _state.value = UiState.Success(lastData.copy(saving = true))
                complete(completionPersona)
            }
            OnboardingAction.NavigationConsumed -> update { it.copy(navigateToCapture = false) }
        }
    }

    private fun update(transform: (OnboardingData) -> OnboardingData) {
        val current = _state.value as? UiState.Success ?: return
        val next = transform(current.data)
        lastData = next
        _state.value = UiState.Success(next)
    }

    private fun complete(persona: Persona) {
        completionPersona = persona
        update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = withContext(dispatchers.io) { completeOnboarding(persona) }
            when (result) {
                is AppResult.Success -> update { it.copy(saving = false, navigateToCapture = true) }
                is AppResult.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }
}
