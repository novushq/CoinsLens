package app.novushq.coinlens.designsystem.component

import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.toDisplayString
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Locale-aware currency string. [wholeUnits] drops the minor part (e.g. dashboard totals).
 * Unknown currency codes fall back to the model's plain formatter.
 */
fun formatMoney(money: Money, locale: Locale = Locale.getDefault(), wholeUnits: Boolean = false): String {
    val currency = runCatching { Currency.getInstance(money.currency.uppercase()) }.getOrNull()
        ?: return money.toDisplayString()
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        val digits = if (wholeUnits) 0 else currency.defaultFractionDigits.coerceAtLeast(0)
        minimumFractionDigits = digits
        maximumFractionDigits = digits
        roundingMode = RoundingMode.HALF_UP
    }
    val major = money.cents.toBigDecimal().movePointLeft(2)
    return format.format(major)
}
