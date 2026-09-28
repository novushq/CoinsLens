package app.novushq.coinlens.feature.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import android.content.Intent
import androidx.core.net.toUri
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.GrantBonusScanUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module

val paywallModule = module {
    if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
        single<BillingGateway> { FakeBillingGateway(get()) }
        single<EntitlementRepository> { get<app.novushq.coinlens.data.LocalEntitlementRepository>() }
    } else {
        single { RevenueCatSdk(get(), BuildConfig.REVENUECAT_API_KEY) }
        single<RevenueCatEntitlementRepository> { RevenueCatEntitlementRepository(get()) }
        single<EntitlementRepository> { get<RevenueCatEntitlementRepository>() }
        single<BillingGateway> { RevenueCatBillingGateway(get()) }
    }
    if (BuildConfig.ADMOB_REWARDED_UNIT_ID.isBlank()) {
        single<RewardedAdGateway> { FakeRewardedAdGateway() }
    } else {
        single<RewardedAdGateway> { AdMobRewardedAdGateway(get(), BuildConfig.ADMOB_REWARDED_UNIT_ID, get()) }
    }
}

fun NavGraphBuilder.paywallGraph(navigator: AppNavigator) {
    composable<Route.Paywall> { PaywallScreen(navigator) }
}

@Composable
private fun PaywallScreen(
    navigator: AppNavigator,
    billing: BillingGateway = koinInject(),
    ads: RewardedAdGateway = koinInject(),
    entitlements: EntitlementRepository = koinInject(),
    grantBonus: GrantBonusScanUseCase = koinInject(),
    preferences: PreferencesRepository = koinInject(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val persona by preferences.persona.collectAsState(initial = Persona.COLLECTOR)
    val isPro by entitlements.isPro.collectAsState(initial = false)
    var plansState by remember { mutableStateOf<UiState<List<BillingPlan>>>(UiState.Loading) }
    var selected by remember { mutableStateOf(PlanType.ANNUAL) }
    var message by remember { mutableStateOf<Int?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var actionInProgress by remember { mutableStateOf(false) }
    LaunchedEffect(billing, retry) {
        plansState = when (val result = billing.loadPlans()) {
            is AppResult.Success -> if (result.data.isEmpty()) UiState.Empty() else UiState.Success(result.data)
            is AppResult.Failure -> UiState.Error(result.error)
        }
    }

    val title = when (persona) {
        Persona.INHERITED -> R.string.paywall_inherited_title
        Persona.DETECTORIST -> R.string.paywall_detectorist_title
        else -> R.string.paywall_collector_title
    }
    val benefit = when (persona) {
        Persona.INHERITED -> R.string.paywall_inherited_benefit
        Persona.DETECTORIST -> R.string.paywall_detectorist_benefit
        else -> R.string.paywall_collector_benefit
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        UiStateSurface(state = plansState, onRetry = { retry++ }, modifier = Modifier.padding(insets)) { plans ->
            val chosen = plans.firstOrNull { it.type == selected } ?: plans.first()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                TextButton(onClick = navigator::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.paywall_close)) }
                Text(stringResource(title), style = MaterialTheme.typography.displaySmall)
                Text(stringResource(benefit), style = MaterialTheme.typography.bodyLarge)
                SectionHeader(stringResource(R.string.paywall_plans))
                plans.forEach { plan ->
                    val price = plan.localizedPrice ?: stringResource(plan.type.demoPrice())
                    PlanCard(plan, selected == plan.type, price) { selected = plan.type }
                }
                Text(stringResource(R.string.paywall_benefits), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(if (billing.isDemo) R.string.paywall_demo_notice else R.string.paywall_store_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = stringResource(if (isPro) R.string.paywall_pro_active else if (billing.isDemo) R.string.paywall_continue else R.string.paywall_continue_store),
                    onClick = {
                        val activity = context.findActivity()
                        if (activity == null) {
                            message = R.string.paywall_purchase_error
                        } else scope.launch {
                            actionInProgress = true
                            try {
                                when (val result = billing.purchase(activity, chosen.type)) {
                                    is AppResult.Success -> if (result.data) navigator.back() // cancelled checkout stays quiet
                                    is AppResult.Failure -> message = R.string.paywall_purchase_error
                                }
                            } finally {
                                actionInProgress = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPro && !actionInProgress,
                )
                if (actionInProgress) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            actionInProgress = true
                            try {
                                message = when (val result = entitlements.restore()) {
                                    is AppResult.Success -> if (result.data) {
                                        if (billing.isDemo) R.string.paywall_restored else R.string.paywall_store_restored
                                    } else R.string.paywall_nothing_to_restore
                                    is AppResult.Failure -> R.string.paywall_restore_error
                                }
                            } finally {
                                actionInProgress = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionInProgress,
                ) { Text(stringResource(R.string.paywall_restore)) }
                if (!isPro) TextButton(
                    onClick = {
                        val activity = context.findActivity() ?: run { message = R.string.paywall_reward_error; return@TextButton }
                        scope.launch {
                            actionInProgress = true
                            try {
                                when (val result = ads.show(activity)) {
                                    is AppResult.Success -> if (result.data) {
                                        if (grantBonus().isSuccess) {
                                            message = R.string.paywall_bonus_granted
                                            navigator.back()
                                        } else message = R.string.paywall_reward_error
                                    }
                                    is AppResult.Failure -> message = R.string.paywall_reward_error
                                }
                            } finally {
                                actionInProgress = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionInProgress,
                ) { Text(stringResource(if (ads.isDemo) R.string.paywall_reward_demo else R.string.paywall_reward)) }
                message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    if (BuildConfig.TERMS_URL.isNotBlank()) TextButton(onClick = { context.openUrl(BuildConfig.TERMS_URL) }) {
                        Text(stringResource(R.string.paywall_terms))
                    }
                    if (BuildConfig.PRIVACY_URL.isNotBlank()) TextButton(onClick = { context.openUrl(BuildConfig.PRIVACY_URL) }) {
                        Text(stringResource(R.string.paywall_privacy))
                    }
                }
                TextButton(onClick = navigator::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.paywall_later)) }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: BillingPlan, selected: Boolean, price: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
