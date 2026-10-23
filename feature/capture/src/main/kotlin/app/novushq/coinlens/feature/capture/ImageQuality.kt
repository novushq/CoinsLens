package app.novushq.coinlens.feature.capture

/**
 * Pure image-quality math for the capture guide. Everything here operates on
 * plain luma arrays so it is JVM-testable with synthetic inputs; the screen
 * layer feeds it the Y plane of the CameraX preview (or a decoded gallery
 * photo) together with the guide circle geometry.
 */

/** Luma at or above this is treated as blown-out sensor data. */
const val GLARE_LUMA_THRESHOLD = 245

/** Fraction of guide pixels blown out before the glare banner shows. */
const val GLARE_FRACTION_THRESHOLD = 0.06

/**
 * Variance of Laplacian below this counts as blurry. Calibrated so a uniform
 * or heavily smoothed patch (≈0) warns while normal coin texture (typically
 * several hundred) does not.
 */
const val BLUR_VARIANCE_THRESHOLD = 100.0

/** Guide circle diameter as a fraction of the frame's short edge. */
const val GUIDE_DIAMETER_FRACTION = 0.72f

/**
 * Fraction of pixels inside the guide circle whose luma is >= [threshold].
 * Pixels are counted by centre point. Returns 0 when the circle is empty.
 */
fun glareFraction(
    luma: IntArray,
    width: Int,
    height: Int,
    centerX: Double,
    centerY: Double,
    radius: Double,
    threshold: Int = GLARE_LUMA_THRESHOLD,
): Double {
    var inside = 0
    var blown = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val dx = (x + 0.5) - centerX
            val dy = (y + 0.5) - centerY
            if (dx * dx + dy * dy <= radius * radius) {
                inside++
                if (luma[y * width + x] >= threshold) blown++
            }
        }
    }
    if (inside == 0) return 0.0
    return blown.toDouble() / inside
}

/** Advisory only: true when more than 6% of the guide area is blown out. */
fun isGlare(
    luma: IntArray,
    width: Int,
    height: Int,
    centerX: Double,
    centerY: Double,
    radius: Double,
): Boolean =
    glareFraction(luma, width, height, centerX, centerY, radius) > GLARE_FRACTION_THRESHOLD

/**
 * Variance of the 4-neighbour Laplacian over interior pixels inside the
 * guide circle. Flat/blurry patches score near 0; textured in-focus coin
 * surfaces score in the hundreds or more. Returns 0 when fewer than two
 * samples fall inside the circle.
 */
fun laplacianVariance(
    luma: IntArray,
    width: Int,
    height: Int,
    centerX: Double,
    centerY: Double,
    radius: Double,
): Double {
    var count = 0
    var mean = 0.0
    var m2 = 0.0
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val dx = (x + 0.5) - centerX
            val dy = (y + 0.5) - centerY
            if (dx * dx + dy * dy > radius * radius) continue
            val i = y * width + x
            val response = (
                luma[i - width] + luma[i - 1] + luma[i + 1] + luma[i + width] -
                    4 * luma[i]
                ).toDouble()
            count++
            val delta = response - mean
            mean += delta / count
            m2 += delta * (response - mean)
        }
    }
    if (count < 2) return 0.0
    return m2 / count
}

/** Advisory only: true when focus quality is below [threshold]. */
fun isBlurry(variance: Double, threshold: Double = BLUR_VARIANCE_THRESHOLD): Boolean =
    variance < threshold
