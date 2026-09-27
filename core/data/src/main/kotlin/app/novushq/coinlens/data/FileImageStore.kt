package app.novushq.coinlens.data

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.flatMap
import app.novushq.coinlens.common.safeCall
import app.novushq.coinlens.domain.ImageStore
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Scan photos live in app-private storage under [root]; paths handed out are absolute. */
class FileImageStore(
    private val root: File,
    private val dispatchers: DispatcherProvider,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ImageStore {

    override suspend fun save(bytes: ByteArray, nameHint: String): AppResult<String> = withContext(dispatchers.io) {
        if (bytes.isEmpty()) return@withContext AppResult.Failure(AppError.Validation("image", "Image is empty"))
        safeCall(::storageError) {
            root.mkdirs()
            val safeHint = nameHint.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(24)
            val file = File(root, "${newId()}${if (safeHint.isEmpty()) "" else "-$safeHint"}.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        }
    }

    override suspend fun load(path: String): AppResult<ByteArray> = withContext(dispatchers.io) {
        owned(path).flatMap { file ->
            if (!file.exists()) {
                AppResult.Failure(AppError.NotFound("Image not found"))
            } else {
                safeCall(::storageError) { file.readBytes() }
            }
        }
    }

    override suspend fun delete(path: String): AppResult<Unit> = withContext(dispatchers.io) {
        owned(path).flatMap { file ->
            safeCall(::storageError) {
                if (file.exists() && !file.delete()) error("Could not delete image")
            }
        }
    }

    // Refuse to touch anything outside our own directory.
    private fun owned(path: String): AppResult<File> {
        val file = File(path).canonicalFile
        val base = root.canonicalFile
        return if (file.parentFile == base) {
            AppResult.Success(file)
        } else {
            AppResult.Failure(AppError.Validation("path", "Path is outside the image store"))
        }
    }

    companion object {
        const val DIR_NAME = "scans"
    }
}
