package app.novushq.coinlens.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import app.novushq.coinlens.designsystem.R
import app.novushq.coinlens.designsystem.theme.Elevations
import app.novushq.coinlens.designsystem.theme.Motion
import app.novushq.coinlens.designsystem.theme.Sizes

private const val DisabledAlpha = 0.38f

/**
 * Camera shutter: a 72dp brass-ringed button that springs to 0.92 on press with
 * a CONFIRM haptic (docs/DESIGN.md). The one place a drop shadow is expected.
 */
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = stringResource(R.string.ds_shutter),
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) Motion.SHUTTER_PRESS_SCALE else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "shutterPress",
    )
    Box(
        modifier = modifier
            .size(Sizes.shutter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled && !loading) 1f else DisabledAlpha
            }
            .shadow(Elevations.hero, CircleShape, clip = false)
            .clip(CircleShape)
            .background(scheme.primary, CircleShape)
            .border(Sizes.coinRing, scheme.onPrimary.copy(alpha = 0.6f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClickLabel = contentDescription,
                enabled = enabled && !loading,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Sizes.shutterIcon),
                strokeWidth = Sizes.spinnerStroke,
                color = scheme.onPrimary,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(Sizes.shutterInner)
                    .background(scheme.onPrimary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(Sizes.shutterIcon),
                )
            }
        }
    }
}
