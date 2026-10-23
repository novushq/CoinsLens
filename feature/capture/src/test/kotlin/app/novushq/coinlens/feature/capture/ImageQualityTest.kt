package app.novushq.coinlens.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageQualityTest {

    private fun fullFrame(w: Int, h: Int) = Triple(w / 2.0, h / 2.0, maxOf(w, h).toDouble())

    @Test
    fun `blown-out frame reports full glare`() {
        val (cx, cy, r) = fullFrame(100, 100)
        val luma = IntArray(100 * 100) { 255 }
        assertEquals(1.0, glareFraction(luma, 100, 100, cx, cy, r), 0.0)
        assertTrue(isGlare(luma, 100, 100, cx, cy, r))
    }

    @Test
    fun `normal frame reports no glare`() {
        val (cx, cy, r) = fullFrame(100, 100)
        val luma = IntArray(100 * 100) { 128 }
        assertEquals(0.0, glareFraction(luma, 100, 100, cx, cy, r), 0.0)
        assertFalse(isGlare(luma, 100, 100, cx, cy, r))
    }

    @Test
    fun `glare triggers only above six percent`() {
        val (cx, cy, r) = fullFrame(100, 100)
        val over = IntArray(100 * 100) { if (it < 700) 255 else 128 }
        val under = IntArray(100 * 100) { if (it < 500) 255 else 128 }
        assertEquals(0.07, glareFraction(over, 100, 100, cx, cy, r), 1e-9)
        assertTrue(isGlare(over, 100, 100, cx, cy, r))
        assertFalse(isGlare(under, 100, 100, cx, cy, r))
    }

    @Test
    fun `bright pixels outside the guide are ignored`() {
        val luma = IntArray(10 * 10) { 128 }.also {
            it[0] = 255
            it[9] = 255
            it[90] = 255
            it[99] = 255
        }
        assertEquals(0.0, glareFraction(luma, 10, 10, 5.0, 5.0, 1.0), 0.0)
        assertFalse(isGlare(luma, 10, 10, 5.0, 5.0, 1.0))
    }

    @Test
    fun `empty circle reports zero glare`() {
        val luma = IntArray(10 * 10) { 255 }
        assertEquals(0.0, glareFraction(luma, 10, 10, 5.0, 5.0, 0.0), 0.0)
    }

    @Test
    fun `flat frame is blurry`() {
        val (cx, cy, r) = fullFrame(20, 20)
        val luma = IntArray(20 * 20) { 128 }
        assertEquals(0.0, laplacianVariance(luma, 20, 20, cx, cy, r), 0.0)
        assertTrue(isBlurry(laplacianVariance(luma, 20, 20, cx, cy, r)))
    }

    @Test
    fun `textured frame is sharp`() {
        val (cx, cy, r) = fullFrame(20, 20)
        val luma = IntArray(20 * 20) { i -> ((i / 20 + i % 20) % 2) * 255 }
        val variance = laplacianVariance(luma, 20, 20, cx, cy, r)
        assertTrue(variance > BLUR_VARIANCE_THRESHOLD)
        assertFalse(isBlurry(variance))
    }

    @Test
    fun `blur threshold is exclusive`() {
        assertTrue(isBlurry(BLUR_VARIANCE_THRESHOLD - 0.1))
        assertFalse(isBlurry(BLUR_VARIANCE_THRESHOLD))
    }
}
