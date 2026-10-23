package app.novushq.coinlens.feature.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.testing.FakeScanRepository
import app.novushq.coinlens.testing.coin
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShareViewModelTest {
    private val now = Instant.parse("2026-09-27T10:00:00Z")

    private fun viewModel(scans: FakeScanRepository, scanId: String): ShareViewModel =
        ShareViewModel(SavedStateHandle(mapOf("scanId" to scanId)), scans)

    private fun record(id: String): ScanRecord = ScanRecord(
        id = id,
        createdAt = now,
        obversePath = "/files/obverse-1.jpg",
        identification = coin(),
    )

    @Test
    fun `known scanId loads the card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scans = FakeScanRepository()
        scans.records["s-1"] = record("s-1")
        val viewModel = viewModel(scans, "s-1")
        val store = ViewModelStore().apply { put("share", viewModel) }

        try {
            runCurrent()
            val state = viewModel.state.value
            assertTrue(state is UiState.Success)
            assertEquals("s-1", (state as UiState.Success).data.id)
            assertEquals("Test cent", state.data.identification.name)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `unknown scanId yields error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(FakeScanRepository(), "missing")
        val store = ViewModelStore().apply { put("share", viewModel) }

        try {
            runCurrent()
            assertTrue(viewModel.state.value is UiState.Error)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `error then retry succeeds`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scans = FakeScanRepository()
        val viewModel = viewModel(scans, "s-2")
        val store = ViewModelStore().apply { put("share", viewModel) }

        try {
            runCurrent()
            assertTrue(viewModel.state.value is UiState.Error)

            scans.records["s-2"] = record("s-2")
            viewModel.onAction(ShareAction.Retry)
            runCurrent()

            val state = viewModel.state.value
            assertTrue(state is UiState.Success)
            assertEquals("s-2", (state as UiState.Success).data.id)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
