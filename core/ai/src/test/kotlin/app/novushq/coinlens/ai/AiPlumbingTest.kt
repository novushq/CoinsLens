package app.novushq.coinlens.ai

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.domain.CoinIdentificationSpec
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPlumbingTest {

    @Test
    fun `transport errors map to Network or Timeout`() {
        assertTrue(AiErrorMapper.map(IOException("offline")) is AppError.Network)
        assertTrue(AiErrorMapper.map(SocketTimeoutException()) is AppError.Timeout)
        assertTrue(AiErrorMapper.map(IllegalStateException("x", IOException())) is AppError.Network)
    }

    @Test
    fun `anything else is Unknown`() {
        assertTrue(AiErrorMapper.map(IllegalStateException("boom")) is AppError.Unknown)
    }

    @Test
    fun `downscale keeps aspect and caps the long edge`() {
        assertEquals(1600 to 1200, ImageDownscaler.targetSize(4000, 3000, 1600))
        assertEquals(900 to 1600, ImageDownscaler.targetSize(2700, 4800, 1600))
        assertEquals(800 to 600, ImageDownscaler.targetSize(800, 600, 1600))
        assertEquals(2, ImageDownscaler.sampleSize(4000, 3000, 1600))
        assertEquals(1, ImageDownscaler.sampleSize(3000, 2000, 1600))
    }

    @Test
    fun `coin schema maps to a firebase schema with the same required fields`() {
        val spec = CoinIdentificationSpec()
        val mapped = spec.schema.toFirebaseSchema()
        assertEquals(spec.schema.properties.keys, mapped.properties?.keys)
        val required = spec.schema.properties.keys - spec.schema.optional.toSet()
        assertEquals(required, mapped.required?.toSet())
    }

    @Test
    fun `static model config exposes the bundled default`() {
        val info = AiInfoProvider(AiMode.FAKE, StaticModelConfig()).current
        assertEquals(AiInfo(AiMode.FAKE, "gemini-3.5-flash-lite"), info)
    }
}
