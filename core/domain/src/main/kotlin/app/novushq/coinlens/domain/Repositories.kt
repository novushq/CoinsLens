package app.novushq.coinlens.domain

import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.FolderDetail
import app.novushq.coinlens.model.Persona
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.ValuedItem
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    /** Newest first. */
    fun observeRecent(limit: Int = 20): Flow<AppResult<List<ScanRecord>>>

    /** Emits [app.novushq.coinlens.common.AppError.NotFound] once the scan is gone. */
    fun observe(id: String): Flow<AppResult<ScanRecord>>
    suspend fun get(id: String): AppResult<ScanRecord>

    /** Inserts or replaces; returns the record id. */
    suspend fun save(record: ScanRecord): AppResult<String>

    /** Deletes the scan, its photos and (by cascade) collection items made from it. */
    suspend fun delete(id: String): AppResult<Unit>
}

interface CollectionRepository {
    /** Folders by name, each with derived itemCount and grade-aware totalValue. */
    fun observeFolders(): Flow<AppResult<List<Folder>>>
    fun observeFolder(folderId: String): Flow<AppResult<FolderDetail>>

    /** Every item, newest first; filter by [CollectionItem.folderId] for unsorted. */
    fun observeItems(): Flow<AppResult<List<ValuedItem>>>
    fun observeItem(itemId: String): Flow<AppResult<ValuedItem>>
    fun observeSummary(): Flow<AppResult<CollectionSummary>>
    suspend fun createFolder(name: String): AppResult<String>
    suspend fun renameFolder(folderId: String, name: String): AppResult<Unit>

    /** Deletes the folder; its items become unsorted (ON DELETE SET NULL). */
    suspend fun deleteFolder(folderId: String): AppResult<Unit>
    suspend fun addItem(item: CollectionItem): AppResult<String>
    suspend fun updateItem(item: CollectionItem): AppResult<Unit>
    suspend fun deleteItem(itemId: String): AppResult<Unit>
}

/** App-private photo storage. Paths are absolute file paths usable as image models. */
interface ImageStore {
    suspend fun save(bytes: ByteArray, nameHint: String): AppResult<String>
    suspend fun load(path: String): AppResult<ByteArray>
    suspend fun delete(path: String): AppResult<Unit>
}

interface PreferencesRepository {
    val persona: Flow<Persona?>
    val onboardingDone: Flow<Boolean>
    val freeScansUsed: Flow<Int>
    val bonusScans: Flow<Int>
    suspend fun setPersona(persona: Persona?): AppResult<Unit>
    suspend fun setOnboardingDone(done: Boolean): AppResult<Unit>
    suspend fun setFreeScansUsed(used: Int): AppResult<Unit>
    suspend fun setBonusScans(count: Int): AppResult<Unit>
}

/**
 * Pro entitlement. Core binds a local default (never Pro); `:feature:paywall`
 * overrides the binding with the store-backed implementation.
 */
interface EntitlementRepository {
    val isPro: Flow<Boolean>
    suspend fun refresh(): AppResult<Boolean>
    suspend fun restore(): AppResult<Boolean>
}
