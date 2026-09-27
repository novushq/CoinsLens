package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.model.CollectionItem
import com.novushq.coinlens.model.CollectionSummary
import com.novushq.coinlens.model.Folder
import com.novushq.coinlens.model.FolderDetail
import com.novushq.coinlens.model.Grade
import com.novushq.coinlens.model.Money
import com.novushq.coinlens.model.Persona
import com.novushq.coinlens.model.ScanRecord
import com.novushq.coinlens.model.ValuedItem
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun observeRecent(limit: Int = 10): Flow<List<ScanRecord>>
    fun observe(id: String): Flow<ScanRecord?>
    suspend fun get(id: String): AppResult<ScanRecord>
    /** Persists the record, returns its id. */
    suspend fun save(record: ScanRecord): AppResult<String>
    suspend fun delete(id: String): AppResult<Unit>
}

interface CollectionRepository {
    fun observeFolders(): Flow<List<Folder>>
    fun observeFolder(folderId: String): Flow<FolderDetail?>
    fun observeItems(folderId: String? = null): Flow<List<ValuedItem>>
    fun observeItem(itemId: String): Flow<ValuedItem?>
    fun observeSummary(): Flow<CollectionSummary>
    suspend fun createFolder(name: String): AppResult<String>
    suspend fun renameFolder(folderId: String, name: String): AppResult<Unit>
    /** Deletes the folder; its items move to Unsorted. */
    suspend fun deleteFolder(folderId: String): AppResult<Unit>
    suspend fun addItem(item: CollectionItem): AppResult<String>
    suspend fun updateItem(item: CollectionItem): AppResult<Unit>
    suspend fun deleteItem(itemId: String): AppResult<Unit>
}

interface ImageStore {
    suspend fun save(bytes: ByteArray, hint: String): AppResult<String>
    suspend fun load(path: String): AppResult<ByteArray>
    suspend fun delete(path: String): AppResult<Unit>
}

interface PreferencesRepository {
    val persona: Flow<Persona?>
    val onboardingDone: Flow<Boolean>
    val freeScansUsed: Flow<Int>
    val bonusScans: Flow<Int>
    suspend fun setPersona(persona: Persona?)
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setFreeScansUsed(used: Int)
    suspend fun setBonusScans(count: Int)
}

interface EntitlementRepository {
    /** True while the store reports an active Pro entitlement. */
    val isPro: Flow<Boolean>
    suspend fun refresh(): AppResult<Boolean>
    suspend fun restore(): AppResult<Boolean>
}
