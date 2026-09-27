package app.novushq.coinlens.feature.onboarding

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class OnboardingModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module verifies`() {
        onboardingModule.verify()
    }
}
