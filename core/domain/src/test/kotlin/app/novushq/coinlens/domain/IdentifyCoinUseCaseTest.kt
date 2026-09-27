package app.novushq.coinlens.domain

import app.novushq.coinlens.testing.*

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IdentifyCoinUseCaseTest {
    private val engine = FakeEngine(AppResult.Success(coin()))
    private val scans = FakeScanRepository()
    private val images = FakeImageStore()
    private val prefs = FakePreferences()
    private val entitlements = FakeEntitlements()
    private val now = Instant.parse("2026-09-27T10:00:00Z")

    private val useCase = IdentifyCoinUseCase(
        engine = engine,
        spec = CoinIdentificationSpec(),
        scans = scans,
        imageStore = images,
        prefs = prefs,
        allowance = ObserveScanAllowanceUseCase(entitlements, prefs),
        clock = { now },
        newId = { "scan-1" },
    )

    @Test
    fun `first free scan succeeds, persists record and consumes the free scan`() = runTest {
        val result = useCase(listOf(front, back))

        assertEquals(AppResult.Success("scan-1"), result)
        val record = scans.records.getValue("scan-1")
        assertEquals(now, record.createdAt)
        assertEquals(2, images.files.size)
        assertTrue(record.reversePath != null)
        assertEquals(1, prefs.usedState.value)
        assertEquals(listOf(front, back), engine.lastImages)
    }

    @Test
    fun `reverse is optional`() = runTest {
        useCase(listOf(front))
        assertEquals(null, scans.records.getValue("scan-1").reversePath)
    }

    @Test
    fun `no allowance fails with ScanLimitReached without calling the engine`() = runTest {
        prefs.usedState.value = 1

        val result = useCase(listOf(front))

        assertTrue(result.errorOrNull() is AppError.ScanLimitReached)
        assertEquals(0, engine.calls)
        assertTrue(images.files.isEmpty())
    }

    @Test
    fun `bonus scan is spent before the free scan`() = runTest {
        prefs.bonusState.value = 1

        useCase(listOf(front))

        assertEquals(0, prefs.bonusState.value)
        assertEquals(0, prefs.usedState.value)
    }

    @Test
    fun `bonus scan unlocks a scan after the free one is used`() = runTest {
        prefs.usedState.value = 1
        prefs.bonusState.value = 1

        assertTrue(useCase(listOf(front)).isSuccess)
        assertEquals(0, prefs.bonusState.value)
        assertEquals(1, prefs.usedState.value)
    }

    @Test
    fun `pro scans never consume allowance`() = runTest {
        entitlements.state.value = true
        prefs.usedState.value = 5

        assertTrue(useCase(listOf(front)).isSuccess)
        assertEquals(5, prefs.usedState.value)
    }

    @Test
    fun `engine failure consumes nothing, persists nothing and removes photos`() = runTest {
        engine.result = AppResult.Failure(AppError.Network())

        val result = useCase(listOf(front, back))

        assertTrue(result.errorOrNull() is AppError.Network)
        assertEquals(0, prefs.usedState.value)
        assertTrue(scans.records.isEmpty())
        assertTrue(images.files.isEmpty())
    }

    @Test
    fun `retry after failure still has the free scan`() = runTest {
        engine.result = AppResult.Failure(AppError.QuotaExceeded())
        useCase(listOf(front))
        engine.result = AppResult.Success(coin())

        assertTrue(useCase(listOf(front)).isSuccess)
        assertEquals(1, prefs.usedState.value)
    }

    @Test
    fun `unrecognized result is saved but does not consume the scan`() = runTest {
        engine.result = AppResult.Success(coin(recognized = false))

        assertTrue(useCase(listOf(front)).isSuccess)
        assertEquals(0, prefs.usedState.value)
        assertEquals(1, scans.records.size)
    }

    @Test
    fun `photo save failure maps to storage error and cleans up`() = runTest {
        images.failOnSave = 2

        val result = useCase(listOf(front, back))

        assertTrue(result.errorOrNull() is AppError.Storage)
        assertTrue(images.files.isEmpty())
        assertEquals(0, engine.calls)
    }

    @Test
    fun `record save failure consumes nothing`() = runTest {
        scans.failSave = true

        assertTrue(useCase(listOf(front)).errorOrNull() is AppError.Storage)
        assertEquals(0, prefs.usedState.value)
        assertTrue(images.files.isEmpty())
    }

    @Test
    fun `missing obverse is a validation error`() = runTest {
        assertTrue(useCase(listOf(back)).errorOrNull() is AppError.Validation)
        assertTrue(useCase(emptyList()).errorOrNull() is AppError.Validation)
    }
}
