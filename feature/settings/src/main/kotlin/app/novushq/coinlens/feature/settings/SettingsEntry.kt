package app.novushq.coinlens.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.ai.AiInfoProvider
import app.novushq.coinlens.designsystem.component.SectionHeader
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
    var message by remember { mutableStateOf("") }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displaySmall)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader(title = stringResource(R.string.settings_plan))
                Text(
                    text = if (isPro) stringResource(R.string.settings_pro) else {
                        stringResource(R.string.settings_free, allowance.freeRemaining + allowance.bonusRemaining)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(
                    onClick = { navigator.navigate(Route.Paywall(PaywallSource.SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (isPro) R.string.settings_manage_plan else R.string.settings_upgrade))
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            message = if (entitlements.restore().getOrNull() == true) {
                                context.getString(R.string.settings_restore_success)
                            } else {
                                context.getString(R.string.settings_restore_empty)
                            }
                        }
                    },
                ) { Text(stringResource(R.string.settings_restore)) }
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                        context.startActivity(intent)
                    },
                ) { Text(stringResource(R.string.settings_manage_subscription)) }
                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader(title = stringResource(R.string.settings_preferences))
                Text(
                    text = stringResource(R.string.settings_persona, stringResource(personaLabel(persona))),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = { editPersona = true }) {
                    Text(stringResource(R.string.settings_change_persona))
                }
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
