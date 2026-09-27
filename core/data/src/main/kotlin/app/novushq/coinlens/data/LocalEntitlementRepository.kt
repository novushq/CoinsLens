package app.novushq.coinlens.data

import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.domain.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default entitlement source: never Pro unless toggled (debug tools / tests).
 * feature/paywall overrides the Koin binding with its store-backed implementation.
 */
class LocalEntitlementRepository(initialPro: Boolean = false) : EntitlementRepository {
    private val pro = MutableStateFlow(initialPro)

    override val isPro: Flow<Boolean> = pro.asStateFlow()

    override suspend fun refresh(): AppResult<Boolean> = AppResult.Success(pro.value)

    override suspend fun restore(): AppResult<Boolean> = AppResult.Success(pro.value)

    fun setPro(value: Boolean) {
        pro.value = value
    }
}
