package app.novushq.coinlens.common

/** Canonical screen state envelope. Feature ViewModels expose StateFlow<UiState<T>>. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String = "Nothing here yet") : UiState<Nothing>
    data class Error(val error: AppError, val retryable: Boolean = true) : UiState<Nothing>
}

fun <T> AppResult<T>.toUiState(isEmpty: (T) -> Boolean = { false }): UiState<T> = when (this) {
    is AppResult.Success -> if (isEmpty(data)) UiState.Empty() else UiState.Success(data)
    is AppResult.Failure -> UiState.Error(error)
}
