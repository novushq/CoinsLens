package app.novushq.coinlens.feature.home

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.domain.ScanRepository
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Keeps homeModule resolvable in isolation-safe form; core types come from :app's graph (see KoinGraphTest). */
class HomeModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `module definitions verify`() {
        homeModule.verify(
            extraTypes = listOf(
                Context::class,
                SavedStateHandle::class,
                CollectionRepository::class,
                ScanRepository::class,
                ObserveScanAllowanceUseCase::class,
            ),
        )
    }
}
