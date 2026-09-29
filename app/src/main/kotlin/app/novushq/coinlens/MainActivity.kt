package app.novushq.coinlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.Motion
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

private data class TopLevelDestination(
    val route: Route,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(Route.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelDestination(Route.Collection, R.string.nav_cabinet, Icons.Filled.Style, Icons.Outlined.Style),
    TopLevelDestination(Route.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
private fun CoinLensNavHost(startDestination: Route, navigator: AppNavigator) {
    val navController = rememberNavController()
    LaunchedEffect(navController, navigator) {
        navigator.commands.collect { navController.execute(it) }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTopLevel = TopLevelDestinations.firstOrNull { destination?.hasRoute(it.route::class) == true }

    Scaffold(
        bottomBar = {
            if (currentTopLevel != null) {
                NavigationBar {
                    TopLevelDestinations.forEach { item ->
                        val selected = item == currentTopLevel
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { insets ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(insets),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(Motion.MEDIUM)) + fadeIn(tween(Motion.MEDIUM))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(Motion.MEDIUM)) + fadeOut(tween(Motion.MEDIUM))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(Motion.MEDIUM)) + fadeIn(tween(Motion.MEDIUM))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(Motion.MEDIUM)) + fadeOut(tween(Motion.SHORT))
            },
        ) {
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
}
