package app.novushq.coinlens.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.R
import app.novushq.coinlens.designsystem.theme.Motion
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing

/**
 * Every screen renders through this so no feature ships a blank screen:
 * skeleton while loading, an empty state, or an error with retry.
 */
@Composable
fun <T> UiStateSurface(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    loading: @Composable () -> Unit = { SkeletonList() },
    empty: @Composable (message: String) -> Unit = { EmptyState(message = it) },
    content: @Composable (T) -> Unit,
) {
    Box(modifier) {
        when (state) {
            UiState.Loading -> loading()
            is UiState.Empty -> empty(state.message)
            is UiState.Error -> ErrorState(
                error = state.error,
                onRetry = if (state.retryable) onRetry else null,
            )
            is UiState.Success -> content(state.data)
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inventory2,
    action: (@Composable () -> Unit)? = null,
) {
    CenteredMessage(modifier) {
        StateMedallion(icon = icon, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

@Composable
fun ErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    CenteredMessage(modifier) {
        StateMedallion(
            icon = if (error is AppError.Network || error is AppError.Timeout) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline,
            tint = MaterialTheme.colorScheme.error,
            container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        )
        Text(error.userMessage(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.ds_retry)) }
        }
    }
}

@Composable
private fun StateMedallion(
    icon: ImageVector,
    tint: Color,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier = Modifier
            .size(Sizes.stateIcon)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Spacing.xxl))
    }
}

/** Shimmer placeholder rows: a coin circle plus two text lines each, with a travelling sheen. */
@Composable
fun SkeletonList(modifier: Modifier = Modifier, rows: Int = 4) {
    val description = stringResource(R.string.ds_loading)
    val progress by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Motion.SHIMMER, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    val block = MaterialTheme.colorScheme.surfaceContainerHighest
    val sheen = MaterialTheme.colorScheme.surfaceContainerLowest
    val density = LocalDensity.current
    val bandPx = with(density) { Sizes.shimmerBand.roundToPx() }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg)
            .semantics { contentDescription = description },
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            repeat(rows) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Box(Modifier.size(Sizes.coinFrameSmall).background(block, CircleShape))
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Box(Modifier.fillMaxWidth(0.6f).height(Spacing.lg).background(block, MaterialTheme.shapes.small))
                        Box(Modifier.fillMaxWidth(0.35f).height(Spacing.md).background(block, MaterialTheme.shapes.small))
                    }
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(x = (-bandPx + (progress * (widthPx + 2 * bandPx)).toInt()), y = 0) }
                .fillMaxHeight()
                .width(Sizes.shimmerBand)
                .graphicsLayer { alpha = 0.55f }
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to sheen,
                        1f to Color.Transparent,
                    ),
                ),
        )
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        content = content,
    )
}

/** Localised, user-facing copy for an [AppError]. Never shows raw exception text. */
@Composable
fun AppError.userMessage(): String = stringResource(
    when (this) {
        is AppError.Network -> R.string.ds_error_network
        is AppError.Timeout -> R.string.ds_error_timeout
        is AppError.Http -> if (code == 429) R.string.ds_error_quota else R.string.ds_error_server
        is AppError.Storage -> R.string.ds_error_storage
        is AppError.Timeout -> R.string.ds_error_timeout
        is AppError.NotFound -> R.string.ds_error_not_found
        is AppError.ScanLimitReached -> R.string.ds_error_scan_limit
        is AppError.QuotaExceeded -> R.string.ds_error_quota
        is AppError.ContentBlocked -> R.string.ds_error_blocked
        is AppError.Parse -> R.string.ds_error_parse
        is AppError.Validation -> R.string.ds_error_unknown
        is AppError.Unknown -> R.string.ds_error_unknown
    },
)
