package app.novushq.coinlens

import android.app.Application
import app.novushq.coinlens.ai.AiMode
import app.novushq.coinlens.ai.ModelConfig
import app.novushq.coinlens.di.appModules
import app.novushq.coinlens.platform.AppCheckInstaller
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CoinLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val aiMode = aiModeOf(BuildConfig.AI_MODE)
        val koin = startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@CoinLensApplication)
            modules(appModules(aiMode, BuildConfig.DEBUG))
        }.koin
        if (aiMode == AiMode.FIREBASE) {
            // App Check must be installed before the first AI call.
            AppCheckInstaller.install(koin.get())
            koin.get<ModelConfig>().refresh()
        }
    }
}

internal fun aiModeOf(value: String): AiMode = if (value == "firebase") AiMode.FIREBASE else AiMode.FAKE
