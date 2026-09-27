package app.novushq.coinlens.common

import kotlinx.coroutines.CancellationException

/**
 * Wraps a suspending call and converts thrown exceptions into [AppError].
 * Cancellation is always rethrown: swallowing it breaks structured concurrency.
 */
suspend inline fun <T> safeCall(
    crossinline mapError: (Throwable) -> AppError = { AppError.Unknown(it.message ?: "Unknown", it) },
    crossinline block: suspend () -> T,
): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    AppResult.Failure(mapError(e))
}
