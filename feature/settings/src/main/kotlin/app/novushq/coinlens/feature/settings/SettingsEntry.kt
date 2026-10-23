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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.ai.AiMode
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.SecondaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.TertiaryButton
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.PaywallSource
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.settingsGraph(navigator: AppNavigator) {
    composable<Route.Settings> {
        SettingsScreen(navigator)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    navigator: AppNavigator,
    viewModel: SettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appVersion = remember(context) { context.appVersion() }
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SettingsEvent.NavigatePaywall -> navigator.navigate(Route.Paywall(PaywallSource.SETTINGS))
                SettingsEvent.NavigateOnboarding -> navigator.replaceAll(Route.Onboarding)
                is SettingsEvent.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, event.url.toUri())
                    runCatching { context.startActivity(intent) }
                        .onFailure { viewModel.onAction(SettingsAction.LinkFailed) }
                }
            }
        }
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { insets ->
        UiStateSurface(
            state = state,
            onRetry = { viewModel.onAction(SettingsAction.Retry) },
            modifier = Modifier.padding(insets),
        ) { content ->
            SettingsContent(
                content = content,
                appVersion = appVersion,
                onGoPro = { viewModel.onAction(SettingsAction.GoPro) },
                onRestore = { viewModel.onAction(SettingsAction.Restore) },
                onManageSubscription = { viewModel.onAction(SettingsAction.ManageSubscription) },
                onOpenTerms = { viewModel.onAction(SettingsAction.OpenTerms) },
                onOpenPrivacy = { viewModel.onAction(SettingsAction.OpenPrivacy) },
                onSelectPersona = { viewModel.onAction(SettingsAction.SelectPersona(it)) },
                onResetOnboarding = { viewModel.onAction(SettingsAction.ResetOnboarding) },
            )
        }
    }
}

@Composable
private fun SettingsContent(
    content: SettingsContent,
    appVersion: String,
    onGoPro: () -> Unit = {},
    onRestore: () -> Unit = {},
    onManageSubscription: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onSelectPersona: (Persona) -> Unit = {},
    onResetOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var editPersona by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SectionHeader(title = stringResource(R.string.settings_plan))
            Text(
                text = if (content.isPro) {
                    stringResource(R.string.settings_pro)
                } else {
                    pluralStringResource(R.plurals.settings_free, content.scansLeft, content.scansLeft)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            SecondaryButton(
                text = stringResource(if (content.isPro) R.string.settings_manage_plan else R.string.settings_upgrade),
                onClick = onGoPro,
                modifier = Modifier.fillMaxWidth(),
            )
            TertiaryButton(
                text = stringResource(R.string.settings_restore),
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth(),
                loading = content.restoring,
            )
            TextButton(onClick = onManageSubscription, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_manage_subscription))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (content.termsUrl.isNotBlank()) {
                    TextButton(onClick = onOpenTerms) { Text(stringResource(R.string.settings_terms)) }
                }
                if (content.privacyUrl.isNotBlank()) {
                    TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.settings_privacy)) }
                }
            }
            content.message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SectionHeader(title = stringResource(R.string.settings_preferences))
            Text(
                text = stringResource(R.string.settings_persona, stringResource(personaLabel(content.persona))),
                style = MaterialTheme.typography.bodyLarge,
            )
            TertiaryButton(
                text = stringResource(R.string.settings_change_persona),
                onClick = { editPersona = true },
            )
            if (BuildConfig.DEBUG) {
                TextButton(onClick = onResetOnboarding) {
                    Text(stringResource(R.string.settings_reset_onboarding))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SectionHeader(title = stringResource(R.string.settings_about))
            Text(
                text = stringResource(
                    if (content.aiMode == AiMode.FAKE) R.string.settings_ai_demo else R.string.settings_ai_live,
                    content.modelName,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.settings_disclaimer), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.settings_version, appVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                            selected = content.persona == option,
                            onClick = {
                                onSelectPersona(option)
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

private fun personaLabel(persona: Persona?): Int = when (persona) {
    Persona.INHERITED -> R.string.settings_inherited
    Persona.DETECTORIST -> R.string.settings_detectorist
    else -> R.string.settings_collector
}

private fun android.content.Context.appVersion(): String = runCatching {
    val info = packageManager.getPackageInfo(packageName, 0)
    @Suppress("DEPRECATION")
    val code = info.versionCode.toString()
    "${info.versionName} ($code)"
}.getOrDefault("")

private fun previewContent(isPro: Boolean = false, scansLeft: Int = 1) = SettingsContent(
    isPro = isPro,
    scansLeft = scansLeft,
    persona = Persona.COLLECTOR,
    aiMode = AiMode.FAKE,
    modelName = "gemini-3.5-flash-lite",
    termsUrl = "https://example.com/terms",
    privacyUrl = "https://example.com/privacy",
)

@Preview(name = "Loading")
@Composable
private fun SettingsLoadingPreview() {
    CoinLensTheme {
        UiStateSurface<SettingsContent>(UiState.Loading) {}
    }
}

@Preview(name = "Empty")
@Composable
private fun SettingsEmptyPreview() {
    CoinLensTheme {
        UiStateSurface<SettingsContent>(UiState.Empty("Nothing to show")) {}
    }
}

@Preview(name = "Error")
@Composable
private fun SettingsErrorPreview() {
    CoinLensTheme {
        UiStateSurface<SettingsContent>(UiState.Error(AppError.Network()), onRetry = {}) {}
    }
}

@Preview(name = "Free")
@Composable
private fun SettingsFreePreview() {
    CoinLensTheme {
        SettingsContent(content = previewContent(isPro = false, scansLeft = 1), appVersion = "1.0 (1)")
    }
}

@Preview(name = "Pro")
@Composable
private fun SettingsProPreview() {
    CoinLensTheme {
        SettingsContent(content = previewContent(isPro = true), appVersion = "1.0 (1)")
    }
}
