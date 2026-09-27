package app.novushq.coinlens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Resolves the start destination once; the splash screen stays up until it is known. */
class MainViewModel(private val prefs: PreferencesRepository) : ViewModel() {
    private val _startRoute = MutableStateFlow<Route?>(null)
    val startRoute: StateFlow<Route?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            _startRoute.value = if (prefs.onboardingDone.first()) Route.Home else Route.Onboarding
        }
    }
}
