package app.novushq.coinlens.feature.home

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class HomeModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module verifies`() {
        homeModule.verify()
    }
}
