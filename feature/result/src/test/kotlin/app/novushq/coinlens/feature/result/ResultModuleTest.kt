package app.novushq.coinlens.feature.result

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.novushq.coinlens.data.ScanDraft
import app.novushq.coinlens.domain.AddToCollectionUseCase
import app.novushq.coinlens.domain.IdentifyCoinUseCase
import app.novushq.coinlens.domain.ScanRepository
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps resultModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class ResultModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        resultModule.verify(
            extraTypes = listOf(
                Context::class,
                SavedStateHandle::class,
                IdentifyCoinUseCase::class,
                ScanRepository::class,
                ScanDraft::class,
                AddToCollectionUseCase::class,
            ),
        )
    }
}
