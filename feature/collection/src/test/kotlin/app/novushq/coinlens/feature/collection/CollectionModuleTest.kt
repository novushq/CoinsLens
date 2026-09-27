package app.novushq.coinlens.feature.collection

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps collectionModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class CollectionModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        collectionModule.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
