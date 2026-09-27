package app.novushq.coinlens.feature.capture

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps captureModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class CaptureModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        captureModule.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
