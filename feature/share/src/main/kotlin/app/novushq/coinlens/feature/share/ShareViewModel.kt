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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShareViewModel(
    savedStateHandle: SavedStateHandle,
    scans: ScanRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ScanRecord>>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val record = scans.get(savedStateHandle.toRoute<Route.Share>().scanId)) {
                is AppResult.Success -> _state.value = UiState.Success(record.data)
                is AppResult.Failure -> _state.value = UiState.Error(record.error)
            }
        }
    }
}
