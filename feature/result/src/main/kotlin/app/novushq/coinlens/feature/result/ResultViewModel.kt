package app.novushq.coinlens.feature.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.data.ScanDraft
import app.novushq.coinlens.domain.AddToCollectionUseCase
import app.novushq.coinlens.domain.IdentifyCoinUseCase
import app.novushq.coinlens.domain.ScanRepository
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResultData(
    val record: ScanRecord,
    val addedToCabinet: Boolean = false,
    val collectionError: AppError? = null,
)

class ResultViewModel(
    savedStateHandle: SavedStateHandle,
    private val identify: IdentifyCoinUseCase,
    private val scans: ScanRepository,
    private val draft: ScanDraft,
    private val addToCollection: AddToCollectionUseCase,
) : ViewModel() {
    private var scanId = savedStateHandle.toRoute<Route.Result>().scanId
    private val _state = MutableStateFlow<UiState<ResultData>>(UiState.Loading)
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun addToCabinet() {
        val result = _state.value as? UiState.Success ?: return
        viewModelScope.launch {
            when (val saved = addToCollection(AddToCollectionUseCase.Params(scanId = result.data.record.id, grade = Grade.GOOD))) {
                is AppResult.Success -> _state.value = UiState.Success(result.data.copy(addedToCabinet = true))
                is AppResult.Failure -> _state.value = UiState.Success(result.data.copy(collectionError = saved.error))
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val id = scanId ?: run {
                val images = draft.current.toImages()
                if (images.isEmpty()) {
                    _state.value = UiState.Error(AppError.Validation("photo", "Choose a coin photo first"))
                    return@launch
                }
                when (val identification = identify(images)) {
                    is AppResult.Success -> identification.data
                    is AppResult.Failure -> {
                        _state.value = UiState.Error(identification.error)
                        return@launch
                    }
                }.also {
                    scanId = it
                    draft.clear()
                }
            }
            when (val record = scans.get(id)) {
                is AppResult.Success -> _state.value = UiState.Success(ResultData(record.data))
                is AppResult.Failure -> _state.value = UiState.Error(record.error)
            }
        }
    }
}
