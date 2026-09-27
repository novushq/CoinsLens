package app.novushq.coinlens.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.data.db.CoinLensDatabase
import app.novushq.coinlens.domain.ComputeCollectionValueUseCase
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.VarietyHint
import app.novushq.coinlens.model.usd
import app.novushq.coinlens.testing.FakeImageStore
import app.novushq.coinlens.testing.TestDispatcherProvider
import app.novushq.coinlens.testing.coin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomRepositoriesTest {

    private lateinit var db: CoinLensDatabase
    private lateinit var images: FakeImageStore
    private lateinit var scans: RoomScanRepository
    private lateinit var collection: RoomCollectionRepository
    private var ids = 0
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CoinLensDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        images = FakeImageStore()
        val dispatchers = TestDispatcherProvider(testDispatcher)
        scans = RoomScanRepository(db.scanDao(), images, dispatchers)
        collection = RoomCollectionRepository(
            folders = db.folderDao(),
            items = db.itemDao(),
            computeValue = ComputeCollectionValueUseCase(),
            dispatchers = dispatchers,
            clock = { Instant.ofEpochMilli(1_000) },
            newId = { "folder-${++ids}" },
        )
    }

    @After
    fun tearDown() = db.close()

    private fun scan(id: String, at: Long = 0) = ScanRecord(
        id = id,
        createdAt = Instant.ofEpochMilli(at),
        obversePath = "/scans/$id-front.jpg",
        reversePath = "/scans/$id-back.jpg",
        identification = coin().copy(hints = listOf(VarietyHint("Doubled die", "Look at LIBERTY"))),
    )

    private fun item(id: String, scanId: String, folderId: String? = null, grade: Grade? = null, qty: Int = 1) =
        CollectionItem(
            id = id,
            scanId = scanId,
            folderId = folderId,
            grade = grade,
            purchasePrice = usd(2.5),
            quantity = qty,
            addedAt = Instant.ofEpochMilli(5),
        )

    @Test
    fun `scan round-trips including identification json`() = runTest(testDispatcher) {
        val record = scan("s1")
        scans.save(record)

        assertEquals(AppResult.Success(record), scans.get("s1"))
        assertEquals(AppResult.Success(record), scans.observe("s1").first())
    }

    @Test
    fun `recent scans are newest first and limited`() = runTest(testDispatcher) {
        scans.save(scan("old", at = 1))
        scans.save(scan("new", at = 3))
        scans.save(scan("mid", at = 2))

        val recent = scans.observeRecent(limit = 2).first().ok()
        assertEquals(listOf("new", "mid"), recent.map { it.id })
    }

    @Test
    fun `missing scan is NotFound`() = runTest(testDispatcher) {
        assertTrue(scans.get("nope").errorOrNull() is AppError.NotFound)
        assertTrue(scans.observe("nope").first().errorOrNull() is AppError.NotFound)
    }

    @Test
    fun `deleting a scan removes photos and cascades to collection items`() = runTest(testDispatcher) {
        scans.save(scan("s1"))
        collection.addItem(item("i1", "s1"))

        assertEquals(AppResult.Success(Unit), scans.delete("s1"))

        assertTrue(images.deleted.containsAll(listOf("/scans/s1-front.jpg", "/scans/s1-back.jpg")))
        assertTrue(collection.observeItem("i1").first().errorOrNull() is AppError.NotFound)
    }

    @Test
    fun `folder totals are grade aware and deleting a folder leaves items unsorted`() = runTest(testDispatcher) {
        scans.save(scan("s1"))
        val folderId = collection.createFolder("  Wheat cents ").ok()
        // coin(): circulated 1..3 (mid 2), uncirculated 10..20 (mid 15)
        collection.addItem(item("i1", "s1", folderId, Grade.FINE, qty = 2))
        collection.addItem(item("i2", "s1", folderId, Grade.MINT_STATE))

        val folder = collection.observeFolders().first().ok().single()
        assertEquals("Wheat cents", folder.name)
        assertEquals(3, folder.itemCount)
        assertEquals(usd(19.0), folder.totalValue)

        val detail = collection.observeFolder(folderId).first().ok()
        assertEquals(setOf("i1", "i2"), detail.items.map { it.item.id }.toSet())

        assertEquals(AppResult.Success(Unit), collection.deleteFolder(folderId))
        assertNull(collection.observeItem("i1").first().ok().item.folderId)
        assertTrue(collection.observeFolder(folderId).first().errorOrNull() is AppError.NotFound)
    }

    @Test
    fun `summary sums every item`() = runTest(testDispatcher) {
        scans.save(scan("s1"))
        collection.addItem(item("i1", "s1", grade = Grade.GOOD, qty = 3))

        val summary = collection.observeSummary().first().ok()
        assertEquals(3, summary.itemCount)
        assertEquals(usd(3.0), summary.totalLow)
        assertEquals(usd(9.0), summary.totalHigh)
        assertEquals(usd(7.5), summary.totalCost)
    }

    @Test
    fun `update and delete item report missing rows`() = runTest(testDispatcher) {
        scans.save(scan("s1"))
        collection.addItem(item("i1", "s1"))

        val updated = item("i1", "s1", grade = Grade.VERY_FINE).copy(gradeNotes = "light toning")
        assertEquals(AppResult.Success(Unit), collection.updateItem(updated))
        val stored = collection.observeItem("i1").first().ok().item
        assertEquals(Grade.VERY_FINE, stored.grade)
        assertEquals("light toning", stored.gradeNotes)
        assertEquals(usd(2.5), stored.purchasePrice)

        assertTrue(collection.updateItem(item("ghost", "s1")).errorOrNull() is AppError.NotFound)
        assertEquals(AppResult.Success(Unit), collection.deleteItem("i1"))
        assertTrue(collection.deleteItem("i1").errorOrNull() is AppError.NotFound)
    }

    @Test
    fun `folder names are validated and renamed`() = runTest(testDispatcher) {
        assertTrue(collection.createFolder("   ").errorOrNull() is AppError.Validation)
        val id = collection.createFolder("Silver").ok()
        assertEquals(AppResult.Success(Unit), collection.renameFolder(id, "Silver dollars"))
        assertEquals("Silver dollars", collection.observeFolder(id).first().ok().folder.name)
        assertTrue(collection.renameFolder("ghost", "x").errorOrNull() is AppError.NotFound)
    }

    @Test
    fun `adding an item for an unknown scan fails as storage error`() = runTest(testDispatcher) {
        assertTrue(collection.addItem(item("i1", "missing")).errorOrNull() is AppError.Storage)
    }

    private fun <T> AppResult<T>.ok(): T = when (this) {
        is AppResult.Success -> data
        is AppResult.Failure -> throw AssertionError("Expected success but got $error", error.cause)
    }
}
