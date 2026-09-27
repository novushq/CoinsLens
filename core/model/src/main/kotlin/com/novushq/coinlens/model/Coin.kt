package com.novushq.coinlens.model

import kotlinx.serialization.Serializable

enum class ItemKind { COIN, BANKNOTE }

enum class Rarity { COMMON, SCARCE, RARE, VERY_RARE, UNKNOWN }

enum class Confidence { HIGH, MEDIUM, LOW }

/** Low–high estimate. Invariant: same currency, 0 <= low <= high. */
@Serializable
data class ValueRange(val low: Money, val high: Money) {
    init {
        require(low.currency == high.currency) { "range currencies must match" }
        require(low.cents in 0..high.cents) { "expected 0 <= low <= high" }
    }

    val midpoint: Money get() = Money((low.cents + high.cents) / 2, low.currency)
}

/** At least one of the two ranges is always present. */
@Serializable
data class ValueEstimate(
    val circulated: ValueRange? = null,
    val uncirculated: ValueRange? = null,
    val confidence: Confidence,
    val basis: String = "",
) {
    init {
        require(circulated != null || uncirculated != null) { "at least one range required" }
    }

    /** Grade-aware range: uncirculated for AU/MS/PR, circulated otherwise (falls back to the other band). */
    fun rangeFor(grade: Grade?): ValueRange =
        if (grade?.isUncirculated == true) {
            uncirculated ?: circulated
        } else {
            circulated ?: uncirculated
        } ?: error("unreachable: init guarantees a range")
}

/** Error/variety hint. UI always renders it as "Possible <name>", never as fact (PRD §MVP-4). */
@Serializable
data class VarietyHint(
    val name: String,
    val description: String = "",
    val whereToLook: String = "",
)

@Serializable
data class CoinIdentification(
    val kind: ItemKind,
    val name: String,
    val country: String,
    val denomination: String,
    val year: Int? = null,
    val yearText: String = "",
    val mintMark: String? = null,
    val mint: String? = null,
    val composition: String? = null,
    val weightGrams: Double? = null,
    val diameterMm: Double? = null,
    val rarity: Rarity = Rarity.UNKNOWN,
    val description: String = "",
    val value: ValueEstimate,
    val hints: List<VarietyHint> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val recognized: Boolean = true,
) {
    companion object {
        /** Placeholder for photos that are not a coin or banknote. UI renders its own copy. */
        fun unrecognized(description: String = ""): CoinIdentification = CoinIdentification(
            kind = ItemKind.COIN,
            name = "",
            country = "",
            denomination = "",
            description = description,
            value = ValueEstimate(circulated = ValueRange(Money.ZERO, Money.ZERO), confidence = Confidence.LOW),
            recognized = false,
        )
    }
}
