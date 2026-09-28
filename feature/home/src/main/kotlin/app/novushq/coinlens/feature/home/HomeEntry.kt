package app.novushq.coinlens.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.CoinFrame
import app.novushq.coinlens.designsystem.component.DisclaimerText
import app.novushq.coinlens.designsystem.component.MoneyRangeText
import app.novushq.coinlens.designsystem.component.MoneyText
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.screenGutter
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.ScanAllowance
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.PaywallSource
import app.novushq.coinlens.navigation.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
}

fun NavGraphBuilder.homeGraph(navigator: AppNavigator) {
    composable<Route.Home> {
        val viewModel: HomeViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        HomeContent(
            state = state,
            onRetry = viewModel::retry,
            onScan = { allowance ->
                navigator.navigate(
                    if (allowance.canScan) Route.Capture else Route.Paywall(PaywallSource.QUOTA),
                )
            },
            onSettings = { navigator.navigate(Route.Settings) },
            onOpenScan = { navigator.navigate(Route.Result(it)) },
        )
    }
}

@Composable
private fun HomeContent(
    state: UiState<HomeData>,
    onRetry: () -> Unit,
    onScan: (ScanAllowance) -> Unit,
    onSettings: () -> Unit,
    onOpenScan: (String) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screenGutter(), vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinFrame(
                    obverse = null,
                    contentDescription = null,
                    size = app.novushq.coinlens.designsystem.theme.Sizes.coinFrameSmall,
                )
                Text(
                    text = stringResource(R.string.home_title),
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSettings) {
                    Text(stringResource(R.string.home_settings))
                }
            }
        },
    ) { insets ->
        UiStateSurface(
            state = state,
            onRetry = onRetry,
            modifier = Modifier.padding(insets),
        ) { data ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = screenGutter(),
                    vertical = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            text = stringResource(R.string.home_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.home_headline),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.home_intro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item { ScanInvitation(onScan, data.allowance) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        SectionHeader(title = stringResource(R.string.home_collection))
                        CabinetSummary(data.summary)
                    }
                }
                item {
                    SectionHeader(title = stringResource(R.string.home_recent))
                }
                if (data.recent.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.home_recent_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(data.recent, key = { it.id }) { scan ->
                        Surface(
                            onClick = { onOpenScan(scan.id) },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            ) {
                                CoinFrame(
                                    obverse = scan.obversePath,
                                    contentDescription = scan.identification.name,
                                    size = app.novushq.coinlens.designsystem.theme.Sizes.coinFrameSmall,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scan.identification.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = scan.identification.country,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                scan.identification.value.circulated?.let { MoneyRangeText(it) }
                            }
                        }
                    }
                }
                item { DisclaimerText() }
            }
        }
    }
}

@Composable
private fun ScanInvitation(onScan: (ScanAllowance) -> Unit, allowance: ScanAllowance) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (allowance.isPro) R.string.home_pro_badge else R.string.home_free_badge,
                        if (allowance.isPro) 0 else allowance.freeRemaining + allowance.bonusRemaining,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), CircleShape)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
                Spacer(Modifier.weight(1f))
                CoinFrame(
                    obverse = null,
                    contentDescription = null,
                    size = app.novushq.coinlens.designsystem.theme.Sizes.coinFrameMedium,
                )
            }
            Text(
                text = stringResource(R.string.home_scan_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.home_scan_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            PrimaryButton(
            text = stringResource(if (allowance.canScan) R.string.home_scan_action else R.string.home_view_plans),
            onClick = { onScan(allowance) },
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            )
            if (!allowance.canScan) {
                Text(
                    text = stringResource(R.string.home_quota_used),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun CabinetSummary(summary: CollectionSummary) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = stringResource(
                        if (summary.itemCount == 0) R.string.home_no_estimate else R.string.home_total_estimate,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (summary.itemCount > 0) {
                    MoneyText(
                        summary.totalMid,
                        style = MaterialTheme.typography.headlineMedium,
                        animate = true,
                    )
                    MoneyRangeText(ValueRange(summary.totalLow, summary.totalHigh))
                } else {
                    Text(
                        text = stringResource(R.string.home_cabinet_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = stringResource(R.string.home_piece_count, summary.itemCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun HomePreview() {
    CoinLensTheme {
        HomeContent(
            state = UiState.Success(
                HomeData(CollectionSummary(), ScanAllowance(), emptyList()),
            ),
            onRetry = {},
        onScan = { _ -> },
            onSettings = {},
            onOpenScan = {},
        )
    }
}
