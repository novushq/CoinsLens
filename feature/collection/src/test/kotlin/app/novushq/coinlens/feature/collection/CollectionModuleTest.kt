package app.novushq.coinlens.feature.collection

import androidx.lifecycle.SavedStateHandle
import app.novushq.coinlens.domain.CollectionRepository
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class CollectionModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module verifies`() {
        collectionModule.verify(extraTypes = listOf(SavedStateHandle::class, CollectionRepository::class))
    }
}
