package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.identify.IdentifyEngine
import com.novushq.coinlens.identify.IdentifySpec
import com.novushq.coinlens.identify.ImageInput
import com.novushq.coinlens.model.CollectionItem
import com.novushq.coinlens.model.CollectionSummary
import com.novushq.coinlens.model.Folder
import com.novushq.coinlens.model.FolderDetail
import com.novushq.coinlens.model.Persona
import com.novushq.coinlens.model.ScanRecord
import com.novushq.coinlens.model.ValuedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
class FakeScanRepository : ScanRepository {
    val records = mutableMapOf<String, ScanRecord>()
    var failSave = false
    override fun observeRecent(limit: Int): Flow<List<ScanRecord>> = MutableStateFlow(records.values.sortedByDescending { it.createdAt }.take(limit))
    override fun observe(id: String): Flow<ScanRecord?> = MutableStateFlow(records[id])
    override suspend fun get(id: String): AppResult<ScanRecord> =
        records[id]?.let { AppResult.Success(it) } ?: AppResult.Failure(com.novushq.coinlens.common.AppError.NotFound())
    override suspend fun save(record: ScanRecord): AppResult<String> {
        if (failSave) return AppResult.Failure(com.novushq.coinlens.common.AppError.Storage())
        records[record.id] = record
        return AppResult.Success(record.id)
    }
    override suspend fun delete(id: String): AppResult<Unit> {
        records.remove(id)
        return AppResult.Success(Unit)
    }
}

class FakeImageStore : ImageStore {
    val saved = mutableListOf<ByteArray>()
    var fail = false
    override suspend fun save(bytes: ByteArray, hint: String): AppResult<String> {
        if (fail) return AppResult.Failure(com.novushq.coinlens.common.AppError.Storage())
        saved += bytes
        return AppResult.Success("mem://${saved.size - 1}")
    }
    override suspend fun load(path: String): AppResult<ByteArray> =
        AppResult.Success(saved.getOrElse(path.removePrefix("mem://").toInt()) { ByteArray(0) })
    override suspend fun delete(path: String): AppResult<Unit> = AppResult.Success(Unit)
}

class FakePreferences(
    used: Int = 0,
    bonus: Int = 0,
    done: Boolean = false,
    persona: Persona? = null,
) : PreferencesRepository {
    private val _persona = MutableStateFlow(persona)
    private val _done = MutableStateFlow(done)
    private val _used = MutableStateFlow(used)
    private val _bonus = MutableStateFlow(bonus)
    override val persona: Flow<Persona?> = _persona
    override val onboardingDone: Flow<Boolean> = _done
    override val freeScansUsed: Flow<Int> = _used
    override val bonusScans: Flow<Int> = _bonus
    override suspend fun setPersona(persona: Persona?) { _persona.value = persona }
    override suspend fun setOnboardingDone(done: Boolean) { _done.value = done }
    override suspend fun setFreeScansUsed(used: Int) { _used.value = used }
    override suspend fun setBonusScans(count: Int) { _bonus.value = count }
}

class FakeEntitlements(var pro: Boolean = false) : EntitlementRepository {
    private val _isPro = MutableStateFlow(pro)
    override val isPro: Flow<Boolean> = _isPro
    fun setPro(value: Boolean) { _isPro.value = value }
    override suspend fun refresh(): AppResult<Boolean> = AppResult.Success(_isPro.value)
    override suspend fun restore(): AppResult<Boolean> = AppResult.Success(_isPro.value)
}

class FakeEngine(val result: AppResult<com.novushq.coinlens.model.CoinIdentification>) : IdentifyEngine {
    var calls = 0
    override suspend fun <T> identify(spec: IdentifySpec<T>, images: List<ImageInput>): AppResult<T> {
        calls++
        @Suppress("UNCHECKED_CAST")
        return result as AppResult<T>
    }
}

class FakeCollectionRepository : CollectionRepository {
    val folders = mutableMapOf<String, Folder>()
    val items = mutableMapOf<String, CollectionItem>()
    override fun observeFolders(): Flow<List<Folder>> = MutableStateFlow(folders.values.toList())
    override fun observeFolder(folderId: String): Flow<FolderDetail?> = MutableStateFlow(null)
    override fun observeItems(folderId: String?): Flow<List<ValuedItem>> = MutableStateFlow(emptyList())
    override fun observeItem(itemId: String): Flow<ValuedItem?> = MutableStateFlow(null)
    override fun observeSummary(): Flow<CollectionSummary> =
        MutableStateFlow(ComputeCollectionValueUseCase().invoke(items.values.map { ValuedItem(it, null) }))
    override suspend fun createFolder(name: String): AppResult<String> {
        if (name.isBlank()) return AppResult.Failure(com.novushq.coinlens.common.AppError.Validation("name", "blank"))
        val id = "f${folders.size}"
        folders[id] = Folder(id, name, 0)
        return AppResult.Success(id)
    }
    override suspend fun renameFolder(folderId: String, name: String): AppResult<Unit> {
        folders[folderId] = (folders[folderId] ?: return AppResult.Failure(com.novushq.coinlens.common.AppError.NotFound())).copy(name = name)
        return AppResult.Success(Unit)
    }
    override suspend fun deleteFolder(folderId: String): AppResult<Unit> {
        folders.remove(folderId)
        items.replaceAll { _, v -> if (v.folderId == folderId) v.copy(folderId = null) else v }
        return AppResult.Success(Unit)
    }
    override suspend fun addItem(item: CollectionItem): AppResult<String> {
        items[item.id] = item
        return AppResult.Success(item.id)
    }
    override suspend fun updateItem(item: CollectionItem): AppResult<Unit> {
        items[item.id] = item
        return AppResult.Success(Unit)
    }
    override suspend fun deleteItem(itemId: String): AppResult<Unit> {
        items.remove(itemId)
        return AppResult.Success(Unit)
    }
}


