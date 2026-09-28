package app.novushq.coinlens.feature.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.toDisplayString

data class SharePalette(
    val background: Int,
    val surface: Int,
    val primary: Int,
    val text: Int,
    val secondary: Int,
    val outline: Int,
)

object ShareCardRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val INSET = 88f

    fun render(context: Context, record: ScanRecord, palette: SharePalette): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(palette.background)
        canvas.drawRoundRect(RectF(36f, 36f, WIDTH - 36f, HEIGHT - 36f), 48f, 48f, paint.apply { color = palette.surface })

        val photo = decodePhoto(record.obversePath)
        if (photo != null) {
            val photoBounds = RectF(INSET, 110f, WIDTH - INSET, 770f)
            val crop = centerCrop(photo)
            val clip = Path().apply { addOval(photoBounds, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clip)
            canvas.drawBitmap(photo, crop, photoBounds, paint)
            canvas.restore()
            photo.recycle()
            paint.color = palette.primary
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 7f
            canvas.drawOval(photoBounds, paint)
            paint.style = Paint.Style.FILL
        }

        val coin = record.identification
        var titleSize = 60f
        paint.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        paint.color = palette.text
        val title = if (coin.recognized) coin.name else context.getString(R.string.share_unrecognized)
        do {
            paint.textSize = titleSize
            titleSize -= 2f
        } while (paint.measureText(title) > WIDTH - INSET * 2 && titleSize > 34f)
        canvas.drawText(title, INSET, 875f, paint)

        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        paint.textSize = 26f
        paint.color = palette.secondary
        val metadata = listOfNotNull(
            coin.country.takeIf(String::isNotBlank),
            (coin.year?.toString() ?: coin.yearText).takeIf(String::isNotBlank),
            (coin.mintMark ?: coin.mint).takeIf { !it.isNullOrBlank() },
        ).joinToString(" · ").uppercase()
        canvas.drawText(metadata, INSET, 930f, paint)

        if (coin.recognized) {
            val range = coin.value.circulated ?: coin.value.uncirculated
            if (range != null) {
                paint.color = palette.text
                paint.textSize = 52f
                canvas.drawText(context.getString(R.string.share_estimate), INSET, 1040f, paint)
                paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                paint.textSize = 36f
                canvas.drawText(
                    context.getString(R.string.share_range, range.low.toDisplayString(), range.high.toDisplayString()),
                    INSET,
                    1098f,
                    paint,
                )
                paint.color = palette.primary
                paint.textSize = 25f
                canvas.drawText(context.getString(R.string.share_confidence, coin.value.confidence.name), INSET, 1152f, paint)
            }
        }

        paint.color = palette.outline
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(INSET, 1190f, WIDTH - INSET, 1190f, paint)
        paint.style = Paint.Style.FILL
        paint.color = palette.secondary
        paint.textSize = 20f
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        canvas.drawText(context.getString(R.string.share_disclaimer), INSET, 1230f, paint)
        canvas.drawText(context.getString(R.string.share_brand), INSET, 1280f, paint)
        return bitmap
    }

    private fun decodePhoto(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun centerCrop(bitmap: Bitmap): Rect {
        val side = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - side) / 2
        val top = (bitmap.height - side) / 2
        return Rect(left, top, left + side, top + side)
    }
}
