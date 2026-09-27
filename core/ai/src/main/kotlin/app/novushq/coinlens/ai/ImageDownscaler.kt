package app.novushq.coinlens.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Keeps requests well under the 20 MB cap: long edge ≤ [MAX_EDGE_PX], JPEG q[JPEG_QUALITY]. */
internal object ImageDownscaler {
    const val MAX_EDGE_PX = 1600
    const val JPEG_QUALITY = 85
    const val MIME_JPEG = "image/jpeg"

    /** Null when the bytes are not a decodable image. */
    fun toJpeg(bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val (w, h) = targetSize(decoded.width, decoded.height, MAX_EDGE_PX)
        val scaled = if (w == decoded.width && h == decoded.height) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, w, h, true)
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return out.toByteArray()
    }

    /** Largest power of two that keeps the decoded long edge ≥ [maxEdge]; cheap first pass before exact scaling. */
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    /** Aspect-preserving size whose long edge is at most [maxEdge]. */
    fun targetSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        val longEdge = max(width, height)
        if (longEdge <= maxEdge) return width to height
        val ratio = maxEdge.toDouble() / longEdge
        return (width * ratio).roundToInt().coerceAtLeast(1) to (height * ratio).roundToInt().coerceAtLeast(1)
    }
}
