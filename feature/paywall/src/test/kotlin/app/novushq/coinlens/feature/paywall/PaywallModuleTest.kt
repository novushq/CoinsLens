package app.novushq.coinlens.feature.paywall

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class PaywallModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module verifies`() {
        paywallModule.verify()
    }
}
