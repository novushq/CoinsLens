package app.novushq.coinlens.feature.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val CAMERA_PERMISSION = "android.permission.CAMERA"

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}

fun NavGraphBuilder.onboardingGraph(navigator: AppNavigator) {
    composable<Route.Onboarding> {
        OnboardingDestination(navigator)
    }
}

@Composable
private fun OnboardingDestination(
    navigator: AppNavigator,
    viewModel: OnboardingViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigateToCapture = (state as? UiState.Success)?.data?.navigateToCapture == true
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onAction(OnboardingAction.PermissionResult(granted))
    }
    LaunchedEffect(navigateToCapture) {
        if (navigateToCapture) {
            viewModel.onAction(OnboardingAction.NavigationConsumed)
            navigator.replaceAll(Route.Capture)
        }
    }
    OnboardingContent(
        state = state,
        onAction = viewModel::onAction,
        onRequestCamera = { cameraPermission.launch(CAMERA_PERMISSION) },
        modifier = modifier,
    )
}
