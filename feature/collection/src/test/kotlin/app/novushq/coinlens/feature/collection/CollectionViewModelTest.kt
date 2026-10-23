package app.novushq.coinlens.feature.collection

import androidx.lifecycle.SavedStateHandle
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.CoinIdentification
import app.novushq.coinlens.model.Confidence
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.FolderDetail
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.ItemKind
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.ValueEstimate
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.ValuedItem
import app.novushq.coinlens.model.usd
import app.novushq.coinlens.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

private fun testIdentification() = CoinIdentification(
    kind = ItemKind.COIN,
    name = "Wheat cent",
    country = "USA",
    denomination = "1 cent",
    year = 1943,
    value = ValueEstimate(
        circulated = ValueRange(usd(1.0), usd(3.0)),
        uncirculated = ValueRange(usd(10.0), usd(20.0)),
        confidence = Confidence.MEDIUM,
    ),
)

private fun testItem(id: String, folderId: String? = null) = ValuedItem(
    item = CollectionItem(id = id, scanId = "s-$id", folderId = folderId, addedAt = Instant.now()),
    scan = ScanRecord(
        id = "s-$id",
        createdAt = Instant.now(),
        obversePath = "",
        identification = testIdentification(),
    ),
)

private class ControllableCollectionRepository : CollectionRepository {
    val foldersFlow = MutableStateFlow<AppResult<List<Folder>>>(AppResult.Success(emptyList()))
    val folderFlow = MutableStateFlow<AppResult<FolderDetail>>(AppResult.Failure(AppError.NotFound()))
    val itemsFlow = MutableStateFlow<AppResult<List<ValuedItem>>>(AppResult.Success(emptyList()))
    val itemFlow = MutableStateFlow<AppResult<ValuedItem>>(AppResult.Failure(AppError.NotFound()))
    val summaryFlow = MutableStateFlow<AppResult<CollectionSummary>>(AppResult.Success(CollectionSummary()))

    val createdNames = mutableListOf<String>()
    val renamed = mutableListOf<Pair<String, String>>()
    val deletedFolders = mutableListOf<String>()
    val updatedItems = mutableListOf<CollectionItem>()
    val deletedItems = mutableListOf<String>()
    var failure: AppError? = null

    override fun observeFolders(): Flow<AppResult<List<Folder>>> = foldersFlow
    override fun observeFolder(folderId: String): Flow<AppResult<FolderDetail>> = folderFlow
    override fun observeItems(): Flow<AppResult<List<ValuedItem>>> = itemsFlow
    override fun observeItem(itemId: String): Flow<AppResult<ValuedItem>> = itemFlow
    override fun observeSummary(): Flow<AppResult<CollectionSummary>> = summaryFlow
    override suspend fun createFolder(name: String): AppResult<String> {
        failure?.let { return AppResult.Failure(it) }
        createdNames += name
        return AppResult.Success("f-${createdNames.size}")
    }
    override suspend fun renameFolder(folderId: String, name: String): AppResult<Unit> {
        failure?.let { return AppResult.Failure(it) }
        renamed += folderId to name
        return AppResult.Success(Unit)
    }
    override suspend fun deleteFolder(folderId: String): AppResult<Unit> {
        failure?.let { return AppResult.Failure(it) }
        deletedFolders += folderId
        return AppResult.Success(Unit)
    }
    override suspend fun addItem(item: CollectionItem): AppResult<String> = AppResult.Success(item.id)
    override suspend fun updateItem(item: CollectionItem): AppResult<Unit> {
        failure?.let { return AppResult.Failure(it) }
        updatedItems += item
        return AppResult.Success(Unit)
    }
    override suspend fun deleteItem(itemId: String): AppResult<Unit> {
        failure?.let { return AppResult.Failure(it) }
        deletedItems += itemId
        return AppResult.Success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CollectionViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun viewModel(
        fake: ControllableCollectionRepository,
        args: Map<String, String> = emptyMap(),
    ) = CollectionViewModel(SavedStateHandle(args), fake)

    @Test
    fun `collection route emits folders summary and items`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        val folders = listOf(Folder(id = "f1", name = "Wheat cents", createdAt = Instant.now(), itemCount = 12, totalValue = usd(24.0)))
        fake.foldersFlow.value = AppResult.Success(folders)
        fake.summaryFlow.value = AppResult.Success(CollectionSummary(itemCount = 13, totalMid = usd(20.0), totalCost = usd(15.0)))
        fake.itemsFlow.value = AppResult.Success(listOf(testItem("i1")))

        val vm = viewModel(fake)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(folders, data.folders)
        assertEquals(13, data.summary.itemCount)
        assertEquals(usd(20.0), data.summary.totalMid)
        assertEquals(1, data.items.size)
    }

    @Test
    fun `create rename and delete folder delegate to repository`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        val vm = viewModel(fake)
        advanceUntilIdle()

        vm.onAction(CollectionAction.CreateFolder("Wheat cents"))
        vm.onAction(CollectionAction.RenameFolder("f1", "Lincolns"))
        vm.onAction(CollectionAction.DeleteFolder("f1"))
        advanceUntilIdle()

        assertEquals(listOf("Wheat cents"), fake.createdNames)
        assertEquals(listOf("f1" to "Lincolns"), fake.renamed)
        assertEquals(listOf("f1"), fake.deletedFolders)
    }

    @Test
    fun `update item persists grade notes price quantity and folder`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        val vm = viewModel(fake)
        advanceUntilIdle()

        val edited = CollectionItem(
            id = "i1",
            scanId = "s-i1",
            folderId = "f1",
            grade = Grade.VERY_FINE,
            gradeNotes = "Red-brown lustre",
            purchasePrice = usd(2.5),
            quantity = 3,
            addedAt = Instant.now(),
        )
        vm.onAction(CollectionAction.UpdateItem(edited))
        advanceUntilIdle()

        assertEquals(listOf(edited), fake.updatedItems)
    }

    @Test
    fun `delete item delegates to repository`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        val vm = viewModel(fake)
        advanceUntilIdle()

        vm.onAction(CollectionAction.DeleteItem("i1"))
        advanceUntilIdle()

        assertEquals(listOf("i1"), fake.deletedItems)
    }

    @Test
    fun `repository failure surfaces error state`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        fake.foldersFlow.value = AppResult.Failure(AppError.Storage())
        val vm = viewModel(fake)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Error)
    }

    @Test
    fun `mutation failure surfaces error state`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        fake.failure = AppError.Storage()
        val vm = viewModel(fake)
        advanceUntilIdle()

        vm.onAction(CollectionAction.DeleteFolder("f1"))
        advanceUntilIdle()

        assertTrue(vm.state.value is UiState.Error)
    }

    @Test
    fun `folder route emits folder detail`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        val folder = Folder(id = "f1", name = "Wheat cents", createdAt = Instant.now(), itemCount = 1, totalValue = usd(2.0))
        fake.folderFlow.value = AppResult.Success(FolderDetail(folder, listOf(testItem("i1", folderId = "f1"))))

        val vm = viewModel(fake, mapOf("folderId" to "f1"))
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Success)
        assertEquals("Wheat cents", (state as UiState.Success).data.folder?.folder?.name)
    }

    @Test
    fun `item route emits valued item`() = runTest(main.dispatcher) {
        val fake = ControllableCollectionRepository()
        fake.itemFlow.value = AppResult.Success(testItem("i1"))

        val vm = viewModel(fake, mapOf("itemId" to "i1"))
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is UiState.Success)
        assertEquals("i1", (state as UiState.Success).data.item?.item?.id)
    }
}
