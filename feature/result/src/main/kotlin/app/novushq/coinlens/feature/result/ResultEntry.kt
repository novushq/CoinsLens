package app.novushq.coinlens.feature.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.BackIconButton
import app.novushq.coinlens.designsystem.component.BrassDivider
import app.novushq.coinlens.designsystem.component.CloseIconButton
import app.novushq.coinlens.designsystem.component.CoinFrame
import app.novushq.coinlens.designsystem.component.DisclaimerText
import app.novushq.coinlens.designsystem.component.MetadataRow
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SecondaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.TertiaryButton
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.component.ValueRangeBar
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.Stroke
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val resultModule = module {
    viewModelOf(::ResultViewModel)
}

fun NavGraphBuilder.resultGraph(navigator: AppNavigator) {
    composable<Route.Result> {
        val viewModel: ResultViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ResultContent(
            state = state,
            isFresh = viewModel.isFresh,
            onRetry = viewModel::retry,
            onAddToCabinet = viewModel::addToCabinet,
            onShare = { id -> navigator.navigate(Route.Share(id)) },
            onScanAnother = { navigator.navigate(Route.Capture) },
            onBack = navigator::back,
            onCloseToHome = { navigator.navigate(Route.Home, popUpTo = Route.Capture, inclusive = true) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultContent(
    state: UiState<ResultData>,
    isFresh: Boolean,
    onRetry: () -> Unit,
    onAddToCabinet: () -> Unit,
    onShare: (String) -> Unit,
    onScanAnother: () -> Unit,
    onBack: () -> Unit,
    onCloseToHome: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val addedMessage = stringResource(R.string.result_added_message)
    val added = (state as? UiState.Success)?.data?.addedToCabinet == true
    LaunchedEffect(added) {
        if (added) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            snackbar.showSnackbar(addedMessage)
        }
    }
    val revealed = state is UiState.Success
    LaunchedEffect(revealed) {
        if (revealed) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    if (isFresh) CloseIconButton(onCloseToHome) else BackIconButton(onBack)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        UiStateSurface(state = state, onRetry = onRetry, modifier = Modifier.padding(insets)) { data ->
            val coin = data.record.identification
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Spacing.xl,
                    vertical = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        CoinFrame(
                            obverse = data.record.obversePath,
                            reverse = data.record.reversePath,
                            contentDescription = if (coin.recognized) coin.name else stringResource(R.string.result_unknown_photo),
                            size = Sizes.coinFrameLarge,
                        )
                        if (coin.recognized) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(Stroke.hairline, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.lg),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                ) {
                                    Text(
                                        text = coin.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics { heading() },
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    MetadataRow(
                                        listOf(coin.country, coin.year?.toString() ?: coin.yearText, coin.mintMark ?: coin.mint.orEmpty()),
                                    )
                                    BrassDivider()
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.result_unrecognized),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(coin.description, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (coin.recognized) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            SectionHeader(title = stringResource(R.string.result_value))
                            ValueRangeBar(
                                circulated = coin.value.circulated,
                                uncirculated = coin.value.uncirculated,
                                confidence = coin.value.confidence,
                            )
                            DisclaimerText()
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SectionHeader(title = stringResource(R.string.result_details))
                            val details = buildList {
                                add(stringResource(R.string.result_denomination) to coin.denomination)
                                add(stringResource(R.string.result_composition) to coin.composition.orEmpty())
                                add(stringResource(R.string.result_rarity) to coin.rarity.name.replace('_', ' '))
                                coin.weightGrams?.let { add(stringResource(R.string.result_weight) to stringResource(R.string.result_grams, it)) }
                                coin.diameterMm?.let { add(stringResource(R.string.result_diameter) to stringResource(R.string.result_millimeters, it)) }
                            }.filter { it.second.isNotBlank() }
                            details.forEachIndexed { index, (label, value) ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = Stroke.hairline)
                                }
                                DetailRow(label, value)
                            }
                            if (coin.description.isNotBlank()) {
                                Text(coin.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (coin.hints.isNotEmpty()) {
                        item { SectionHeader(title = stringResource(R.string.result_hints)) }
                        items(coin.hints, key = { "${it.name}-${it.whereToLook}" }) { hint ->
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(
                                    stringResource(R.string.result_possible_hint, hint.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (hint.description.isNotBlank()) Text(hint.description, style = MaterialTheme.typography.bodyMedium)
                                if (hint.whereToLook.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.result_where_to_look, hint.whereToLook),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    if (coin.alternatives.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                SectionHeader(title = stringResource(R.string.result_alternatives))
                                coin.alternatives.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            PrimaryButton(
                                text = stringResource(if (data.addedToCabinet) R.string.result_added else R.string.result_add),
                                onClick = onAddToCabinet,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !data.addedToCabinet,
                                icon = Icons.Outlined.Add,
                            )
                            data.collectionError?.let {
                                Text(stringResource(R.string.result_add_error), color = MaterialTheme.colorScheme.error)
                            }
                            SecondaryButton(
                                text = stringResource(R.string.result_share),
                                onClick = { onShare(data.record.id) },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Outlined.Share,
                            )
                            TertiaryButton(
                                text = stringResource(R.string.result_scan_another),
                                onClick = onScanAnother,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    item {
                        PrimaryButton(
                            text = stringResource(R.string.result_try_again),
                            onClick = onScanAnother,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
