package app.novushq.coinlens.feature.collection

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import org.koin.dsl.module

/** Koin bindings for :feature:collection. :app loads it after every core module. */
val collectionModule = module { }

/** Registers the :feature:collection destinations (docs/NAVIGATION.md). Placeholder UI until the feature lands. */
fun NavGraphBuilder.collectionGraph(navigator: AppNavigator) {
    composable<Route.Collection> {
        FeaturePlaceholder(
            title = R.string.collection_collection_title,
            actions = listOf(
                PlaceholderAction(R.string.collection_collection_action_0) { navigator.back() },
            ),
        )
    }
    composable<Route.Folder> {
        FeaturePlaceholder(
            title = R.string.collection_folder_title,
            actions = listOf(
                PlaceholderAction(R.string.collection_folder_action_0) { navigator.back() },
            ),
        )
    }
    composable<Route.Item> {
        FeaturePlaceholder(
            title = R.string.collection_item_title,
            actions = listOf(
                PlaceholderAction(R.string.collection_item_action_0) { navigator.back() },
            ),
        )
    }
}

private class PlaceholderAction(@StringRes val label: Int, val onClick: () -> Unit)

@Composable
private fun FeaturePlaceholder(
    @StringRes title: Int,
    actions: List<PlaceholderAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        actions.forEach { action ->
            PrimaryButton(
                text = stringResource(action.label),
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
