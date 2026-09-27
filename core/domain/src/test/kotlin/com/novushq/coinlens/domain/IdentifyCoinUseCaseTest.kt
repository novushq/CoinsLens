package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppError
import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.identify.ImageInput
import com.novushq.coinlens.model.CoinIdentification
import com.novushq.coinlens.model.Confidence
import com.novushq.coinlens.model.ItemKind
import com.novushq.coinlens.model.Rarity
import com.novushq.coinlens.model.ScanAllowance
import com.novushq.coinlens.model.ValueEstimate
import com.novushq.coinlens.model.ValueRange
import com.novushq.coinlens.model.usd
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdentifyCoinUseCaseTest {

    @get:Rule val main = MainDispatcherRule()

    private fun identified() = CoinIdentification(
        kind = ItemKind.COIN,
        name = "Lincoln cent",
        country = "USA",
        denomination = "1 cent",
        year = 1943,
        rarity = Rarity.COMMON,
        value = ValueEstimate(ValueRange(usd(0.5), usd(2.0)), null, Confidence.HIGH),
    )

    private fun setup(
        engineResult: AppResult<CoinIdentification> = AppResult.Success(identified()),
        used: Int = 0,
        bonus: Int = 0,
        pro: Boolean = false,
    ): Quad {
        val prefs = FakePreferences(used = used, bonus = bonus)
        val useCase = IdentifyCoinUseCase(
            FakeEngine(engineResult).also { q -> quadEngine = q },
            FakeScanRepository().also { quadScans = it },
            FakeImageStore(),
            prefs,
            FakeEntitlements(pro),
            TestDispatcherProvider(main.dispatcher),
            clock = { 1_700_000_000_000 },
            ids = { "scan-1" },
        )
        quadPrefs = prefs
        quadUseCase = useCase
        return Quad(useCase, prefs)
    }

    private lateinit var quadUseCase: IdentifyCoinUseCase
    private lateinit var quadPrefs: FakePreferences
    private lateinit var quadScans: FakeScanRepository
    private lateinit var quadEngine: FakeEngine

    data class Quad(val useCase: IdentifyCoinUseCase, val prefs: FakePreferences)

    @Test fun `success consumes the free scan and returns the id`() = runTest(main.dispatcher) {
        setup()
        val result = quadUseCase(listOf(ImageInput(ByteArray(4) { 1 })))
        assertEquals(AppResult.Success("scan-1"), result)
        assertEquals(1, quadPrefs.freeScansUsed.first())
        assertEquals(1, quadScans.records.size)
    }

    @Test fun `bonus is consumed before the free scan`() = runTest(main.dispatcher) {
        setup(bonus = 2)
        quadUseCase(listOf(ImageInput(ByteArray(4))))
        assertEquals(1, quadPrefs.bonusScans.first())
        assertEquals(0, quadPrefs.freeScansUsed.first())
    }

    @Test fun `pro scans consume nothing`() = runTest(main.dispatcher) {
        setup(pro = true)
        quadUseCase(listOf(ImageInput(ByteArray(4))))
        assertEquals(0, quadPrefs.freeScansUsed.first())
        assertEquals(0, quadPrefs.bonusScans.first())
    }

    @Test fun `exhausted allowance fails quota without calling the engine`() = runTest(main.dispatcher) {
        setup(used = 1)
        val result = quadUseCase(listOf(ImageInput(ByteArray(4))))
        assertTrue(result is AppResult.Failure && result.error is AppError.QuotaExceeded)
        assertEquals(0, quadEngine.calls)
        assertTrue(quadScans.records.isEmpty())
    }

    @Test fun `engine failure consumes nothing and persists nothing`() = runTest(main.dispatcher) {
        setup(engineResult = AppResult.Failure(AppError.Network()))
        val result = quadUseCase(listOf(ImageInput(ByteArray(4))))
        assertTrue(result is AppResult.Failure)
        assertEquals(0, quadPrefs.freeScansUsed.first())
        assertTrue(quadScans.records.isEmpty())
    }

    @Test fun `image store failure maps to Storage`() = runTest(main.dispatcher) {
        setup()
        val failingImages = object : ImageStore by FakeImageStore() {
            override suspend fun save(bytes: ByteArray, hint: String) =
                AppResult.Failure(AppError.Storage()) as AppResult<String>
        }
        val useCase = IdentifyCoinUseCase(
            FakeEngine(AppResult.Success(identified())), FakeScanRepository(),
            failingImages, quadPrefs, FakeEntitlements(false),
            TestDispatcherProvider(main.dispatcher),
        )
        val result = useCase(listOf(ImageInput(ByteArray(4))))
        assertTrue(result is AppResult.Failure && (result as AppResult.Failure).error is AppError.Storage)
    }

    @Test fun `empty images fail validation`() = runTest(main.dispatcher) {
        setup()
        val result = quadUseCase(emptyList())
        assertTrue(result is AppResult.Failure && result.error is AppError.Validation)
    }

    @Test fun `unrecognized result still consumes the scan`() = runTest(main.dispatcher) {
        setup(engineResult = AppResult.Success(CoinIdentification.unrecognized()))
        val result = quadUseCase(listOf(ImageInput(ByteArray(4))))
        assertTrue(result is AppResult.Success)
        assertEquals(1, quadPrefs.freeScansUsed.first())
    }

    @Test fun `allowance math hides free scans for pro`() = runTest(main.dispatcher) {
        val prefs = FakePreferences()
        val allowance = ObserveScanAllowanceUseCase(FakeEntitlements(true), prefs).invoke().first()
        assertEquals(ScanAllowance(true, 1, 0).canScan, allowance.canScan)
        assertTrue(allowance.canScan)
        assertFalse(ScanAllowance(false, 0, 0).canScan)
    }
}
