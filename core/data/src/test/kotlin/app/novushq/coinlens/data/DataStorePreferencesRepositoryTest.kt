package app.novushq.coinlens.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.model.Persona
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStorePreferencesRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun repo(file: File = File(tmp.root, "test.preferences_pb")) =
        DataStorePreferencesRepository(PreferenceDataStoreFactory.create(scope = scope) { file })

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `defaults are empty`() = runTest {
        val prefs = repo()
        assertNull(prefs.persona.first())
        assertFalse(prefs.onboardingDone.first())
        assertEquals(0, prefs.freeScansUsed.first())
        assertEquals(0, prefs.bonusScans.first())
    }

    @Test
    fun `values persist and negatives clamp to zero`() = runTest {
        val prefs = repo()
        assertEquals(AppResult.Success(Unit), prefs.setPersona(Persona.DETECTORIST))
        prefs.setOnboardingDone(true)
        prefs.setFreeScansUsed(1)
        prefs.setBonusScans(-4)

        assertEquals(Persona.DETECTORIST, prefs.persona.first())
        assertTrue(prefs.onboardingDone.first())
        assertEquals(1, prefs.freeScansUsed.first())
        assertEquals(0, prefs.bonusScans.first())

        prefs.setPersona(null)
        assertNull(prefs.persona.first())
    }

    @Test
    fun `corrupt file reads as defaults`() = runTest {
        val file = File(tmp.root, "corrupt.preferences_pb").apply { writeBytes(byteArrayOf(0x7f, 0x01, 0x02)) }
        val prefs = repo(file)
        assertFalse(prefs.onboardingDone.first())
    }
}
