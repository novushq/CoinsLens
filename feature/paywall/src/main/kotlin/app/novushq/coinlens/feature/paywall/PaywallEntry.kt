package app.novushq.coinlens.feature.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.HeroCard
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SecondaryButton
import app.novushq.coinlens.designsystem.component.SelectableCard
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val paywallModule = module {
    if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
        single<BillingGateway> { FakeBillingGateway(get()) }
        single<app.novushq.coinlens.domain.EntitlementRepository> { get<app.novushq.coinlens.data.LocalEntitlementRepository>() }
    } else {
        single { RevenueCatSdk(get(), BuildConfig.REVENUECAT_API_KEY) }
        single<RevenueCatEntitlementRepository> { RevenueCatEntitlementRepository(get()) }
        single<app.novushq.coinlens.domain.EntitlementRepository> { get<RevenueCatEntitlementRepository>() }
        single<BillingGateway> { RevenueCatBillingGateway(get()) }
    }
    if (BuildConfig.ADMOB_REWARDED_UNIT_ID.isBlank()) {
        single<RewardedAdGateway> { FakeRewardedAdGateway() }
    } else {
        single<RewardedAdGateway> { AdMobRewardedAdGateway(get(), BuildConfig.ADMOB_REWARDED_UNIT_ID, get()) }
    }
    viewModel { PaywallViewModel(get(), get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.paywallGraph(navigator: AppNavigator) {
    composable<Route.Paywall> { PaywallScreen(navigator) }
}

@Composable
private fun PaywallScreen(
    navigator: AppNavigator,
    viewModel: PaywallViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                PaywallEvent.NavigateBack -> navigator.back()
            }
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        UiStateSurface(
            state = state,
            onRetry = { viewModel.onAction(PaywallAction.RetryPlans) },
            modifier = modifier.padding(insets),
        ) { content ->
            val activity = context.findActivity()
            PaywallContent(
                content = content,
                onSelectPlan = { viewModel.onAction(PaywallAction.SelectPlan(it)) },
                onPurchase = {
                    activity?.let { viewModel.onAction(PaywallAction.Purchase(it)) }
                        ?: viewModel.onAction(PaywallAction.ClearMessage)
                },
                onRestore = { viewModel.onAction(PaywallAction.Restore) },
                onWatchAd = {
                    activity?.let { viewModel.onAction(PaywallAction.WatchAd(it)) }
                        ?: viewModel.onAction(PaywallAction.ClearMessage)
                },
                onClose = navigator::back,
                onOpenUrl = context::openUrl,
            )
        }
    }
}

@Composable
private fun PaywallContent(
    content: PaywallContent,
    onSelectPlan: (PlanType) -> Unit = {},
    onPurchase: () -> Unit = {},
    onRestore: () -> Unit = {},
    onWatchAd: () -> Unit = {},
    onClose: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val title = when (content.persona) {
        Persona.INHERITED -> R.string.paywall_inherited_title
        Persona.DETECTORIST -> R.string.paywall_detectorist_title
        else -> R.string.paywall_collector_title
    }
    val benefit = when (content.persona) {
        Persona.INHERITED -> R.string.paywall_inherited_benefit
        Persona.DETECTORIST -> R.string.paywall_detectorist_benefit
        else -> R.string.paywall_collector_benefit
    }
    val chosen = content.plans.firstOrNull { it.type == content.selected } ?: content.plans.first()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.paywall_close))
        }
        HeroCard {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(benefit),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        SectionHeader(stringResource(R.string.paywall_plans))
        content.plans.forEach { plan ->
            val price = plan.localizedPrice ?: stringResource(plan.type.demoPrice())
            PlanCard(
                plan = plan,
                selected = plan.type == content.selected,
                price = price,
                onClick = { onSelectPlan(plan.type) },
            )
        }
        Text(stringResource(R.string.paywall_benefits), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(if (content.demoBilling) R.string.paywall_demo_notice else R.string.paywall_store_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = stringResource(
                if (content.isPro) R.string.paywall_pro_active
                else if (content.demoBilling) R.string.paywall_continue
                else R.string.paywall_continue_store,
            ),
            icon = Icons.Outlined.ArrowForward,
            onClick = onPurchase,
            modifier = Modifier.fillMaxWidth(),
            enabled = !content.isPro && !content.busy,
        )
        if (content.busy) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        SecondaryButton(
            text = stringResource(R.string.paywall_restore),
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            enabled = !content.busy,
        )
        if (!content.isPro) {
            TextButton(
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth(),
                enabled = !content.busy,
            ) {
                Text(stringResource(if (content.demoAds) R.string.paywall_reward_demo else R.string.paywall_reward))
            }
        }
        content.message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (BuildConfig.TERMS_URL.isNotBlank()) {
                TextButton(onClick = { onOpenUrl(BuildConfig.TERMS_URL) }) {
                    Text(stringResource(R.string.paywall_terms))
                }
            }
            if (BuildConfig.PRIVACY_URL.isNotBlank()) {
                TextButton(onClick = { onOpenUrl(BuildConfig.PRIVACY_URL) }) {
                    Text(stringResource(R.string.paywall_privacy))
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: BillingPlan,
    selected: Boolean,
    price: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableCard(selected = selected, onSelect = onClick, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
                Text(stringResource(plan.type.title()), style = MaterialTheme.typography.titleMedium)
                plan.trialDays?.let { days ->
                    Text(
                        if (days == 3) stringResource(R.string.paywall_trial) else stringResource(R.string.paywall_trial_days, days),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(price, style = MaterialTheme.typography.titleMedium)
                if (selected) Text(stringResource(R.string.paywall_selected), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun PlanType.title() = when (this) {
    PlanType.WEEKLY -> R.string.paywall_weekly
    PlanType.ANNUAL -> R.string.paywall_annual
    PlanType.LIFETIME -> R.string.paywall_lifetime
}

private fun PlanType.demoPrice() = when (this) {
    PlanType.WEEKLY -> R.string.paywall_weekly_price
    PlanType.ANNUAL -> R.string.paywall_annual_price
    PlanType.LIFETIME -> R.string.paywall_lifetime_price
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

private fun previewContent(persona: Persona, isPro: Boolean = false) = PaywallContent(
    plans = listOf(
        BillingPlan(PlanType.WEEKLY, localizedPrice = "$4.99 / week (test)"),
        BillingPlan(PlanType.ANNUAL, localizedPrice = "$29.99 / year (test)", trialDays = 3),
        BillingPlan(PlanType.LIFETIME, localizedPrice = "$59.99 once (test)"),
    ),
    selected = PlanType.ANNUAL,
    persona = persona,
    isPro = isPro,
)

@Preview(name = "Loading")
@Composable
private fun PaywallLoadingPreview() {
    CoinLensTheme {
        UiStateSurface<PaywallContent>(UiState.Loading) {}
    }
}

@Preview(name = "Empty")
@Composable
private fun PaywallEmptyPreview() {
    CoinLensTheme {
        UiStateSurface<PaywallContent>(UiState.Empty("No plans right now")) {}
    }
}

@Preview(name = "Error")
@Composable
private fun PaywallErrorPreview() {
    CoinLensTheme {
        UiStateSurface<PaywallContent>(UiState.Error(AppError.Network()), onRetry = {}) {}
    }
}

@Preview(name = "Collector")
@Composable
private fun PaywallCollectorPreview() {
    CoinLensTheme {
        PaywallContent(content = previewContent(Persona.COLLECTOR))
    }
}

@Preview(name = "Inherited")
@Composable
private fun PaywallInheritedPreview() {
    CoinLensTheme {
        PaywallContent(content = previewContent(Persona.INHERITED))
    }
}

@Preview(name = "Detectorist Pro")
@Composable
private fun PaywallDetectoristPreview() {
    CoinLensTheme {
        PaywallContent(content = previewContent(Persona.DETECTORIST, isPro = true))
    }
}
