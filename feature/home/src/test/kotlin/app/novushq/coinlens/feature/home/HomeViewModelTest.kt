package app.novushq.coinlens.feature.home

import androidx.lifecycle.ViewModelStore
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.testing.FakeCollectionRepository
import app.novushq.coinlens.testing.FakeEntitlements
import app.novushq.coinlens.testing.FakePreferences
import app.novushq.coinlens.testing.FakeScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Test
    fun `loads empty cabinet and available first scan`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = HomeViewModel(
            collections = FakeCollectionRepository(),
            scans = FakeScanRepository(),
            observeAllowance = ObserveScanAllowanceUseCase(FakeEntitlements(), FakePreferences()),
        )
        val store = ViewModelStore().apply { put("home", viewModel) }

        try {
            runCurrent()
            val state = viewModel.state.value
            assertTrue(state is UiState.Success)
            val data = (state as UiState.Success).data
            assertEquals(0, data.summary.itemCount)
            assertTrue(data.allowance.canScan)
            assertTrue(data.recent.isEmpty())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
