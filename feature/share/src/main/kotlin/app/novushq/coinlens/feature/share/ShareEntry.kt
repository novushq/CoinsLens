package app.novushq.coinlens.feature.share

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
 * Koin bindings for :feature:share. Loaded by :app after all core modules
 * (so a binding here may override a core default).
 */
val shareModule = module {
}

/** Registers every :feature:share destination (docs/NAVIGATION.md). Placeholder UI until the feature lands. */
fun NavGraphBuilder.shareGraph(navigator: AppNavigator) {
    composable<Route.Share> {
        SharePlaceholder(title = stringResource(R.string.share_title)) {
        }
    }
}

@Composable
private fun SharePlaceholder(title: String, actions: @Composable () -> Unit = {}) {
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
