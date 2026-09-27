package app.novushq.coinlens.feature.share

import androidx.lifecycle.SavedStateHandle
import app.novushq.coinlens.domain.ScanRepository
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class ShareModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module verifies`() {
        shareModule.verify(extraTypes = listOf(SavedStateHandle::class, ScanRepository::class))
    }
}
