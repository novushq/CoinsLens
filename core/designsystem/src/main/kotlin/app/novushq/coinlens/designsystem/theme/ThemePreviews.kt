package app.novushq.coinlens.designsystem.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Light + dark previews in one annotation. Wrap preview bodies in [CoinLensTheme]. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews
