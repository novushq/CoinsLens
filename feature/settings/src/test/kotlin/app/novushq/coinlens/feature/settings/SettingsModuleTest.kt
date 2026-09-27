package app.novushq.coinlens.feature.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps settingsModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class SettingsModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        settingsModule.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
