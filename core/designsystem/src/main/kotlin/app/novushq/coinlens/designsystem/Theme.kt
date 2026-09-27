package app.novushq.coinlens.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color.White,
    secondary = Color(0xFF4C6358),
    background = Color(0xFFFBFDF9),
    surface = Color(0xFFFBFDF9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF54DBAB),
    onPrimary = Color(0xFF003828),
    secondary = Color(0xFFB3CCBF),
    background = Color(0xFF0F1512),
    surface = Color(0xFF0F1512),
)

/**
 * Placeholder theme. `docs/DESIGN.md` decides this app's real palette, type
 * scale and spacing; the first design-system task replaces the values below
 * with it. Shipping these is shipping the factory default.
 *
 * Dynamic colour is off by default on purpose: a designed palette that is
 * replaced by the user's wallpaper on every Android 12+ device is a palette
 * nobody ever sees, including in the store screenshots.
 */
@Composable
fun CoinlensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
