package app.novushq.coinlens.feature.capture

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

/** Longest output edge in px after downscaling. */
const val MAX_OUTPUT_PX = 1600

/** JPEG quality for photos written to [app.novushq.coinlens.data.ScanDraft]. */
const val OUTPUT_JPEG_QUALITY = 85

/** Centered square matching the guide circle's bounding box. */
data class SquareCrop(val left: Int, val top: Int, val size: Int)

/**
 * Centered square of [GUIDE_DIAMETER_FRACTION] × the short edge. The guide
 * overlay is drawn with the same fraction of the displayed short edge, so
 * this crops the photo to what the user saw inside the circle.
 */
fun guideSquareCrop(imageWidth: Int, imageHeight: Int): SquareCrop {
    val size = (minOf(imageWidth, imageHeight) * GUIDE_DIAMETER_FRACTION).toInt()
        .coerceAtLeast(1)
    return SquareCrop(
        left = ((imageWidth - size) / 2).coerceAtLeast(0),
        top = ((imageHeight - size) / 2).coerceAtLeast(0),
        size = size,
    )
}

/** Scale factor (≤ 1) so the cropped square's longest edge fits [MAX_OUTPUT_PX]. */
fun outputScale(cropSize: Int, maxPx: Int = MAX_OUTPUT_PX): Float =
    if (cropSize <= maxPx) 1f else maxPx.toFloat() / cropSize

/**
 * Crop [source] to the guide square, downscale so no edge exceeds
 * [MAX_OUTPUT_PX], and encode as JPEG at [OUTPUT_JPEG_QUALITY]. The caller
 * owns [source]; the returned bytes are ready for ScanDraft.
 */
fun cropAndCompress(source: Bitmap, maxPx: Int = MAX_OUTPUT_PX): ByteArray {
    val crop = guideSquareCrop(source.width, source.height)
    val safeSize = minOf(crop.size, source.width - crop.left, source.height - crop.top)
    var cropped = Bitmap.createBitmap(source, crop.left, crop.top, safeSize, safeSize)
    val scale = outputScale(safeSize, maxPx)
    if (scale < 1f) {
        val edge = (safeSize * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(cropped, edge, edge, true)
        if (scaled !== cropped) cropped.recycle()
        cropped = scaled
    }
    val out = ByteArrayOutputStream()
    cropped.compress(Bitmap.CompressFormat.JPEG, OUTPUT_JPEG_QUALITY, out)
    if (cropped !== source) cropped.recycle()
    return out.toByteArray()
}

/** Rotate a decoded gallery bitmap upright; no-op for [degrees] == 0. */
fun applyExifRotation(source: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return source
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    if (rotated !== source) source.recycle()
    return rotated
}
