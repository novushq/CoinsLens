package app.novushq.coinlens.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.domain.ScanRepository
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.ScanAllowance
import app.novushq.coinlens.model.ScanRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeData(
    val summary: CollectionSummary,
    val allowance: ScanAllowance,
    val recent: List<ScanRecord>,
)

sealed interface HomeAction {
    data object Retry : HomeAction
}

class HomeViewModel(
    private val collections: CollectionRepository,
    private val scans: ScanRepository,
    private val observeAllowance: ObserveScanAllowanceUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        observe()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Retry -> observe()
        }
    }

    fun retry() = onAction(HomeAction.Retry)

    private fun observe() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                collections.observeSummary(),
                scans.observeRecent(RECENT_LIMIT),
                observeAllowance(),
            ) { summaryResult, recentResult, allowance ->
                when {
                    summaryResult is AppResult.Failure -> UiState.Error(summaryResult.error)
                    recentResult is AppResult.Failure -> UiState.Error(recentResult.error)
                    else -> {
                        val summary = (summaryResult as AppResult.Success).data
                        val recent = (recentResult as AppResult.Success).data
                        if (summary.itemCount == 0 && recent.isEmpty()) {
                            UiState.Empty(EMPTY_STATE_MESSAGE)
                        } else {
                            UiState.Success(HomeData(summary, allowance, recent))
                        }
                    }
                }
            }.collect { _state.value = it }
        }
    }

    companion object {
        const val RECENT_LIMIT = 10
        const val EMPTY_STATE_MESSAGE = "Scan your first coin — it's free"
    }
}
