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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.EmptyState
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SelectableCard
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.Motion
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.ThemePreviews
import app.novushq.coinlens.model.Persona

@Composable
fun OnboardingContent(
    state: UiState<OnboardingData>,
    onAction: (OnboardingAction) -> Unit,
    onRequestCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UiStateSurface(
        state = state,
        modifier = modifier.fillMaxSize(),
        onRetry = { onAction(OnboardingAction.Retry) },
        empty = { message ->
            OnboardingEmpty(
                message = message,
                onStart = { onAction(OnboardingAction.Next) },
            )
        },
        content = { data ->
            OnboardingPages(
                data = data,
                onAction = onAction,
                onRequestCamera = onRequestCamera,
            )
        },
    )
}

@Composable
private fun OnboardingPages(
    data: OnboardingData,
    onAction: (OnboardingAction) -> Unit,
    onRequestCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            LinearProgressIndicator(
                progress = { (data.page + 1) / OnboardingViewModel.PAGE_COUNT.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedContent(
                targetState = data.page,
                transitionSpec = {
                    (slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(Motion.MEDIUM)) + fadeIn(tween(Motion.MEDIUM)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(Motion.SHORT)) + fadeOut(tween(Motion.SHORT)))
                },
                label = "onboardingPage",
            ) { current ->
                when (current) {
                    0 -> PromisePage()
                    1 -> QuizPage(persona = data.persona, onSelect = { onAction(OnboardingAction.SelectPersona(it)) })
                    else -> CameraPage()
                }
            }
            when (data.page) {
                0, 1 -> {
                    PrimaryButton(
                        text = stringResource(R.string.onboarding_continue),
                        onClick = { onAction(OnboardingAction.Next) },
                        modifier = Modifier.fillMaxWidth(),
                        loading = data.saving,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { onAction(OnboardingAction.Skip) },
                            enabled = !data.saving,
                        ) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                        Text(
                            text = stringResource(R.string.onboarding_step, data.page + 1, OnboardingViewModel.PAGE_COUNT),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    PrimaryButton(
                        text = stringResource(R.string.onboarding_camera_cta),
                        onClick = onRequestCamera,
                        modifier = Modifier.fillMaxWidth(),
                        loading = data.saving,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { onAction(OnboardingAction.SkipPermission) },
                            enabled = !data.saving,
                        ) {
                            Text(stringResource(R.string.onboarding_camera_skip))
                        }
                        Text(
                            text = stringResource(R.string.onboarding_step, data.page + 1, OnboardingViewModel.PAGE_COUNT),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingEmpty(message: String, onStart: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        message = message,
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.xl, vertical = Spacing.xxxl),
        action = {
            PrimaryButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun PromisePage(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = stringResource(R.string.onboarding_promise_kicker),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.onboarding_promise_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.onboarding_promise_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            WedgePoint(icon = Icons.Outlined.CheckCircle, text = stringResource(R.string.onboarding_wedge_free))
            WedgePoint(icon = Icons.Outlined.Block, text = stringResource(R.string.onboarding_wedge_no_ads))
            WedgePoint(icon = Icons.Outlined.Scale, text = stringResource(R.string.onboarding_wedge_ranges))
        }
    }
}

@Composable
private fun WedgePoint(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.xl),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun QuizPage(
    persona: Persona,
    onSelect: (Persona) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = stringResource(R.string.onboarding_kicker),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.onboarding_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.onboarding_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.onboarding_persona_prompt),
                style = MaterialTheme.typography.titleMedium,
            )
            Persona.entries.forEach { option ->
                PersonaCard(
                    selected = persona == option,
                    onSelect = { onSelect(option) },
                    icon = personaIcon(option),
                    label = stringResource(personaLabel(option)),
                )
            }
        }
    }
}

@Composable
private fun CameraPage(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Text(
            text = stringResource(R.string.onboarding_camera_kicker),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.onboarding_camera_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.onboarding_camera_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.onboarding_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@ThemePreviews
@Composable
private fun OnboardingLoadingPreview() {
    CoinLensTheme {
        OnboardingContent(state = UiState.Loading, onAction = {}, onRequestCamera = {})
    }
}

@ThemePreviews
@Composable
private fun OnboardingEmptyPreview() {
    CoinLensTheme {
        OnboardingContent(
            state = UiState.Empty("No onboarding content available"),
            onAction = {},
            onRequestCamera = {},
        )
    }
}

@ThemePreviews
@Composable
private fun OnboardingErrorPreview() {
    CoinLensTheme {
        OnboardingContent(
            state = UiState.Error(AppError.Storage("Could not save this choice")),
            onAction = {},
            onRequestCamera = {},
        )
    }
}

@ThemePreviews
@Composable
private fun OnboardingPromisePreview() {
    CoinLensTheme {
        OnboardingContent(
            state = UiState.Success(OnboardingData(page = 0, persona = Persona.COLLECTOR, saving = false, navigateToCapture = false)),
            onAction = {},
            onRequestCamera = {},
        )
    }
}

@ThemePreviews
@Composable
private fun OnboardingQuizPreview() {
    CoinLensTheme {
        OnboardingContent(
            state = UiState.Success(OnboardingData(page = 1, persona = Persona.INHERITED, saving = false, navigateToCapture = false)),
            onAction = {},
            onRequestCamera = {},
        )
    }
}

@ThemePreviews
@Composable
private fun OnboardingCameraPreview() {
    CoinLensTheme {
        OnboardingContent(
            state = UiState.Success(OnboardingData(page = 2, persona = Persona.DETECTORIST, saving = false, navigateToCapture = false)),
            onAction = {},
            onRequestCamera = {},
        )
    }
}
