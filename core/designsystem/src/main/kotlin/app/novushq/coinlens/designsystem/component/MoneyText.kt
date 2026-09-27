package app.novushq.coinlens.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import app.novushq.coinlens.designsystem.R
import app.novushq.coinlens.designsystem.theme.CoinTextStyles
import app.novushq.coinlens.designsystem.theme.Motion
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.ValueRange
import kotlin.math.roundToLong

/**
 * Money with tabular figures, formatted for the current locale.
 * [animate] counts up once on first reveal (≤600ms) and is skipped when the user disabled animations.
 */
@Composable
fun MoneyText(
    money: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    wholeUnits: Boolean = false,
    animate: Boolean = false,
    textAlign: TextAlign? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val reduceMotion = rememberReduceMotion()
    var revealed by rememberSaveable(money) { mutableStateOf(!animate || reduceMotion) }
    val progress = remember(money) { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(money) {
        if (!revealed) {
            progress.animateTo(1f, tween(Motion.COUNT_UP, easing = FastOutSlowInEasing))
            revealed = true
        }
    }
    val shown = if (progress.value >= 1f) money else money.copy(cents = (money.cents * progress.value).roundToLong())
    Text(
        text = formatMoney(shown, locale, wholeUnits),
        modifier = modifier,
        style = style.merge(CoinTextStyles.money),
        color = color,
        textAlign = textAlign,
    )
}

/** "low–high" for a [ValueRange], tabular. */
@Composable
fun MoneyRangeText(
    range: ValueRange,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    wholeUnits: Boolean = false,
) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text = stringResource(
            R.string.ds_range,
            formatMoney(range.low, locale, wholeUnits),
            formatMoney(range.high, locale, wholeUnits),
        ),
        modifier = modifier,
        style = style.merge(CoinTextStyles.money),
        color = color,
    )
}

@Composable
internal fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
