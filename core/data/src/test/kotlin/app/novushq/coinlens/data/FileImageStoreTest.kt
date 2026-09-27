package app.novushq.coinlens.data

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.testing.TestDispatcherProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileImageStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `save load delete round trip`() = runTest {
        val store = FileImageStore(File(tmp.root, "scans"), TestDispatcherProvider(StandardTestDispatcher(testScheduler)))
        val path = (store.save(byteArrayOf(9, 8, 7), "front") as AppResult.Success).data

        assertTrue(path.endsWith("-front.jpg"))
        assertArrayEquals(byteArrayOf(9, 8, 7), (store.load(path) as AppResult.Success).data)
        assertTrue(store.delete(path) is AppResult.Success)
        assertFalse(File(path).exists())
        assertTrue(store.load(path).errorOrNull() is AppError.NotFound)
    }

    @Test
    fun `rejects empty images and foreign paths`() = runTest {
        val store = FileImageStore(File(tmp.root, "scans"), TestDispatcherProvider(StandardTestDispatcher(testScheduler)))
        val outside = tmp.newFile("secret.txt")

        assertTrue(store.save(byteArrayOf(), "x").errorOrNull() is AppError.Validation)
        assertTrue(store.load(outside.absolutePath).errorOrNull() is AppError.Validation)
        assertTrue(store.delete(outside.absolutePath).errorOrNull() is AppError.Validation)
        assertTrue(outside.exists())
    }
}
