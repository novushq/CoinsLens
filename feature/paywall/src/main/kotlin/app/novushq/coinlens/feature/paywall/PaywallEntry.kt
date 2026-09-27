package app.novushq.coinlens.feature.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import org.koin.dsl.module

/**
 * Koin bindings for :feature:paywall. Loaded by :app after all core modules
 * (so a binding here may override a core default).
 */
val paywallModule = module {
}

/** Registers every :feature:paywall destination (docs/NAVIGATION.md). Placeholder UI until the feature lands. */
fun NavGraphBuilder.paywallGraph(navigator: AppNavigator) {
    composable<Route.Paywall> {
        PaywallPlaceholder(title = stringResource(R.string.paywall_title)) {
        }
    }
}

@Composable
private fun PaywallPlaceholder(title: String, actions: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        actions()
    }
}
