package app.novushq.coinlens.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale 4/8/12/16/24/32/48. Use these instead of raw dp in features. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Radii: small for chips, medium for cards, large for sheets and the result card top. */
object Radii {
    val small = 8.dp
    val medium = 16.dp
    val large = 28.dp
}

/** Motion durations in ms (docs/DESIGN.md: 200–280ms, count-up ≤600ms). */
object Motion {
    const val SHORT = 200
    const val MEDIUM = 280
    const val ENTER = 400
    const val COUNT_UP = 600
    const val SHUTTER_PRESS_SCALE = 0.92f
    const val SHIMMER = 1200
    val Emphasized = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}

/** Fixed component sizes shared across features. */
object Sizes {
    val coinFrameLarge = 200.dp
    val coinFrameMedium = 120.dp
    val coinFrameSmall = 56.dp
    val coinRing = 2.dp
    val minTouch = 48.dp
    val buttonHeight = 56.dp
    val shutter = 72.dp
    val shutterInner = 58.dp
    val rangeBand = 6.dp
    val stateIcon = 64.dp
    val shimmerBand = 120.dp
    val spinner = 20.dp
    val spinnerStroke = 2.dp
    val shutterIcon = 30.dp
}

/** Hairlines and outlines. */
object Stroke {
    val hairline = 1.dp
}

/**
 * Elevation is tonal first (surfaceContainer levels); these small shadows are
 * reserved for elements that must lift off busy content: buttons, hero cards,
 * the coin frame and the shutter.
 */
object Elevations {
    val button = 2.dp
    val buttonPressed = 4.dp
    val card = 1.dp
    val hero = 6.dp
    val coin = 6.dp
}

val CoinShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.small / 2),
    small = RoundedCornerShape(Radii.small),
    medium = RoundedCornerShape(Radii.medium),
    large = RoundedCornerShape(Radii.large),
    extraLarge = RoundedCornerShape(Radii.large),
)

/** Screen gutter: 16dp, or 24dp on windows at least 600dp wide. */
@Composable
@ReadOnlyComposable
fun screenGutter(): Dp = if (LocalConfiguration.current.screenWidthDp >= 600) Spacing.xl else Spacing.lg
