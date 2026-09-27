package com.novushq.coinlens.model

import kotlinx.serialization.Serializable

/** Money in minor units. Currency is an ISO 4217 code, default USD (PRD: ranges in USD). */
@Serializable
data class Money(
    val cents: Long,
    val currency: String = USD,
) {
    init {
        require(currency.isNotBlank()) { "currency must not be blank" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "cannot add $currency to ${other.currency}" }
        return copy(cents = cents + other.cents)
    }

    operator fun times(quantity: Int): Money = copy(cents = cents * quantity)

    companion object {
        const val USD = "USD"
        val ZERO = Money(0)
    }
}

/** Pure-Kotlin fallback display string, e.g. "$12.50". Locale formatting lives in designsystem's MoneyText. */
fun Money.toDisplayString(): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val major = abs / 100
    val minor = (abs % 100).toString().padStart(2, '0')
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "GBP" -> "£"
        "EUR" -> "€"
        "INR" -> "₹"
        "CAD" -> "CA$"
        else -> "${currency.uppercase()} "
    }
    return "$sign$symbol$major.$minor"
}

/** Dollars → Money, rounded to the nearest cent. */
fun usd(dollars: Double): Money = Money(kotlin.math.round(dollars * 100).toLong(), Money.USD)
