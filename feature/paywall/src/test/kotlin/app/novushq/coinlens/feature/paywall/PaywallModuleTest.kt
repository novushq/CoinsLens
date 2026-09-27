package app.novushq.coinlens.feature.paywall

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps paywallModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class PaywallModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        paywallModule.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
