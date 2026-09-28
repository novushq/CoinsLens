package app.novushq.coinlens.feature.onboarding

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps onboardingModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class OnboardingModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        onboardingModule.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
