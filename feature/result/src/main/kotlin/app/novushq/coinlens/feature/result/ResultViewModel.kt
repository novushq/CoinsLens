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
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.domain.IdentifyCoinUseCase
import app.novushq.coinlens.domain.ScanRepository
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.VarietyHint
import app.novushq.coinlens.model.usd
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AddSheetState(
    val folders: List<Folder> = emptyList(),
    val loadingFolders: Boolean = true,
    val folderId: String? = null,
    val newFolderName: String = "",
    val grade: Grade? = null,
    val priceText: String = "",
    val priceError: Boolean = false,
    val notes: String = "",
    val saving: Boolean = false,
    val error: AppError? = null,
)

data class ResultData(
    val record: ScanRecord,
    val addedToCabinet: Boolean = false,
    val expandedHint: VarietyHint? = null,
    val sheet: AddSheetState? = null,
)

class ResultViewModel(
    savedStateHandle: SavedStateHandle,
    private val identify: IdentifyCoinUseCase,
    private val scans: ScanRepository,
    private val draft: ScanDraft,
    private val addToCollection: AddToCollectionUseCase,
    private val collections: CollectionRepository,
) : ViewModel() {
    private var scanId = savedStateHandle.toRoute<Route.Result>().scanId

    /** True when this result came from a fresh identification (no saved id yet). */
    val isFresh: Boolean = scanId == null

    private val _state = MutableStateFlow<UiState<ResultData>>(UiState.Loading)
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun expandHint(hint: VarietyHint) {
        val result = _state.value as? UiState.Success ?: return
        _state.value = UiState.Success(result.data.copy(expandedHint = hint))
    }

    fun dismissHint() {
        val result = _state.value as? UiState.Success ?: return
        _state.value = UiState.Success(result.data.copy(expandedHint = null))
    }

    fun openAddSheet() {
        val result = _state.value as? UiState.Success ?: return
        if (result.data.addedToCabinet) return
        _state.value = UiState.Success(result.data.copy(sheet = AddSheetState()))
        viewModelScope.launch {
            val folders = when (val observed = collections.observeFolders().first()) {
                is AppResult.Success -> observed.data
                is AppResult.Failure -> emptyList()
            }
            val current = _state.value as? UiState.Success ?: return@launch
            val sheet = current.data.sheet ?: return@launch
            _state.value = UiState.Success(
                current.data.copy(sheet = sheet.copy(folders = folders, loadingFolders = false)),
            )
        }
    }

    fun closeAddSheet() {
        val result = _state.value as? UiState.Success ?: return
        _state.value = UiState.Success(result.data.copy(sheet = null))
    }

    fun selectFolder(folderId: String?) = updateSheet { it.copy(folderId = folderId) }

    fun setNewFolderName(name: String) = updateSheet { it.copy(newFolderName = name) }

    fun setGrade(grade: Grade?) = updateSheet { it.copy(grade = grade) }

    fun setPrice(text: String) = updateSheet { it.copy(priceText = text, priceError = false) }

    fun setNotes(notes: String) = updateSheet { it.copy(notes = notes) }

    fun createFolder() {
        val result = _state.value as? UiState.Success ?: return
        val sheet = result.data.sheet ?: return
        val name = sheet.newFolderName.trim()
        if (name.isEmpty() || sheet.saving) return
        viewModelScope.launch {
            setSheetSaving(true)
            when (val created = collections.createFolder(name)) {
                is AppResult.Success -> {
                    val folders = when (val observed = collections.observeFolders().first()) {
                        is AppResult.Success -> observed.data
                        is AppResult.Failure -> sheet.folders
                    }
                    updateSheetState {
                        it.copy(
                            folders = folders,
                            folderId = created.data,
                            newFolderName = "",
                            saving = false,
                            error = null,
                        )
                    }
                }
                is AppResult.Failure -> updateSheetState {
                    it.copy(saving = false, error = created.error)
                }
            }
        }
    }

    fun confirmAdd() {
        val result = _state.value as? UiState.Success ?: return
        val sheet = result.data.sheet ?: return
        if (sheet.saving) return
        val price = parsePrice(sheet.priceText)
        if (price == null && sheet.priceText.isNotBlank()) {
            _state.value = UiState.Success(result.data.copy(sheet = sheet.copy(priceError = true)))
            return
        }
        viewModelScope.launch {
            setSheetSaving(true)
            when (
                val saved = addToCollection(
                    AddToCollectionUseCase.Params(
                        scanId = result.data.record.id,
                        folderId = sheet.folderId,
                        grade = sheet.grade,
                        gradeNotes = sheet.notes.trim(),
                        purchasePrice = price,
                    ),
                )
            ) {
                is AppResult.Success -> _state.value = UiState.Success(
                    result.data.copy(addedToCabinet = true, sheet = null),
                )
                is AppResult.Failure -> updateSheetState { it.copy(saving = false, error = saved.error) }
            }
        }
    }

    private fun updateSheet(transform: (AddSheetState) -> AddSheetState) {
        val result = _state.value as? UiState.Success ?: return
        val sheet = result.data.sheet ?: return
        _state.value = UiState.Success(result.data.copy(sheet = transform(sheet)))
    }

    private fun updateSheetState(transform: (AddSheetState) -> AddSheetState) {
        val result = _state.value as? UiState.Success ?: return
        val sheet = result.data.sheet ?: return
        _state.value = UiState.Success(result.data.copy(sheet = transform(sheet)))
    }

    private fun setSheetSaving(saving: Boolean) = updateSheetState { it.copy(saving = saving) }

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

    companion object {
        /** Blank means no price; otherwise dollars like "12.50". Negative or unparsable is invalid. */
        internal fun parsePrice(text: String): Money? {
            if (text.isBlank()) return null
            val dollars = text.trim().toDoubleOrNull() ?: return null
            if (dollars < 0) return null
            return usd(dollars)
        }
    }
}
