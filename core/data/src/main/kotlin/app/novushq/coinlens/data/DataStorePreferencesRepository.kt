package app.novushq.coinlens.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.safeCall
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.model.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStorePreferencesRepository(
    private val store: DataStore<Preferences>,
) : PreferencesRepository {

    // A corrupt or unreadable file reads as defaults rather than crashing the app.
    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val persona: Flow<Persona?> = prefs
        .map { p -> p[PERSONA]?.let { name -> Persona.entries.firstOrNull { it.name == name } } }
        .distinctUntilChanged()

    override val onboardingDone: Flow<Boolean> = prefs.map { it[ONBOARDING_DONE] ?: false }.distinctUntilChanged()

    override val freeScansUsed: Flow<Int> = prefs.map { it[FREE_SCANS_USED] ?: 0 }.distinctUntilChanged()

    override val bonusScans: Flow<Int> = prefs.map { it[BONUS_SCANS] ?: 0 }.distinctUntilChanged()

    override suspend fun setPersona(persona: Persona?): AppResult<Unit> = write {
        if (persona == null) it.remove(PERSONA) else it[PERSONA] = persona.name
    }

    override suspend fun setOnboardingDone(done: Boolean): AppResult<Unit> = write { it[ONBOARDING_DONE] = done }

    override suspend fun setFreeScansUsed(used: Int): AppResult<Unit> =
        write { it[FREE_SCANS_USED] = used.coerceAtLeast(0) }

    override suspend fun setBonusScans(count: Int): AppResult<Unit> = write { it[BONUS_SCANS] = count.coerceAtLeast(0) }

    private suspend fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit): AppResult<Unit> =
        safeCall(::storageError) {
            store.edit { block(it) }
            Unit
        }

    companion object {
        const val FILE_NAME = "coinlens_prefs"
        private val PERSONA = stringPreferencesKey("persona")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val FREE_SCANS_USED = intPreferencesKey("free_scans_used")
        private val BONUS_SCANS = intPreferencesKey("bonus_scans")
    }
}
