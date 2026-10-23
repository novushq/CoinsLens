package app.novushq.coinlens.feature.home

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.domain.ScanRepository
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.FolderDetail
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.ValuedItem
import app.novushq.coinlens.model.usd
import app.novushq.coinlens.testing.FakeEntitlements
import app.novushq.coinlens.testing.FakePreferences
import app.novushq.coinlens.testing.FakeScanRepository
import app.novushq.coinlens.testing.coin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.lifecycle.ViewModelStore
import java.time.Instant

private class FlowCollectionRepository(
    initial: AppResult<CollectionSummary> = AppResult.Success(CollectionSummary()),
) : CollectionRepository {
    val summary = MutableStateFlow(initial)
    override fun observeFolders(): Flow<AppResult<List<Folder>>> =
        kotlinx.coroutines.flow.flowOf(AppResult.Success(emptyList()))
    override fun observeFolder(folderId: String): Flow<AppResult<FolderDetail>> =
        kotlinx.coroutines.flow.flowOf(AppResult.Failure(AppError.NotFound()))
    override fun observeItems(): Flow<AppResult<List<ValuedItem>>> =
        kotlinx.coroutines.flow.flowOf(AppResult.Success(emptyList()))
    override fun observeItem(itemId: String): Flow<AppResult<ValuedItem>> =
        kotlinx.coroutines.flow.flowOf(AppResult.Failure(AppError.NotFound()))
    override fun observeSummary(): Flow<AppResult<CollectionSummary>> = summary
    override suspend fun createFolder(name: String): AppResult<String> = AppResult.Success("f1")
    override suspend fun renameFolder(folderId: String, name: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteFolder(folderId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun addItem(item: CollectionItem): AppResult<String> = AppResult.Success(item.id)
    override suspend fun updateItem(item: CollectionItem): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteItem(itemId: String): AppResult<Unit> = AppResult.Success(Unit)
}

private fun scan(id: String) = ScanRecord(
    id = id,
    createdAt = Instant.now(),
    obversePath = "/files/$id.jpg",
    identification = coin(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private fun allowance(pro: Boolean = false, used: Int = 0, bonus: Int = 0) =
        ObserveScanAllowanceUseCase(FakeEntitlements(pro), FakePreferences(used, bonus))

    @Test
    fun `new user sees empty state with first-scan copy`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = HomeViewModel(
            collections = FlowCollectionRepository(),
            scans = FakeScanRepository(),
            observeAllowance = allowance(),
        )
        val store = ViewModelStore()
        store.put("home", viewModel)
        try {
            runCurrent()
            val state = viewModel.state.value
            assertTrue(state is UiState.Empty)
            assertEquals(HomeViewModel.EMPTY_STATE_MESSAGE, (state as UiState.Empty).message)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `success carries totals allowance and recent scans`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scans = FakeScanRepository()
        scans.records["s1"] = scan("s1")
        scans.records["s2"] = scan("s2")
        val summary = CollectionSummary(
            itemCount = 2,
            totalLow = usd(2.0),
            totalHigh = usd(6.0),
            totalMid = usd(4.0),
        )
        val viewModel = HomeViewModel(
            collections = FlowCollectionRepository(AppResult.Success(summary)),
            scans = scans,
            observeAllowance = allowance(),
        )
        val store = ViewModelStore()
        store.put("home", viewModel)
        try {
            runCurrent()
            val state = viewModel.state.value
            assertTrue(state is UiState.Success)
            val data = (state as UiState.Success).data
            assertEquals(usd(4.0), data.summary.totalMid)
            assertEquals(usd(2.0), data.summary.totalLow)
            assertEquals(usd(6.0), data.summary.totalHigh)
            assertEquals(2, data.summary.itemCount)
            assertEquals(2, data.recent.size)
            assertTrue(data.allowance.canScan)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `pro user with scans can always scan`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scans = FakeScanRepository()
        scans.records["s1"] = scan("s1")
        val viewModel = HomeViewModel(
            collections = FlowCollectionRepository(),
            scans = scans,
            observeAllowance = allowance(pro = true, used = 1),
        )
        val store = ViewModelStore()
        store.put("home", viewModel)
        try {
            runCurrent()
            val state = viewModel.state.value
            assertTrue(state is UiState.Success)
            val data = (state as UiState.Success).data
            assertTrue(data.allowance.isPro)
            assertTrue(data.allowance.canScan)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `summary failure surfaces error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = HomeViewModel(
            collections = FlowCollectionRepository(AppResult.Failure(AppError.Storage())),
            scans = FakeScanRepository(),
            observeAllowance = allowance(),
        )
        val store = ViewModelStore()
        store.put("home", viewModel)
        try {
            runCurrent()
            assertTrue(viewModel.state.value is UiState.Error)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `retry recovers after failure`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val collections = FlowCollectionRepository(AppResult.Failure(AppError.Network()))
        val viewModel = HomeViewModel(
            collections = collections,
            scans = FakeScanRepository(),
            observeAllowance = allowance(),
        )
        val store = ViewModelStore()
        store.put("home", viewModel)
        try {
            runCurrent()
            assertTrue(viewModel.state.value is UiState.Error)
            collections.summary.value = AppResult.Success(CollectionSummary())
            viewModel.onAction(HomeAction.Retry)
            runCurrent()
            assertTrue(viewModel.state.value is UiState.Empty)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
