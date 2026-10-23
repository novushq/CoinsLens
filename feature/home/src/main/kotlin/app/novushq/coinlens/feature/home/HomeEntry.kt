package app.novushq.coinlens.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.CoinFrame
import app.novushq.coinlens.designsystem.component.DisclaimerText
import app.novushq.coinlens.designsystem.component.EmptyState
import app.novushq.coinlens.designsystem.component.MoneyRangeText
import app.novushq.coinlens.designsystem.component.MoneyText
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.CoinTextStyles
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.screenGutter
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.ScanAllowance
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.usd
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
            onAction = viewModel::onAction,
            onScan = { allowance ->
                navigator.navigate(
                    if (allowance.canScan) Route.Capture else Route.Paywall(PaywallSource.QUOTA),
                )
            },
            onFirstScan = { navigator.navigate(Route.Capture) },
            onOpenCollection = { navigator.navigate(Route.Collection) },
            onOpenSettings = { navigator.navigate(Route.Settings) },
            onOpenScan = { navigator.navigate(Route.Result(it)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: UiState<HomeData>,
    onAction: (HomeAction) -> Unit,
    onScan: (ScanAllowance) -> Unit,
    onFirstScan: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val data = (state as? UiState.Success)?.data
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenCollection) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = stringResource(R.string.home_cd_collection),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.home_cd_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (data != null) {
                ScanFab(allowance = data.allowance, onScan = { onScan(data.allowance) })
            }
        },
    ) { insets ->
        UiStateSurface(
            state = state,
            onRetry = { onAction(HomeAction.Retry) },
            modifier = Modifier.padding(insets),
            empty = { message ->
                EmptyState(
                    message = message,
                    action = {
                        OutlinedButton(onClick = onFirstScan) {
                            Text(stringResource(R.string.home_scan))
                        }
                    },
                )
            },
        ) { home ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = screenGutter(),
                    vertical = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                item {
                    WorthHero(summary = home.summary)
                }
                if (!home.allowance.isPro) {
                    item {
                        AllowanceLine(allowance = home.allowance)
                    }
                }
                item {
                    SectionHeader(
                        title = stringResource(R.string.home_collection),
                        actionLabel = stringResource(R.string.home_see_all),
                        onAction = onOpenCollection,
                    )
                }
                item {
                    SectionHeader(title = stringResource(R.string.home_recent))
                }
                if (home.recent.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.home_recent_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            contentPadding = PaddingValues(vertical = Spacing.xs),
                        ) {
                            items(home.recent, key = { it.id }) { scan ->
                                Column(
                                    modifier = Modifier
                                        .width(Sizes.coinFrameMedium)
                                        .clickable { onOpenScan(scan.id) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                ) {
                                    CoinFrame(
                                        obverse = scan.obversePath,
                                        contentDescription = scan.identification.name,
                                        size = Sizes.coinFrameMedium,
                                    )
                                    Text(
                                        text = scan.identification.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val range = scan.identification.value.circulated
                                        ?: scan.identification.value.uncirculated
                                    if (range != null) {
                                        MoneyRangeText(
                                            range = range,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
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
private fun ScanFab(
    allowance: ScanAllowance,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onScan,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
            )
        },
        text = {
            Text(
                stringResource(
                    if (allowance.canScan) R.string.home_scan else R.string.home_view_plans,
                ),
            )
        },
    )
}

@Composable
private fun WorthHero(
    summary: CollectionSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.home_worth_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            money = summary.totalMid,
            style = CoinTextStyles.moneyDisplay,
            color = MaterialTheme.colorScheme.onSurface,
            animate = true,
        )
        MoneyRangeText(
            range = ValueRange(summary.totalLow, summary.totalHigh),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = pluralStringResource(R.plurals.home_piece_count, summary.itemCount, summary.itemCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AllowanceLine(
    allowance: ScanAllowance,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (allowance.freeRemaining > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.home_allowance_free,
                    allowance.freeRemaining,
                    allowance.freeRemaining,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.home_allowance_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (allowance.bonusRemaining > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.home_allowance_bonus,
                    allowance.bonusRemaining,
                    allowance.bonusRemaining,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Success")
@Composable
private fun HomePreviewSuccess() {
    CoinLensTheme {
        HomeContent(
            state = UiState.Success(HomeData(CollectionSummary(), ScanAllowance(), emptyList())),
            onAction = {},
            onScan = {},
            onFirstScan = {},
            onOpenCollection = {},
            onOpenSettings = {},
            onOpenScan = {},
        )
    }
}

@Preview(name = "Empty")
@Composable
private fun HomePreviewEmpty() {
    CoinLensTheme {
        HomeContent(
            state = UiState.Empty(HomeViewModel.EMPTY_STATE_MESSAGE),
            onAction = {},
            onScan = {},
            onFirstScan = {},
            onOpenCollection = {},
            onOpenSettings = {},
            onOpenScan = {},
        )
    }
}

@Preview(name = "Loading")
@Composable
private fun HomePreviewLoading() {
    CoinLensTheme {
        HomeContent(
            state = UiState.Loading,
            onAction = {},
            onScan = {},
            onFirstScan = {},
            onOpenCollection = {},
            onOpenSettings = {},
            onOpenScan = {},
        )
    }
}

@Preview(name = "Error")
@Composable
private fun HomePreviewError() {
    CoinLensTheme {
        HomeContent(
            state = UiState.Error(AppError.Network()),
            onAction = {},
            onScan = {},
            onFirstScan = {},
            onOpenCollection = {},
            onOpenSettings = {},
            onOpenScan = {},
        )
    }
}

@Preview(name = "Success with value")
@Composable
private fun HomePreviewValued() {
    CoinLensTheme {
        HomeContent(
            state = UiState.Success(
                HomeData(
                    summary = CollectionSummary(
                        itemCount = 3,
                        totalLow = usd(12.0),
                        totalHigh = usd(30.0),
                        totalMid = usd(21.0),
                    ),
                    allowance = ScanAllowance(isPro = true, freeRemaining = 0),
                    recent = emptyList(),
                ),
            ),
            onAction = {},
            onScan = {},
            onFirstScan = {},
            onOpenCollection = {},
            onOpenSettings = {},
            onOpenScan = {},
        )
    }
}
