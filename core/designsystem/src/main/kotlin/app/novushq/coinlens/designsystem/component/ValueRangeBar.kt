package app.novushq.coinlens.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import app.novushq.coinlens.designsystem.R
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.coinExtras
import app.novushq.coinlens.model.Confidence
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.ValueRange

/** Pure scale maths for [ValueRangeBar], kept separate so it is unit-testable. */
internal data class RangeScale(val min: Long, val max: Long) {
    /** Fraction 0..1 of [cents] along the scale. */
    fun fraction(cents: Long): Float {
        val span = (max - min).coerceAtLeast(1)
        return ((cents - min).toFloat() / span).coerceIn(0f, 1f)
    }

    companion object {
        fun of(vararg ranges: ValueRange?): RangeScale? {
            val present = ranges.filterNotNull()
            if (present.isEmpty()) return null
            return RangeScale(present.minOf { it.low.cents }, present.maxOf { it.high.cents })
        }
    }
}

/**
 * Signature value display: circulated band (tonal) and uncirculated band (primary) on one scale,
 * with tabular low/high labels and an optional [ConfidencePill] at the right end.
 */
@Composable
fun ValueRangeBar(
    circulated: ValueRange?,
    uncirculated: ValueRange?,
    modifier: Modifier = Modifier,
    confidence: Confidence? = null,
) {
    val scale = RangeScale.of(circulated, uncirculated) ?: return
    val scheme = MaterialTheme.colorScheme
    val gold = coinExtras
    val locale = LocalConfiguration.current.locales[0]
    val circulatedLabel = stringResource(R.string.ds_circulated)
    val uncirculatedLabel = stringResource(R.string.ds_uncirculated)
    val bands = buildString {
        circulated?.let {
            append(stringResource(R.string.ds_value_band_description, circulatedLabel, formatMoney(it.low, locale), formatMoney(it.high, locale)))
        }
        uncirculated?.let {
            append(' ')
            append(stringResource(R.string.ds_value_band_description, uncirculatedLabel, formatMoney(it.low, locale), formatMoney(it.high, locale)))
        }
    }
    val description = stringResource(R.string.ds_value_range_description, bands.trim())

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.md),
            ) {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(color = scheme.surfaceContainerHighest, cornerRadius = radius)
                circulated?.let { band ->
                    val start = scale.fraction(band.low.cents) * size.width
                    val end = scale.fraction(band.high.cents) * size.width
                    drawRoundRect(
                        color = scheme.primaryContainer,
                        topLeft = Offset(start, 0f),
                        size = Size((end - start).coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
                uncirculated?.let { band ->
                    val bandHeight = Sizes.rangeBand.toPx()
                    val inset = (size.height - bandHeight) / 2
                    val start = scale.fraction(band.low.cents) * size.width
                    val end = scale.fraction(band.high.cents) * size.width
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            0f to gold.goldDeep,
                            1f to gold.goldBright,
                            startX = start,
                            endX = end.coerceAtLeast(start + bandHeight),
                        ),
                        topLeft = Offset(start, inset),
                        size = Size((end - start).coerceAtLeast(bandHeight), bandHeight),
                        cornerRadius = CornerRadius(bandHeight / 2, bandHeight / 2),
                    )
                    drawCircle(
                        color = scheme.primary,
                        radius = size.height / 2,
                        center = Offset(end.coerceIn(size.height / 2, size.width - size.height / 2), size.height / 2),
                    )
                }
            }
            if (confidence != null) {
                Spacer(Modifier.width(Spacing.sm))
                ConfidencePill(confidence)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MoneyText(Money(scale.min), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            MoneyText(Money(scale.max), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        circulated?.let { LegendRow(circulatedLabel, scheme.primaryContainer, it) }
        uncirculated?.let { LegendRow(uncirculatedLabel, scheme.primary, it) }
    }
}

@Composable
private fun LegendRow(label: String, swatch: Color, range: ValueRange) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Box(Modifier.size(Spacing.md).background(swatch, CircleShape))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        MoneyRangeText(range, style = MaterialTheme.typography.titleMedium)
    }
}
