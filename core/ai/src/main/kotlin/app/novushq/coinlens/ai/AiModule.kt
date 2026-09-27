package app.novushq.coinlens.ai

import app.novushq.coinlens.identify.IdentifyEngine
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Engine selection happens once, from `BuildConfig.AI_MODE` in `:app`.
 * Firebase mode binds only the Gemini engine, so a failed call surfaces its
 * error instead of silently returning sample data.
 */
fun aiModule(mode: AiMode, isDebug: Boolean): Module = module {
    when (mode) {
        AiMode.FIREBASE -> {
            single<ModelConfig> { RemoteModelConfig(get(), isDebug) }
            single<IdentifyEngine> { FirebaseGeminiEngine(get(), get(), get()) }
        }
        AiMode.FAKE -> {
            single<ModelConfig> { StaticModelConfig() }
            single<IdentifyEngine> { FakeIdentifyEngine(SampleCatalogue.json, get()) }
        }
    }
    single { AiInfoProvider(mode, get()) }
}
