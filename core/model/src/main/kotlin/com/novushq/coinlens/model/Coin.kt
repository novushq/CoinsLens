package com.novushq.coinlens.model

import kotlinx.serialization.Serializable

/** Fixed disclaimer rendered under every value display (PRD §MVP-3). */
const val VALUATION_DISCLAIMER =
    "Estimates only. Condition, authenticity and market change the value. " +
        "Get an in-hand appraisal before selling."

enum class ItemKind { COIN, BANKNOTE }

enum class Rarity { COMMON, SCARCE, RARE, VERY_RARE, UNKNOWN }

enum class Confidence { HIGH, MEDIUM, LOW }

/** Low–high estimate. Invariant: low <= high. */
@Serializable
data class ValueRange(val low: Money, high: Money) {
    init {
        require(low.currency == high.currency) { "range currencies must match" }
        require(low.cents <= high.cents) { "low must not exceed high" }
    }

    fun midpoint(): Money = Money((low.cents + high.cents) / 2, low.currency)
}

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
}

/** Error/variety hint. Always rendered prefixed with "Possible", never as fact (PRD §MVP-4). */
@Serializable
data class VarietyHint(
    val name: String,
    val description: String = "",
    val whereToLook: String = "",
) {
    fun displayName(): String = "Possible $name"
}

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
        fun unrecognized(description: String = "This doesn't look like a coin or banknote."): CoinIdentification =
            CoinIdentification(
                kind = ItemKind.COIN,
                name = "Unrecognized",
                country = "",
                denomination = "",
                value = ValueEstimate(
                    circulated = ValueRange(usd(0.0), usd(0.0)),
                    confidence = Confidence.LOW,
                ),
                description = description,
                recognized = false,
            )
    }
}
