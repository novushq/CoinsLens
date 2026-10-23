package app.novushq.coinlens.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CoinCropTest {

    @Test
    fun `crop is centered square of 72 percent of short edge`() {
        assertEquals(SquareCrop(left = 140, top = 640, size = 720), guideSquareCrop(1000, 2000))
        assertEquals(SquareCrop(left = 640, top = 140, size = 720), guideSquareCrop(2000, 1000))
        assertEquals(SquareCrop(left = 70, top = 70, size = 360), guideSquareCrop(500, 500))
    }

    @Test
    fun `tiny images still produce a one-pixel crop`() {
        assertEquals(SquareCrop(left = 0, top = 0, size = 1), guideSquareCrop(1, 1))
    }

    @Test
    fun `output keeps small crops at full size`() {
        assertEquals(1f, outputScale(1600), 0f)
        assertEquals(1f, outputScale(800), 0f)
    }

    @Test
    fun `output downscales large crops to the cap`() {
        assertEquals(0.5f, outputScale(3200), 1e-6f)
        assertEquals(1600, (3200 * outputScale(3200)).toInt())
    }

    @Test
    fun `output contract constants match the spec`() {
        assertEquals(1600, MAX_OUTPUT_PX)
        assertEquals(85, OUTPUT_JPEG_QUALITY)
        assertEquals(0.72f, GUIDE_DIAMETER_FRACTION, 0f)
    }
}
