package app.novushq.coinlens.di

import app.novushq.coinlens.MainViewModel
import app.novushq.coinlens.ai.AiMode
import app.novushq.coinlens.ai.aiModule
import app.novushq.coinlens.common.DefaultDispatcherProvider
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.Logger
import app.novushq.coinlens.data.dataModule
import app.novushq.coinlens.domain.domainModule
import app.novushq.coinlens.feature.capture.captureModule
import app.novushq.coinlens.feature.collection.collectionModule
import app.novushq.coinlens.feature.home.homeModule
import app.novushq.coinlens.feature.onboarding.onboardingModule
import app.novushq.coinlens.feature.paywall.paywallModule
import app.novushq.coinlens.feature.result.resultModule
import app.novushq.coinlens.feature.settings.settingsModule
import app.novushq.coinlens.feature.share.shareModule
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.DefaultAppNavigator
import app.novushq.coinlens.platform.AndroidLogger
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Platform singletons every other module relies on. */
val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Logger> { AndroidLogger() }
    single<AppNavigator> { DefaultAppNavigator() }
    viewModelOf(::MainViewModel)
}

/** Feature modules, loaded last so a feature binding (paywall's EntitlementRepository) overrides the core default. */
val featureModules: List<Module> = listOf(
    onboardingModule,
    homeModule,
    captureModule,
    resultModule,
    collectionModule,
    shareModule,
    paywallModule,
    settingsModule,
)

fun appModules(aiMode: AiMode, isDebug: Boolean): List<Module> =
    listOf(coreModule, aiModule(aiMode, isDebug), dataModule, domainModule) + featureModules
