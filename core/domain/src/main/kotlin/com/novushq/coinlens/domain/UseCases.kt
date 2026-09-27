package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppError
import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.common.DispatcherProvider
import com.novushq.coinlens.identify.IdentifyEngine
import com.novushq.coinlens.identify.ImageInput
import com.novushq.coinlens.model.CollectionItem
import com.novushq.coinlens.model.CollectionSummary
import com.novushq.coinlens.model.FREE_SCAN_COUNT
import com.novushq.coinlens.model.Money
import com.novushq.coinlens.model.Persona
import com.novushq.coinlens.model.ScanAllowance
import com.novushq.coinlens.model.ScanRecord
import com.novushq.coinlens.model.ValuedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

/** Free scans = 1 (PRD). Hidden for Pro via ScanAllowance.canScan. */
class ObserveScanAllowanceUseCase(
    private val entitlements: EntitlementRepository,
    private val prefs: PreferencesRepository,
) {
    operator fun invoke(): Flow<ScanAllowance> = combine(
        entitlements.isPro,
        prefs.freeScansUsed,
        prefs.bonusScans,
    ) { isPro, used, bonus ->
        ScanAllowance(isPro, maxOf(0, FREE_SCAN_COUNT - used), maxOf(0, bonus))
    }
}

/**
 * Checks allowance → saves photos → engine → persists ScanRecord → consumes
 * free/bonus allowance only on success → returns the scan id.
 * Retry after a failure consumes nothing extra: failures never consume.
 */
class IdentifyCoinUseCase(
    private val engine: IdentifyEngine,
    private val scans: ScanRepository,
    private val images: ImageStore,
    private val prefs: PreferencesRepository,
    private val entitlements: EntitlementRepository,
    private val dispatchers: DispatcherProvider,
    private val spec: CoinIdentificationSpec = CoinIdentificationSpec(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val ids: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(images: List<ImageInput>): AppResult<String> = withContext(dispatchers.io) {
        if (images.isEmpty()) return@withContext AppResult.Failure(AppError.Validation("images", "No photos to identify"))
        val allowance = ObserveScanAllowanceUseCase(entitlements, prefs).invoke().first()
        if (!allowance.canScan) return@withContext AppResult.Failure(AppError.QuotaExceeded())
        val isPro = allowance.isPro

        val obverse = this@IdentifyCoinUseCase.images.save(images[0].bytes, "obverse")
        if (obverse is AppResult.Failure) return@withContext obverse.toStorage()
        var reversePath: String? = null
        if (images.size > 1) {
            when (val r = this@IdentifyCoinUseCase.images.save(images[1].bytes, "reverse")) {
                is AppResult.Failure -> return@withContext r.toStorage()
                is AppResult.Success -> reversePath = r.data
            }
        }

        val identified = engine.identify(spec, images)
        if (identified is AppResult.Failure) return@withContext identified

        val record = ScanRecord(
            id = ids(),
            createdAt = clock(),
            obversePath = (obverse as AppResult.Success).data,
            reversePath = reversePath,
            identification = (identified as AppResult.Success).data,
        )
        when (val saved = scans.save(record)) {
            is AppResult.Failure -> return@withContext saved
            is AppResult.Success -> {
                if (!isPro) consumeAllowance()
                return@withContext AppResult.Success(saved.data)
            }
        }
    }

    private suspend fun consumeAllowance() {
        val bonus = prefs.bonusScans.first()
        if (bonus > 0) {
            prefs.setBonusScans(bonus - 1)
        } else {
            prefs.setFreeScansUsed(prefs.freeScansUsed.first() + 1)
        }
    }

    private fun <T> AppResult.Failure.toStorage(): AppResult<T> =
        AppResult.Failure(AppError.Storage(error.message, error.cause))
}

/** Rewarded ad completion grants exactly one bonus scan (paywall only). */
class GrantBonusScanUseCase(private val prefs: PreferencesRepository) {
    suspend operator fun invoke(): AppResult<Unit> {
        val current = prefs.bonusScans.first()
        prefs.setBonusScans(current + 1)
        return AppResult.Success(Unit)
    }
}

/**
 * Grade-aware totals: uncirculated range for AU/MS/Proof grades, circulated
 * otherwise; midpoint × quantity. Missing identifications count as items but add no value.
 */
class ComputeCollectionValueUseCase {
    operator fun invoke(items: List<ValuedItem>): CollectionSummary {
        var low = 0L
        var high = 0L
        var mid = 0L
        var cost = 0L
        val byFolder = mutableMapOf<String, Long>()
        items.forEach { (item, id) ->
            val range = id?.let {
                val unc = item.grade?.isUncirculated == true
                if (unc) it.value.uncirculated ?: it.value.circulated
                else it.value.circulated ?: it.value.uncirculated
            }
            if (range != null) {
                low += range.low.cents * item.quantity
                high += range.high.cents * item.quantity
                mid += range.midpoint().cents * item.quantity
                val key = item.folderId ?: UNSORTED
                byFolder[key] = (byFolder[key] ?: 0) + range.midpoint().cents * item.quantity
            }
            item.purchasePrice?.let { cost += it.cents * item.quantity }
        }
        return CollectionSummary(
            itemCount = items.size,
            totalLow = Money(low),
            totalHigh = Money(high),
            totalMid = Money(mid),
            totalCost = Money(cost),
            byFolder = byFolder.mapValues { Money(it.value) },
        )
    }

    companion object {
        const val UNSORTED = "unsorted"
    }
}

class AddToCollectionUseCase(
    private val repo: CollectionRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ids: () -> String = { UUID.randomUUID().toString() },
) {
    data class Params(
        val scanId: String,
        val folderId: String? = null,
        val grade: com.novushq.coinlens.model.Grade? = null,
        val gradeNotes: String = "",
        val purchasePrice: Money? = null,
        val quantity: Int = 1,
    )

    suspend operator fun invoke(params: Params): AppResult<String> {
        if (params.scanId.isBlank()) {
            return AppResult.Failure(AppError.Validation("scanId", "Scan is required"))
        }
        if (params.quantity < 1) {
            return AppResult.Failure(AppError.Validation("quantity", "Quantity must be at least 1"))
        }
        if (params.purchasePrice != null && params.purchasePrice.cents < 0) {
            return AppResult.Failure(AppError.Validation("purchasePrice", "Price cannot be negative"))
        }
        return repo.addItem(
            CollectionItem(
                id = ids(),
                scanId = params.scanId,
                folderId = params.folderId,
                grade = params.grade,
                gradeNotes = params.gradeNotes,
                purchasePrice = params.purchasePrice,
                quantity = params.quantity,
                addedAt = clock(),
            ),
        )
    }
}

class CompleteOnboardingUseCase(private val prefs: PreferencesRepository) {
    suspend operator fun invoke(persona: Persona?): AppResult<Unit> {
        prefs.setPersona(persona)
        prefs.setOnboardingDone(true)
        return AppResult.Success(Unit)
    }
}

