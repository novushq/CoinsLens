package app.novushq.coinlens.ai

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.identify.IdentifyEngine
import app.novushq.coinlens.identify.IdentifySpec
import app.novushq.coinlens.identify.ImageInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Deterministic, offline engine for fake mode (no google-services.json or
 * `-Pcoinlens.ai=fake`) and for tests. It knows nothing about coins: it is
 * handed model-shaped JSON [samples] and picks one from the first image's
 * bytes hash, then runs it through the real [IdentifySpec.parse] path.
 */
class FakeIdentifyEngine(
    private val samples: List<String>,
    private val dispatchers: DispatcherProvider,
    private val delayMillis: Long = DEFAULT_DELAY_MILLIS,
) : IdentifyEngine {

    init {
        require(samples.isNotEmpty()) { "fake engine needs at least one sample" }
    }

    override suspend fun <T> identify(spec: IdentifySpec<T>, images: List<ImageInput>): AppResult<T> {
        val first = images.firstOrNull()
            ?: return AppResult.Failure(AppError.Validation("images", "At least one photo is required"))
        if (delayMillis > 0) delay(delayMillis)
        return withContext(dispatchers.default) { spec.parse(samples[indexFor(first.bytes)]) }
    }

    /** Stable across runs and JVMs: a plain polynomial hash of the bytes. */
    fun indexFor(bytes: ByteArray): Int {
        var hash = 0
        for (b in bytes) hash = 31 * hash + (b.toInt() and 0xff)
        return Math.floorMod(hash, samples.size)
    }

    companion object {
        const val DEFAULT_DELAY_MILLIS = 900L
    }
}
