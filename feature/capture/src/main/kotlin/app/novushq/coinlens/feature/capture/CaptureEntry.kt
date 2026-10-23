package app.novushq.coinlens.feature.capture

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val captureModule = module {
    viewModelOf(::CaptureViewModel)
}

fun NavGraphBuilder.captureGraph(navigator: AppNavigator) {
    composable<Route.Capture> {
        CaptureRoute(navigator)
    }
}
