package com.novushq.coinlens.model

import java.time.Instant

/** One identification run. Photos are absolute paths in app-private storage. */
data class ScanRecord(
    val id: String,
    val createdAt: Instant,
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
    PROOF,
    ;

    /** Grades valued with the uncirculated range. */
    val isUncirculated: Boolean
        get() = this == ABOUT_UNCIRCULATED || this == MINT_STATE || this == PROOF
}

data class CollectionItem(
    val id: String,
    val scanId: String,
    val folderId: String? = null,
    val grade: Grade? = null,
    val gradeNotes: String = "",
    val purchasePrice: Money? = null,
    val quantity: Int = 1,
    val addedAt: Instant,
) {
    init {
        require(quantity >= 1) { "quantity must be >= 1" }
    }
}

/** [itemCount] and [totalValue] (grade-aware midpoint × quantity) are derived by the repository. */
data class Folder(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val itemCount: Int = 0,
    val totalValue: Money = Money.ZERO,
)

/** A collection row joined with the scan it was added from. */
data class ValuedItem(
    val item: CollectionItem,
    val scan: ScanRecord,
) {
    /** Range matching the item's grade (see [ValueEstimate.rangeFor]). */
    val range: ValueRange get() = scan.identification.value.rangeFor(item.grade)
}

data class FolderDetail(
    val folder: Folder,
    val items: List<ValuedItem>,
)

/** Dashboard totals, all in USD. [byFolder] keys are folder ids, or [UNSORTED] for items without a folder. */
data class CollectionSummary(
    val itemCount: Int = 0,
    val totalLow: Money = Money.ZERO,
    val totalHigh: Money = Money.ZERO,
    val totalMid: Money = Money.ZERO,
    val totalCost: Money = Money.ZERO,
    val byFolder: Map<String, Money> = emptyMap(),
) {
    companion object {
        const val UNSORTED = "unsorted"
    }
}

enum class Persona { COLLECTOR, INHERITED, DETECTORIST }

/** Free scans granted to every install (PRD: hard paywall after 1 free scan). */
const val FREE_SCAN_COUNT = 1

data class ScanAllowance(
    val isPro: Boolean = false,
    val freeRemaining: Int = FREE_SCAN_COUNT,
    val bonusRemaining: Int = 0,
) {
    val canScan: Boolean get() = isPro || freeRemaining > 0 || bonusRemaining > 0
}
