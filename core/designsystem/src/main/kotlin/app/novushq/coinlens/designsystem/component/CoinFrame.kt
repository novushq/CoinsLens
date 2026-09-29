package app.novushq.coinlens.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import app.novushq.coinlens.designsystem.R
import app.novushq.coinlens.designsystem.theme.Elevations
import app.novushq.coinlens.designsystem.theme.Motion
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.Stroke
import app.novushq.coinlens.designsystem.theme.coinExtras
import coil3.compose.AsyncImage

/**
 * Signature coin photo: always a circle, metallic gold ring, subtle inner shadow.
 * When [reverse] is present a tap flips between obverse and reverse.
 * [obverse]/[reverse] are any Coil model (file path, File, Uri, ByteArray); null shows a placeholder.
 */
@Composable
fun CoinFrame(
    obverse: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    reverse: Any? = null,
    size: Dp = Sizes.coinFrameLarge,
) {
    var showReverse by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (showReverse && reverse != null) 180f else 0f,
        animationSpec = tween(Motion.MEDIUM),
        label = "coinFlip",
    )
    val sideLabel = stringResource(if (rotation > 90f) R.string.ds_reverse else R.string.ds_obverse)
    val flipLabel = stringResource(R.string.ds_flip_hint)
    val scheme = MaterialTheme.colorScheme
    val extras = coinExtras
    val ringBrush = Brush.sweepGradient(listOf(extras.goldBright, extras.goldDeep, extras.goldBright))

    Box(
        modifier = modifier
            .size(size)
            .shadow(Elevations.coin, CircleShape, clip = false)
            .background(scheme.surfaceContainerLow, CircleShape)
            .border(Stroke.hairline, scheme.outlineVariant, CircleShape)
            .padding(Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clip(CircleShape)
                .border(Sizes.coinRing, ringBrush, CircleShape)
                .background(scheme.surfaceContainerHigh)
                .then(
                    if (reverse != null) {
                        Modifier.clickable(role = Role.Button, onClickLabel = flipLabel) { showReverse = !showReverse }
                    } else {
                        Modifier
                    },
                )
                .semantics { stateDescription = sideLabel },
            contentAlignment = Alignment.Center,
        ) {
            val model = if (rotation > 90f) reverse else obverse
            if (model == null) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = stringResource(R.string.ds_no_photo),
                    tint = scheme.onSurfaceVariant,
                )
            } else {
                AsyncImage(
                    model = model,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        // Un-mirror the back face while flipped.
                        .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f }
                        .innerShadow(),
                )
            }
        }
    }
}

private fun Modifier.innerShadow(): Modifier = drawWithContent {
    drawContent()
    drawRect(
        Brush.radialGradient(
            0.78f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.22f),
        ),
    )
}
