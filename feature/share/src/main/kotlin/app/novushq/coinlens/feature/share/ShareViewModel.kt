package app.novushq.coinlens.feature.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.ScanRepository
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ShareAction {
    data object Retry : ShareAction
}

class ShareViewModel(
    savedStateHandle: SavedStateHandle,
    private val scans: ScanRepository,
) : ViewModel() {
    private val scanId = savedStateHandle.toRoute<Route.Share>().scanId

    private val _state = MutableStateFlow<UiState<ScanRecord>>(UiState.Loading)
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: ShareAction) {
        when (action) {
            ShareAction.Retry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            when (val record = scans.get(scanId)) {
                is AppResult.Success -> _state.value = UiState.Success(record.data)
                is AppResult.Failure -> _state.value = UiState.Error(record.error)
            }
        }
    }
}
