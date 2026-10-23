package app.novushq.coinlens.feature.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.data.ScanDraft
import app.novushq.coinlens.domain.AddToCollectionUseCase
import app.novushq.coinlens.domain.CoinIdentificationSpec
import app.novushq.coinlens.domain.IdentifyCoinUseCase
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.Rarity
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.VarietyHint
import app.novushq.coinlens.model.usd
import app.novushq.coinlens.testing.FakeCollectionRepository
import app.novushq.coinlens.testing.FakeEngine
import app.novushq.coinlens.testing.FakeEntitlements
import app.novushq.coinlens.testing.FakeImageStore
import app.novushq.coinlens.testing.FakePreferences
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ResultViewModelTest {
    private val now = Instant.parse("2026-09-27T10:00:00Z")

    private inner class Deps(
        val engine: FakeEngine,
        val scans: FakeScanRepository = FakeScanRepository(),
        val images: FakeImageStore = FakeImageStore(),
        val prefs: FakePreferences = FakePreferences(),
        val collections: FakeCollectionRepository = FakeCollectionRepository(),
        val draft: ScanDraft = ScanDraft(),
    ) {
        fun viewModel(scanId: String? = null): ResultViewModel {
            val handle = if (scanId == null) {
                SavedStateHandle()
            } else {
                SavedStateHandle(mapOf("scanId" to scanId))
            }
            return ResultViewModel(
                savedStateHandle = handle,
                identify = IdentifyCoinUseCase(
                    engine = engine,
                    spec = CoinIdentificationSpec(),
                    scans = scans,
                    imageStore = images,
                    prefs = prefs,
                    allowance = ObserveScanAllowanceUseCase(FakeEntitlements(), prefs),
                    clock = { now },
                    newId = { "scan-1" },
                ),
                scans = scans,
                draft = draft,
                addToCollection = AddToCollectionUseCase(collections),
                collections = collections,
            )
        }
    }

    private fun successData(viewModel: ResultViewModel): ResultData {
        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        return (state as UiState.Success).data
    }

    @Test
    fun `identifying a draft shows the result card and consumes one scan`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val deps = Deps(
            engine = FakeEngine(
                AppResult.Success(
                    coin().copy(
                        year = 1969,
                        mintMark = "S",
                        composition = "Copper",
                        rarity = Rarity.RARE,
                        hints = listOf(VarietyHint("doubled die", whereToLook = "the date")),
                        alternatives = listOf("1969 Philadelphia cent"),
                    ),
                ),
            ),
        )
        deps.draft.setObverse(byteArrayOf(1, 2, 3))
        val viewModel = deps.viewModel()
        val store = ViewModelStore().apply { put("result", viewModel) }

        try {
            runCurrent()
            val data = successData(viewModel)
            val coin = data.record.identification
            assertEquals("Test cent", coin.name)
            assertEquals("USA", coin.country)
            assertEquals(1969, coin.year)
            assertEquals("S", coin.mintMark)
            assertEquals("Copper", coin.composition)
            assertEquals(Rarity.RARE, coin.rarity)
            assertEquals(1, deps.prefs.usedState.value)
            assertTrue(deps.draft.current.toImages().isEmpty())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `stored scanId loads the record without calling the engine`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val deps = Deps(engine = FakeEngine(AppResult.Success(coin())))
        deps.scans.records["s-9"] = ScanRecord(
            id = "s-9",
            createdAt = now,
            obversePath = "/files/obverse-1.jpg",
            identification = coin(),
        )
        val viewModel = deps.viewModel(scanId = "s-9")
        val store = ViewModelStore().apply { put("result", viewModel) }

        try {
            runCurrent()
            val data = successData(viewModel)
            assertEquals("s-9", data.record.id)
            assertEquals(0, deps.engine.calls)
            assertEquals(0, deps.prefs.usedState.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `unrecognized result renders the not-a-coin state and consumes nothing`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val deps = Deps(engine = FakeEngine(AppResult.Success(coin(recognized = false))))
        deps.draft.setObverse(byteArrayOf(1, 2, 3))
        val viewModel = deps.viewModel()
        val store = ViewModelStore().apply { put("result", viewModel) }

        try {
            runCurrent()
            val data = successData(viewModel)
            assertFalse(data.record.identification.recognized)
            assertEquals(0, deps.prefs.usedState.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `error then retry succeeds and the failed attempt consumes nothing`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val engine = FakeEngine(AppResult.Failure(AppError.Network()))
        val deps = Deps(engine = engine)
        deps.draft.setObverse(byteArrayOf(1, 2, 3))
        val viewModel = deps.viewModel()
        val store = ViewModelStore().apply { put("result", viewModel) }

        try {
            runCurrent()
            assertTrue(viewModel.state.value is UiState.Error)

            engine.result = AppResult.Success(coin())
            deps.draft.setObverse(byteArrayOf(1, 2, 3))
            viewModel.retry()
            runCurrent()

            successData(viewModel)
            assertEquals(2, engine.calls)
            assertEquals(1, deps.prefs.usedState.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `add to collection saves folder grade price and notes`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val deps = Deps(engine = FakeEngine(AppResult.Success(coin())))
        deps.draft.setObverse(byteArrayOf(1, 2, 3))
        val viewModel = deps.viewModel()
        val store = ViewModelStore().apply { put("result", viewModel) }

        try {
            runCurrent()
            successData(viewModel)

            viewModel.openAddSheet()
            runCurrent()
            viewModel.setNewFolderName("Pennies")
            viewModel.createFolder()
            runCurrent()
            viewModel.setGrade(Grade.FINE)
            viewModel.setPrice("12.50")
            viewModel.setNotes("flea market find")
            viewModel.confirmAdd()
            runCurrent()

            val data = successData(viewModel)
            assertTrue(data.addedToCabinet)
            assertNull(data.sheet)
            val item = deps.collections.items.values.single()
            assertEquals("scan-1", item.scanId)
            assertEquals("f1", item.folderId)
            assertEquals(Grade.FINE, item.grade)
            assertEquals(usd(12.5), item.purchasePrice)
            assertEquals("flea market find", item.gradeNotes)
            assertEquals(1, deps.engine.calls)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `invalid price blocks the save`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val deps = Deps(engine = FakeEngine(AppResult.Success(coin())))
        deps.draft.setObverse(byteArrayOf(1, 2, 3))
        val viewModel = deps.viewModel()
        val store = ViewModelStore().apply { put("result", viewModel) }

        try {
            runCurrent()
            viewModel.openAddSheet()
            runCurrent()
            viewModel.setPrice("not-a-price")
            viewModel.confirmAdd()
            runCurrent()

            val data = successData(viewModel)
            assertTrue(data.sheet?.priceError == true)
            assertTrue(deps.collections.items.isEmpty())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
