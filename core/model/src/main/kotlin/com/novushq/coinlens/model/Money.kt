package com.novushq.coinlens.model

import kotlinx.serialization.Serializable

/** Money in minor units. Currency is an ISO 4217 code, default USD (PRD: ranges in USD). */
@Serializable
data class Money(
    val cents: Long,
    val currency: String = "USD",
) {
    init {
        require(currency.isNotBlank()) { "currency must not be blank" }
    }
}

/** Pure-Kotlin display string, e.g. "$12.50". Rich locale formatting lives in designsystem. */
fun Money.toDisplayString(): String {
    val major = cents / 100
    val minor = (cents % 100).toString().padStart(2, '0')
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "GBP" -> "£"
        "EUR" -> "€"
        "INR" -> "₹"
        "CAD" -> "CA$"
        else -> "${currency.uppercase()} "
    }
    return "$symbol$major.$minor"
}

fun usd(priceDollars: Double): Money = Money((priceDollars * 100).toLong(), "USD")
