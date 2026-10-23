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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * Material 3 tonal-spot schemes generated from seed #1F7A6D (verdigris), spec 2021.
 * Surfaces come from the seed-tinted neutral palette, never flat grey. This file is the only
 * place raw colour literals are allowed (guardrail UI-001).
 */

/** Seed colour; also used for the launcher icon and splash. */
val VerdigrisSeed = Color(0xFF1F7A6D)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F695E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFAAF0E2),
    onPrimaryContainer = Color(0xFF00201B),
    inversePrimary = Color(0xFF8ED4C6),
    secondary = Color(0xFF4A635E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8E1),
    onSecondaryContainer = Color(0xFF06201B),
    tertiary = Color(0xFF326576),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB7EAFF),
    onTertiaryContainer = Color(0xFF001F28),
    background = Color(0xFFF6FAF8),
    onBackground = Color(0xFF181D1B),
    surface = Color(0xFFF6FAF8),
    onSurface = Color(0xFF181D1B),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3E4946),
    surfaceTint = Color(0xFF1F695E),
    inverseSurface = Color(0xFF2C3130),
    inverseOnSurface = Color(0xFFEDF2EF),
    error = Color(0xFFA73736),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD7),
    onErrorContainer = Color(0xFF410004),
    outline = Color(0xFF6E7976),
    outlineVariant = Color(0xFFBEC9C5),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF6FAF8),
    surfaceContainer = Color(0xFFEAEFEC),
    surfaceContainerHigh = Color(0xFFE5E9E7),
    surfaceContainerHighest = Color(0xFFDFE3E1),
    surfaceContainerLow = Color(0xFFF0F5F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD6DBD9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED4C6),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFAAF0E2),
    inversePrimary = Color(0xFF1F695E),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3530),
    secondaryContainer = Color(0xFF334B46),
    onSecondaryContainer = Color(0xFFCDE8E1),
    tertiary = Color(0xFF9CCEE2),
    onTertiary = Color(0xFF003543),
    tertiaryContainer = Color(0xFF154D5D),
    onTertiaryContainer = Color(0xFFB7EAFF),
    background = Color(0xFF0F1413),
    onBackground = Color(0xFFDFE3E1),
    surface = Color(0xFF0F1413),
    onSurface = Color(0xFFDFE3E1),
    surfaceVariant = Color(0xFF3E4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    surfaceTint = Color(0xFF8ED4C6),
    inverseSurface = Color(0xFFDFE3E1),
    inverseOnSurface = Color(0xFF2C3130),
    error = Color(0xFFFFB3AE),
    onError = Color(0xFF67040D),
    errorContainer = Color(0xFF871F21),
    onErrorContainer = Color(0xFFFFDAD7),
    outline = Color(0xFF889390),
    outlineVariant = Color(0xFF3E4946),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353A39),
    surfaceContainer = Color(0xFF1C211F),
    surfaceContainerHigh = Color(0xFF262B2A),
    surfaceContainerHighest = Color(0xFF313634),
    surfaceContainerLow = Color(0xFF181D1B),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceDim = Color(0xFF0F1413),
)

/**
 * Brand colours beyond the M3 roles: metallic gold stops for rings, gradients
 * and value bands, plus a success pair (tertiary is reserved for MEDIUM
 * confidence, so success states need their own green).
 */
@Immutable
data class CoinExtraColors(
    val goldBright: Color,
    val goldDeep: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

private val LightExtras = CoinExtraColors(
    goldBright = Color(0xFFF0BF6D),
    goldDeep = Color(0xFF7C580D),
    success = Color(0xFF3B6B2F),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD8E8C8),
    onSuccessContainer = Color(0xFF1E3A17),
)

private val DarkExtras = CoinExtraColors(
    goldBright = Color(0xFFFFDEAC),
    goldDeep = Color(0xFFF0BF6D),
    success = Color(0xFF9ED488),
    onSuccess = Color(0xFF0F2E0A),
    successContainer = Color(0xFF2A4A22),
    onSuccessContainer = Color(0xFFD8E8C8),
)

private val LocalCoinExtraColors = staticCompositionLocalOf { LightExtras }

/** Brand extension colours for the ambient theme. */
val coinExtras: CoinExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCoinExtraColors.current

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
        content = {
            CompositionLocalProvider(
                LocalCoinExtraColors provides if (darkTheme) DarkExtras else LightExtras,
            ) {
                content()
            }
        },
    )
}
