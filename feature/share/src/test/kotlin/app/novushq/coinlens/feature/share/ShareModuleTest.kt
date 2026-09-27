package app.novushq.coinlens.feature.share

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps shareModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class ShareModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        shareModule.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
