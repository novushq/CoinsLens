package app.novushq.coinlens.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.domain.CompleteOnboardingUseCase
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module

val onboardingModule = module {}

fun NavGraphBuilder.onboardingGraph(navigator: AppNavigator) {
    composable<Route.Onboarding> {
        val completeOnboarding: CompleteOnboardingUseCase = koinInject()
        val scope = rememberCoroutineScope()
        var page by rememberSaveable { mutableStateOf(0) }
        var persona by rememberSaveable { mutableStateOf(Persona.COLLECTOR) }
        var saving by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }

        fun finish() {
            scope.launch {
                saving = true
                failed = !completeOnboarding(persona).isSuccess
                saving = false
                if (!failed) navigator.replaceAll(Route.Capture)
            }
        }

        Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Text(
                    text = stringResource(if (page == 0) R.string.onboarding_kicker else R.string.onboarding_camera_kicker),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(if (page == 0) R.string.onboarding_title else R.string.onboarding_camera_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(if (page == 0) R.string.onboarding_intro else R.string.onboarding_camera_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (page == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(stringResource(R.string.onboarding_persona_prompt), style = MaterialTheme.typography.titleMedium)
                        Persona.entries.forEach { option ->
                            FilterChip(
                                selected = persona == option,
                                onClick = { persona = option },
                                label = {
                                    Text(
                                        stringResource(
                                            when (option) {
                                                Persona.COLLECTOR -> R.string.onboarding_collector
                                                Persona.INHERITED -> R.string.onboarding_inherited
                                                Persona.DETECTORIST -> R.string.onboarding_detectorist
                                            },
                                        ),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_privacy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (failed) {
                    Text(
                        text = stringResource(R.string.onboarding_save_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                PrimaryButton(
                    text = stringResource(if (page == 0) R.string.onboarding_continue else R.string.onboarding_start),
                    onClick = { if (page == 0) page = 1 else finish() },
                    modifier = Modifier.fillMaxWidth(),
                    loading = saving,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { persona = Persona.COLLECTOR; finish() }) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                    Text(
                        text = stringResource(R.string.onboarding_step, page + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
