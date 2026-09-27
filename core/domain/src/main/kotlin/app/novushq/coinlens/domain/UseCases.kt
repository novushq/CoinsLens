package app.novushq.coinlens.domain

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.flatMap
import app.novushq.coinlens.identify.IdentifyEngine
import app.novushq.coinlens.identify.ImageInput
import app.novushq.coinlens.identify.ImageRole
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.FREE_SCAN_COUNT
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.model.ScanAllowance
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.ValuedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID

/** Pro, free (1 per install) and rewarded-bonus scans combined. */
class ObserveScanAllowanceUseCase(
    private val entitlements: EntitlementRepository,
    private val prefs: PreferencesRepository,
) {
    operator fun invoke(): Flow<ScanAllowance> = combine(
        entitlements.isPro,
        prefs.freeScansUsed,
        prefs.bonusScans,
    ) { isPro, used, bonus ->
        ScanAllowance(
            isPro = isPro,
            freeRemaining = (FREE_SCAN_COUNT - used).coerceAtLeast(0),
            bonusRemaining = bonus.coerceAtLeast(0),
        )
    }.distinctUntilChanged()
}

/**
 * allowance check → save photos → engine → persist [ScanRecord] → consume one
 * scan → scan id. Allowance is consumed only for a successful, recognized
 * identification; any failure consumes nothing and removes the saved photos.
 * Bonus scans are spent before the free one. Pro never consumes.
 */
class IdentifyCoinUseCase(
    private val engine: IdentifyEngine,
    private val spec: CoinIdentificationSpec,
    private val scans: ScanRepository,
    private val imageStore: ImageStore,
    private val prefs: PreferencesRepository,
    private val allowance: ObserveScanAllowanceUseCase,
    private val clock: () -> Instant = Instant::now,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    /** [images]: obverse ([ImageRole.PRIMARY]) first, optional reverse ([ImageRole.SECONDARY]). */
    suspend operator fun invoke(images: List<ImageInput>): AppResult<String> {
        val obverse = images.firstOrNull { it.role == ImageRole.PRIMARY }
            ?: return AppResult.Failure(AppError.Validation("images", "Obverse photo is required"))
        val reverse = images.firstOrNull { it.role == ImageRole.SECONDARY }
        val current = allowance().first()
        if (!current.canScan) return AppResult.Failure(AppError.ScanLimitReached())

        val saved = mutableListOf<String>()
        val result = imageStore.save(obverse.bytes, "obverse")
            .flatMap { path ->
                saved += path
                if (reverse == null) {
                    AppResult.Success(null)
                } else {
                    imageStore.save(reverse.bytes, "reverse").also { r -> r.getOrNull()?.let { saved += it } }
                }
            }
            .flatMap { reversePath ->
                engine.identify(spec, listOfNotNull(obverse, reverse)).flatMap { identification ->
                    scans.save(
                        ScanRecord(
                            id = newId(),
                            createdAt = clock(),
                            obversePath = saved.first(),
                            reversePath = reversePath,
                            identification = identification,
                        ),
                    ).flatMap { id -> AppResult.Success(id to identification.recognized) }
                }
            }

        return when (result) {
            is AppResult.Failure -> {
                saved.forEach { imageStore.delete(it) }
                result
            }
            is AppResult.Success -> {
                val (id, recognized) = result.data
                if (recognized && !current.isPro) consumeOne(current)
                AppResult.Success(id)
            }
        }
    }

    private suspend fun consumeOne(current: ScanAllowance) {
        if (current.bonusRemaining > 0) {
            prefs.setBonusScans(current.bonusRemaining - 1)
        } else {
            prefs.setFreeScansUsed(prefs.freeScansUsed.first() + 1)
        }
    }
}

/** Called by the paywall after a rewarded ad completes: exactly one extra scan. */
class GrantBonusScanUseCase(private val prefs: PreferencesRepository) {
    suspend operator fun invoke(): AppResult<Unit> = prefs.setBonusScans(prefs.bonusScans.first() + 1)
}

/**
 * Grade-aware totals: uncirculated range for AU/MS/PR, circulated otherwise
 * (see [app.novushq.coinlens.model.ValueEstimate.rangeFor]); midpoint × quantity.
 * Unrecognized scans count as items but add no value.
 */
class ComputeCollectionValueUseCase {
    operator fun invoke(items: List<ValuedItem>): CollectionSummary {
        var low = Money.ZERO
        var high = Money.ZERO
        var mid = Money.ZERO
        var cost = Money.ZERO
        val byFolder = mutableMapOf<String, Money>()
        items.forEach { valued ->
            val qty = valued.item.quantity
            if (valued.scan.identification.recognized) {
                val range = valued.range
                low += range.low * qty
                high += range.high * qty
                mid += range.midpoint * qty
                val key = valued.item.folderId ?: CollectionSummary.UNSORTED
                byFolder[key] = (byFolder[key] ?: Money.ZERO) + range.midpoint * qty
            }
            valued.item.purchasePrice?.let { cost += it * qty }
        }
        return CollectionSummary(
            itemCount = items.sumOf { it.item.quantity },
            totalLow = low,
            totalHigh = high,
            totalMid = mid,
            totalCost = cost,
            byFolder = byFolder,
        )
    }
}

class AddToCollectionUseCase(
    private val repo: CollectionRepository,
    private val clock: () -> Instant = Instant::now,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    data class Params(
        val scanId: String,
        val folderId: String? = null,
        val grade: Grade? = null,
        val gradeNotes: String = "",
        val purchasePrice: Money? = null,
        val quantity: Int = 1,
    )

    /** Returns the new item id. */
    suspend operator fun invoke(params: Params): AppResult<String> {
        val error = when {
            params.scanId.isBlank() -> AppError.Validation("scanId", "Scan is required")
            params.quantity < 1 -> AppError.Validation("quantity", "Quantity must be at least 1")
            (params.purchasePrice?.cents ?: 0) < 0 -> AppError.Validation("purchasePrice", "Price cannot be negative")
            else -> null
        }
        if (error != null) return AppResult.Failure(error)
        return repo.addItem(
            CollectionItem(
                id = newId(),
                scanId = params.scanId,
                folderId = params.folderId,
                grade = params.grade,
                gradeNotes = params.gradeNotes.trim(),
                purchasePrice = params.purchasePrice,
                quantity = params.quantity,
                addedAt = clock(),
            ),
        )
    }
}

/** Persists the quiz answer (null = skipped) and marks onboarding done so launch starts at Home. */
class CompleteOnboardingUseCase(private val prefs: PreferencesRepository) {
    suspend operator fun invoke(persona: Persona?): AppResult<Unit> =
        prefs.setPersona(persona).flatMap { prefs.setOnboardingDone(true) }
}
