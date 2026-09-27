package app.novushq.coinlens.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** stateIn with WhileSubscribed(5s): flows stop off-screen but survive rotation. */
fun <T> Flow<T>.stateIn(
    scope: CoroutineScope,
    initial: T,
): StateFlow<T> = stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)
