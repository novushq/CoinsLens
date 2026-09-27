package app.novushq.coinlens.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * Material 3 tonal-spot schemes generated from seed #8C6A2F (aged brass), spec 2021.
 * Surfaces come from the warm neutral palette, never grey. This file is the only
 * place raw colour literals are allowed (guardrail UI-001).
 */

/** Seed colour; also used for the launcher icon and splash. */
val BrassSeed = Color(0xFF8C6A2F)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C580D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEAC),
    onPrimaryContainer = Color(0xFF604100),
    inversePrimary = Color(0xFFF0BF6D),
    secondary = Color(0xFF6E5C40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF8DFBB),
    onSecondaryContainer = Color(0xFF55442A),
    tertiary = Color(0xFF4E6542),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD0EABF),
    onTertiaryContainer = Color(0xFF374C2C),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF201B13),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF201B13),
    surfaceVariant = Color(0xFFEFE0CF),
    onSurfaceVariant = Color(0xFF4E4539),
    surfaceTint = Color(0xFF7C580D),
    inverseSurface = Color(0xFF362F27),
    inverseOnSurface = Color(0xFFFBEFE2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF807567),
    outlineVariant = Color(0xFFD2C4B4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F3),
    surfaceContainer = Color(0xFFF8ECDF),
    surfaceContainerHigh = Color(0xFFF2E6D9),
    surfaceContainerHighest = Color(0xFFECE1D4),
    surfaceContainerLow = Color(0xFFFEF2E5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE4D8CC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0BF6D),
    onPrimary = Color(0xFF432C00),
    primaryContainer = Color(0xFF604100),
    onPrimaryContainer = Color(0xFFFFDEAC),
    inversePrimary = Color(0xFF7C580D),
    secondary = Color(0xFFDBC3A1),
    onSecondary = Color(0xFF3D2E16),
    secondaryContainer = Color(0xFF55442A),
    onSecondaryContainer = Color(0xFFF8DFBB),
    tertiary = Color(0xFFB5CEA4),
    onTertiary = Color(0xFF213618),
    tertiaryContainer = Color(0xFF374C2C),
    onTertiaryContainer = Color(0xFFD0EABF),
    background = Color(0xFF17130B),
    onBackground = Color(0xFFECE1D4),
    surface = Color(0xFF17130B),
    onSurface = Color(0xFFECE1D4),
    surfaceVariant = Color(0xFF4E4539),
    onSurfaceVariant = Color(0xFFD2C4B4),
    surfaceTint = Color(0xFFF0BF6D),
    inverseSurface = Color(0xFFECE1D4),
    inverseOnSurface = Color(0xFF362F27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9B8F80),
    outlineVariant = Color(0xFF4E4539),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3F382F),
    surfaceContainer = Color(0xFF241F17),
    surfaceContainerHigh = Color(0xFF2F2921),
    surfaceContainerHighest = Color(0xFF3A342B),
    surfaceContainerLow = Color(0xFF201B13),
    surfaceContainerLowest = Color(0xFF120D07),
    surfaceDim = Color(0xFF17130B),
)

/**
 * App theme. Dynamic colour is supported but off by default: the brass identity
 * matters in screenshots (docs/DESIGN.md).
 */
@Composable
fun CoinLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoinTypography,
        shapes = CoinShapes,
        content = content,
    )
}
