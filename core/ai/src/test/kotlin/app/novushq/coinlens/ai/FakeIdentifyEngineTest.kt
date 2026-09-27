package app.novushq.coinlens.ai

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.domain.CoinIdentificationSpec
import app.novushq.coinlens.identify.ImageInput
import app.novushq.coinlens.identify.ImageRole
import app.novushq.coinlens.testing.TestDispatcherProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeIdentifyEngineTest {

    private val spec = CoinIdentificationSpec()

    private fun engine(dispatcher: TestDispatcher) =
        FakeIdentifyEngine(SampleCatalogue.json, TestDispatcherProvider(dispatcher), delayMillis = 900)

    @Test
    fun `catalogue has at least ten recognised entries and one unrecognised case`() {
        assertTrue(SampleCatalogue.entries.count { it.recognized } >= 10)
        assertEquals(1, SampleCatalogue.entries.count { !it.recognized })
        assertEquals(SampleCatalogue.entries.size, SampleCatalogue.json.size)
    }

    @Test
    fun `every sample round-trips through the real spec parser unchanged`() {
        SampleCatalogue.json.forEachIndexed { i, json ->
            val parsed = spec.parse(json)
            assertEquals("entry $i", AppResult.Success(SampleCatalogue.entries[i]), parsed)
        }
    }

    @Test
    fun `hints in the catalogue exist only where the docs call for them`() {
        val withHints = SampleCatalogue.entries.filter { it.hints.isNotEmpty() }.map { it.year }
        assertEquals(listOf(1955, 2004, 1983), withHints)
    }

    @Test
    fun `same bytes always give the same entry`() = runTest {
        val engine = engine(StandardTestDispatcher(testScheduler))
        val image = listOf(ImageInput(byteArrayOf(9, 8, 7, 6), role = ImageRole.PRIMARY))
        val first = engine.identify(spec, image)
        val second = engine.identify(spec, image)
        assertEquals(first, second)
        assertTrue(first.isSuccess)
    }

    @Test
    fun `selection depends only on the first image`() = runTest {
        val engine = engine(StandardTestDispatcher(testScheduler))
        val front = ImageInput(byteArrayOf(1, 2, 3), role = ImageRole.PRIMARY)
        val a = engine.identify(spec, listOf(front))
        val b = engine.identify(spec, listOf(front, ImageInput(byteArrayOf(4), role = ImageRole.SECONDARY)))
        assertEquals(a, b)
    }

    @Test
    fun `every catalogue entry is reachable from some input`() {
        val engine = FakeIdentifyEngine(SampleCatalogue.json, TestDispatcherProvider(StandardTestDispatcher()))
        val hit = (0 until 256).map { engine.indexFor(byteArrayOf(it.toByte())) }.toSet()
        assertEquals(SampleCatalogue.entries.indices.toSet(), hit)
    }

    @Test
    fun `waits the configured delay before answering`() = runTest {
        val engine = engine(StandardTestDispatcher(testScheduler))
        val start = testScheduler.currentTime
        engine.identify(spec, listOf(ImageInput(byteArrayOf(1), role = ImageRole.PRIMARY)))
        assertEquals(900L, testScheduler.currentTime - start)
    }

    @Test
    fun `no images is a validation error`() = runTest {
        val result = engine(StandardTestDispatcher(testScheduler)).identify(spec, emptyList())
        assertTrue(result.errorOrNull() is AppError.Validation)
    }
}
