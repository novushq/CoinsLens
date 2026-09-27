package com.novushq.coinlens.model

import kotlinx.serialization.Serializable

/**
 * Timestamps are epoch millis (Long) rather than java.time types so the model
 * stays trivially @Serializable for the Room JSON column without extra adapters.
 */
@Serializable
data class ScanRecord(
    val id: String,
    val createdAt: Long,
    val obversePath: String,
    val reversePath: String? = null,
    val identification: CoinIdentification,
)

enum class Grade {
    POOR,
    FAIR,
    ABOUT_GOOD,
    GOOD,
    VERY_GOOD,
    FINE,
    VERY_FINE,
    EXTREMELY_FINE,
    ABOUT_UNCIRCULATED,
    MINT_STATE,
    PROOF;

    /** Grades that use the uncirculated range for valuation. */
    val isUncirculated: Boolean
        get() = this == ABOUT_UNCIRCULATED || this == MINT_STATE || this == PROOF
}

@Serializable
data class CollectionItem(
    val id: String,
    val scanId: String,
    val folderId: String? = null,
    val grade: Grade? = null,
    val gradeNotes: String = "",
    val purchasePrice: Money? = null,
    val quantity: Int = 1,
    val addedAt: Long,
) {
    init {
        require(quantity >= 1) { "quantity must be >= 1" }
    }
}

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val createdAt: Long,
    val itemCount: Int = 0,
    val totalValue: Money = Money(0),
)

/** Dashboard totals. All Money values share one currency (USD). */
@Serializable
data class CollectionSummary(
    val itemCount: Int = 0,
    val totalLow: Money = Money(0),
    val totalHigh: Money = Money(0),
    val totalMid: Money = Money(0),
    val totalCost: Money = Money(0),
    /** Folder id (or "unsorted") → midpoint total. */
    val byFolder: Map<String, Money> = emptyMap(),
)

enum class Persona { COLLECTOR, INHERITED, DETECTORIST }

/** Number of free scans granted to every install (PRD: hard paywall after 1 free scan). */
const val FREE_SCAN_COUNT = 1

@Serializable
data class ScanAllowance(
    val isPro: Boolean = false,
    val freeRemaining: Int = FREE_SCAN_COUNT,
    val bonusRemaining: Int = 0,
) {
    val canScan: Boolean get() = isPro || freeRemaining > 0 || bonusRemaining > 0
}

/** One collection row joined with its identification for valuation. */
data class ValuedItem(
    val item: CollectionItem,
    val identification: CoinIdentification?,
)
