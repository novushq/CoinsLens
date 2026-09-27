package app.novushq.coinlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.feature.capture.captureGraph
import app.novushq.coinlens.feature.collection.collectionGraph
import app.novushq.coinlens.feature.home.homeGraph
import app.novushq.coinlens.feature.onboarding.onboardingGraph
import app.novushq.coinlens.feature.paywall.paywallGraph
import app.novushq.coinlens.feature.result.resultGraph
import app.novushq.coinlens.feature.settings.settingsGraph
import app.novushq.coinlens.feature.share.shareGraph
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import app.novushq.coinlens.navigation.execute
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()
    private val navigator: AppNavigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.startRoute.value == null }
        setContent {
            CoinLensTheme {
                // Screens draw edge-to-edge and consume their own insets (Scaffold / safeDrawingPadding).
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val start by viewModel.startRoute.collectAsStateWithLifecycle()
                    start?.let { CoinLensNavHost(startDestination = it, navigator = navigator) }
                }
            }
        }
    }
}

@Composable
private fun CoinLensNavHost(startDestination: Route, navigator: AppNavigator) {
    val navController = rememberNavController()
    LaunchedEffect(navController, navigator) {
        navigator.commands.collect { navController.execute(it) }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        onboardingGraph(navigator)
        homeGraph(navigator)
        captureGraph(navigator)
        resultGraph(navigator)
        collectionGraph(navigator)
        shareGraph(navigator)
        paywallGraph(navigator)
        settingsGraph(navigator)
    }
}
