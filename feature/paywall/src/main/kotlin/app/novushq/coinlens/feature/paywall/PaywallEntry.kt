package app.novushq.coinlens.feature.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.data.LocalEntitlementRepository
import app.novushq.coinlens.domain.GrantBonusScanUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module

val paywallModule = module { }

fun NavGraphBuilder.paywallGraph(navigator: AppNavigator) {
    composable<Route.Paywall> { PaywallScreen(navigator) }
}

@Composable
private fun PaywallScreen(
    navigator: AppNavigator,
    entitlements: LocalEntitlementRepository = koinInject(),
    grantBonus: GrantBonusScanUseCase = koinInject(),
    preferences: PreferencesRepository = koinInject(),
) {
    val persona by preferences.persona.collectAsState(initial = Persona.COLLECTOR)
    val isPro by entitlements.isPro.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(1) }
    var message by remember { mutableStateOf<Int?>(null) }
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
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            TextButton(onClick = navigator::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.paywall_close)) }
            Text(stringResource(title), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(benefit), style = MaterialTheme.typography.bodyLarge)
            SectionHeader(stringResource(R.string.paywall_plans))
            PlanCard(0, selected == 0, R.string.paywall_weekly, R.string.paywall_weekly_price) { selected = 0 }
            PlanCard(1, selected == 1, R.string.paywall_annual, R.string.paywall_annual_price, R.string.paywall_trial) { selected = 1 }
            PlanCard(2, selected == 2, R.string.paywall_lifetime, R.string.paywall_lifetime_price) { selected = 2 }
            Text(stringResource(R.string.paywall_benefits), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.paywall_demo_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(
                text = stringResource(if (isPro) R.string.paywall_pro_active else R.string.paywall_continue),
                onClick = { entitlements.setPro(true); navigator.back() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPro,
            )
            OutlinedButton(
                onClick = { scope.launch { message = if (entitlements.restore().getOrNull() == true) R.string.paywall_restored else R.string.paywall_nothing_to_restore } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.paywall_restore)) }
            if (!isPro) TextButton(
                onClick = { scope.launch { if (grantBonus().isSuccess) { message = R.string.paywall_bonus_granted; navigator.back() } } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.paywall_reward)) }
            message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            TextButton(onClick = navigator::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.paywall_later)) }
        }
    }
}

@Composable
private fun PlanCard(index: Int, selected: Boolean, title: Int, price: Int, badge: Int? = null, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                badge?.let { Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(stringResource(price), style = MaterialTheme.typography.titleMedium)
                if (selected) Text(stringResource(R.string.paywall_selected), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
