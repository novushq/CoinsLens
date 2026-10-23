package app.novushq.coinlens.feature.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.novushq.coinlens.ai.AiInfoProvider
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps settingsModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class SettingsModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        settingsModule.verify(
            extraTypes = listOf(
                Context::class,
                SavedStateHandle::class,
                EntitlementRepository::class,
                ObserveScanAllowanceUseCase::class,
                PreferencesRepository::class,
                AiInfoProvider::class,
                DispatcherProvider::class,
            ),
        )
    }
}
