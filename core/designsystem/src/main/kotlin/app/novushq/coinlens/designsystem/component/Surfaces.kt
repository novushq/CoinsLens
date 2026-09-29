package app.novushq.coinlens.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import app.novushq.coinlens.designsystem.theme.Elevations
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.Stroke

/**
 * Warm gradient feature card with faint concentric "loupe rings" in the top-end
 * corner. Home for the one hero block per screen (scan invitation, paywall pitch).
 * Content inherits [onPrimaryContainer]-friendly tones: use
 * [androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer] for text.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .shadow(Elevations.hero, MaterialTheme.shapes.large, clip = false)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.primaryContainer),
                ),
            )
            .border(Stroke.hairline, scheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.large),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val origin = Offset(size.width * 0.92f, -size.height * 0.12f)
            val ring = scheme.primary.copy(alpha = 0.10f)
            drawCircle(ring, radius = size.minDimension * 0.55f, center = origin)
            drawCircle(ring, radius = size.minDimension * 0.40f, center = origin)
            drawCircle(scheme.primary.copy(alpha = 0.16f), radius = size.minDimension * 0.25f, center = origin)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            content = content,
        )
    }
}

/** Museum-label divider: hairlines joined by a small brass diamond. */
@Composable
fun BrassDivider(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.minTouch / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(Modifier.weight(1f).height(Stroke.hairline).background(scheme.outlineVariant))
        Box(
            Modifier
                .size(Spacing.sm)
                .graphicsLayer { rotationZ = 45f }
                .background(scheme.primary),
        )
        Box(Modifier.weight(1f).height(Stroke.hairline).background(scheme.outlineVariant))
    }
}

/**
 * Tappable option card with a clear selected state (tonal fill + brass border).
 * Replaces ad-hoc selected Cards in onboarding and the paywall.
 */
@Composable
fun SelectableCard(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) scheme.secondaryContainer else scheme.surfaceContainerLow,
        ),
        border = if (selected) {
            BorderStroke(Sizes.coinRing, scheme.primary)
        } else {
            BorderStroke(Stroke.hairline, scheme.outlineVariant)
        },
        content = content,
    )
}
