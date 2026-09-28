package app.novushq.coinlens.feature.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.FolderDetail
import app.novushq.coinlens.model.ValuedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CollectionScreenData(
    val folders: List<Folder> = emptyList(),
    val summary: CollectionSummary = CollectionSummary(),
    val items: List<ValuedItem> = emptyList(),
    val folder: FolderDetail? = null,
    val item: ValuedItem? = null,
)

class CollectionViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CollectionRepository,
) : ViewModel() {
    private val folderId = savedStateHandle.get<String>("folderId")
    private val itemId = savedStateHandle.get<String>("itemId")
    private val _state = MutableStateFlow<UiState<CollectionScreenData>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { observe() }

    fun retry() = observe()

    fun createFolder(name: String) = mutate { repository.createFolder(name) }
    fun renameFolder(id: String, name: String) = mutate { repository.renameFolder(id, name) }
    fun deleteFolder(id: String) = mutate { repository.deleteFolder(id) }
    fun deleteItem(id: String) = mutate { repository.deleteItem(id) }
    fun updateItem(item: app.novushq.coinlens.model.CollectionItem) = mutate { repository.updateItem(item) }

    private fun observe() {
        viewModelScope.launch {
            when {
                itemId != null -> combine(repository.observeItem(itemId), repository.observeFolders()) { item, folders ->
                    when {
                        item is AppResult.Failure -> UiState.Error(item.error)
                        folders is AppResult.Failure -> UiState.Error(folders.error)
                        else -> UiState.Success(CollectionScreenData(
                            item = (item as AppResult.Success).data,
                            folders = (folders as AppResult.Success).data,
                        ))
                    }
                }.collect { _state.value = it }
                folderId != null -> repository.observeFolder(folderId).collect { result ->
                    _state.value = when (result) {
                        is AppResult.Success -> UiState.Success(CollectionScreenData(folder = result.data))
                        is AppResult.Failure -> UiState.Error(result.error)
                    }
                }
                else -> combine(repository.observeFolders(), repository.observeSummary(), repository.observeItems()) { folders, summary, items ->
                    when {
                        folders is AppResult.Failure -> UiState.Error(folders.error)
                        summary is AppResult.Failure -> UiState.Error(summary.error)
                        items is AppResult.Failure -> UiState.Error(items.error)
                        else -> UiState.Success(
                            CollectionScreenData(
                                folders = (folders as AppResult.Success).data,
                                summary = (summary as AppResult.Success).data,
                                items = (items as AppResult.Success).data,
                            ),
                        )
                    }
                }.collect { _state.value = it }
            }
        }
    }

    private fun mutate(block: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> Unit // observed Room flows refresh the screen
                is AppResult.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }
}
