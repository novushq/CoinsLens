package app.novushq.coinlens.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SelectableCard
import app.novushq.coinlens.designsystem.theme.Motion
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
                LinearProgressIndicator(
                    progress = { (page + 1) / 2f },
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        (slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(Motion.MEDIUM)) + fadeIn(tween(Motion.MEDIUM)))
                            .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(Motion.SHORT)) + fadeOut(tween(Motion.SHORT)))
                    },
                    label = "onboardingPage",
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                        Text(
                            text = stringResource(if (current == 0) R.string.onboarding_kicker else R.string.onboarding_camera_kicker),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(if (current == 0) R.string.onboarding_title else R.string.onboarding_camera_title),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(if (current == 0) R.string.onboarding_intro else R.string.onboarding_camera_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (current == 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(stringResource(R.string.onboarding_persona_prompt), style = MaterialTheme.typography.titleMedium)
                                Persona.entries.forEach { option ->
                                    PersonaCard(
                                        selected = persona == option,
                                        onSelect = { persona = option },
                                        icon = personaIcon(option),
                                        label = stringResource(personaLabel(option)),
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
                    }
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

@Composable
private fun PersonaCard(selected: Boolean, onSelect: () -> Unit, icon: ImageVector, label: String) {
    SelectableCard(selected = selected, onSelect = onSelect) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.xl),
            )
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            RadioButton(selected = selected, onClick = null)
        }
    }
}

private fun personaIcon(persona: Persona): ImageVector = when (persona) {
    Persona.COLLECTOR -> Icons.Outlined.Collections
    Persona.INHERITED -> Icons.Outlined.FamilyRestroom
    Persona.DETECTORIST -> Icons.Outlined.Radar
}

private fun personaLabel(persona: Persona): Int = when (persona) {
    Persona.COLLECTOR -> R.string.onboarding_collector
    Persona.INHERITED -> R.string.onboarding_inherited
    Persona.DETECTORIST -> R.string.onboarding_detectorist
}
