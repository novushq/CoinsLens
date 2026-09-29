package app.novushq.coinlens.feature.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.ai.AiInfoProvider
import app.novushq.coinlens.designsystem.component.SecondaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.TertiaryButton
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.PaywallSource
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module

val settingsModule = module {}

fun NavGraphBuilder.settingsGraph(navigator: AppNavigator) {
    composable<Route.Settings> {
        SettingsContent(navigator)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(navigator: AppNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences: PreferencesRepository = koinInject()
    val entitlements: EntitlementRepository = koinInject()
    val allowanceUseCase: ObserveScanAllowanceUseCase = koinInject()
    val aiInfo: AiInfoProvider = koinInject()
    val persona by preferences.persona.collectAsStateWithLifecycle(initialValue = Persona.COLLECTOR)
    val allowance by remember(allowanceUseCase) { allowanceUseCase() }
        .collectAsStateWithLifecycle(initialValue = app.novushq.coinlens.model.ScanAllowance())
    val isPro by entitlements.isPro.collectAsStateWithLifecycle(initialValue = false)
    var editPersona by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader(title = stringResource(R.string.settings_plan))
                Text(
                    text = if (isPro) stringResource(R.string.settings_pro) else {
                        pluralStringResource(R.plurals.settings_free, allowance.freeRemaining + allowance.bonusRemaining, allowance.freeRemaining + allowance.bonusRemaining)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                SecondaryButton(
                    text = stringResource(if (isPro) R.string.settings_manage_plan else R.string.settings_upgrade),
                    onClick = { navigator.navigate(Route.Paywall(PaywallSource.SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            message = when (val result = entitlements.restore()) {
                                is app.novushq.coinlens.common.AppResult.Success -> if (result.data) {
                                    R.string.settings_restore_success
                                } else R.string.settings_restore_empty
                                is app.novushq.coinlens.common.AppResult.Failure -> R.string.settings_restore_error
                            }
                        }
                    },
                ) { Text(stringResource(R.string.settings_restore)) }
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/account/subscriptions".toUri())
                        runCatching { context.startActivity(intent) }.onFailure { message = R.string.settings_subscription_error }
                    },
                ) { Text(stringResource(R.string.settings_manage_subscription)) }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (BuildConfig.TERMS_URL.isNotBlank()) TextButton(onClick = { context.openUrl(BuildConfig.TERMS_URL) }) {
                        Text(stringResource(R.string.settings_terms))
                    }
                    if (BuildConfig.PRIVACY_URL.isNotBlank()) TextButton(onClick = { context.openUrl(BuildConfig.PRIVACY_URL) }) {
                        Text(stringResource(R.string.settings_privacy))
                    }
                }
                message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader(title = stringResource(R.string.settings_preferences))
                Text(
                text = stringResource(R.string.settings_persona, stringResource(personaLabel(persona ?: Persona.COLLECTOR))),
                    style = MaterialTheme.typography.bodyLarge,
                )
                TertiaryButton(
                    text = stringResource(R.string.settings_change_persona),
                    onClick = { editPersona = true },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader(title = stringResource(R.string.settings_about))
                Text(
                    text = stringResource(
                        if (aiInfo.current.mode == app.novushq.coinlens.ai.AiMode.FAKE) R.string.settings_ai_demo
                        else R.string.settings_ai_live,
                        aiInfo.current.modelName,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.settings_disclaimer), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (editPersona) {
        AlertDialog(
            onDismissRequest = { editPersona = false },
            title = { Text(stringResource(R.string.settings_change_persona)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Persona.entries.forEach { option ->
                        FilterChip(
                            selected = persona == option,
                            onClick = {
                                scope.launch { preferences.setPersona(option) }
                                editPersona = false
                            },
                            label = { Text(stringResource(personaLabel(option))) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editPersona = false }) { Text(stringResource(R.string.settings_done)) } },
        )
    }
}

private fun personaLabel(persona: Persona): Int = when (persona) {
    Persona.COLLECTOR -> R.string.settings_collector
    Persona.INHERITED -> R.string.settings_inherited
    Persona.DETECTORIST -> R.string.settings_detectorist
}

private fun android.content.Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
