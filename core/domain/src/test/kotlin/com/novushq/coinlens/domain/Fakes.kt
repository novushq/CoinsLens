package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppError
import com.novushq.coinlens.common.AppResult
import com.novushq.coinlens.identify.IdentifyEngine
import com.novushq.coinlens.identify.IdentifySpec
import com.novushq.coinlens.identify.ImageInput
import com.novushq.coinlens.identify.ImageRole
import com.novushq.coinlens.model.CoinIdentification
import com.novushq.coinlens.model.CollectionItem
import com.novushq.coinlens.model.CollectionSummary
import com.novushq.coinlens.model.Confidence
import com.novushq.coinlens.model.Folder
import com.novushq.coinlens.model.FolderDetail
import com.novushq.coinlens.model.ItemKind
import com.novushq.coinlens.model.Persona
import com.novushq.coinlens.model.ScanRecord
import com.novushq.coinlens.model.ValueEstimate
import com.novushq.coinlens.model.ValueRange
import com.novushq.coinlens.model.ValuedItem
import com.novushq.coinlens.model.usd
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

val front = ImageInput(byteArrayOf(1, 2, 3), role = ImageRole.PRIMARY)
val back = ImageInput(byteArrayOf(4, 5, 6), role = ImageRole.SECONDARY)

fun coin(
    circulated: ValueRange? = ValueRange(usd(1.0), usd(3.0)),
    uncirculated: ValueRange? = ValueRange(usd(10.0), usd(20.0)),
    recognized: Boolean = true,
) = CoinIdentification(
    kind = ItemKind.COIN,
    name = "Test cent",
    country = "USA",
    denomination = "1 cent",
    value = ValueEstimate(circulated, uncirculated, Confidence.MEDIUM),
    recognized = recognized,
)

class FakeImageStore : ImageStore {
    val files = mutableMapOf<String, ByteArray>()
    var failOnSave = 0 // 1-based index of the save call that fails; 0 = never
    private var calls = 0
    override suspend fun save(bytes: ByteArray, nameHint: String): AppResult<String> {
        calls++
        if (calls == failOnSave) return AppResult.Failure(AppError.Storage())
        val path = "/files/$nameHint-$calls.jpg"
        files[path] = bytes
        return AppResult.Success(path)
    }
    override suspend fun load(path: String): AppResult<ByteArray> =
        files[path]?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound())
    override suspend fun delete(path: String): AppResult<Unit> {
        files.remove(path)
        return AppResult.Success(Unit)
    }
}

class FakeScanRepository : ScanRepository {
    val records = mutableMapOf<String, ScanRecord>()
    var failSave = false
    override fun observeRecent(limit: Int): Flow<AppResult<List<ScanRecord>>> =
        flowOf(AppResult.Success(records.values.sortedByDescending { it.createdAt }.take(limit)))
    override fun observe(id: String): Flow<AppResult<ScanRecord>> = flowOf(getNow(id))
    override suspend fun get(id: String): AppResult<ScanRecord> = getNow(id)
    private fun getNow(id: String): AppResult<ScanRecord> =
        records[id]?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound())
    override suspend fun save(record: ScanRecord): AppResult<String> {
        if (failSave) return AppResult.Failure(AppError.Storage())
        records[record.id] = record
        return AppResult.Success(record.id)
    }
    override suspend fun delete(id: String): AppResult<Unit> {
        records.remove(id)
        return AppResult.Success(Unit)
    }
}

class FakePreferences(used: Int = 0, bonus: Int = 0) : PreferencesRepository {
    val personaState = MutableStateFlow<Persona?>(null)
    val doneState = MutableStateFlow(false)
    val usedState = MutableStateFlow(used)
    val bonusState = MutableStateFlow(bonus)
    override val persona: Flow<Persona?> = personaState
    override val onboardingDone: Flow<Boolean> = doneState
    override val freeScansUsed: Flow<Int> = usedState
    override val bonusScans: Flow<Int> = bonusState
    override suspend fun setPersona(persona: Persona?) = AppResult.Success(Unit).also { personaState.value = persona }
    override suspend fun setOnboardingDone(done: Boolean) = AppResult.Success(Unit).also { doneState.value = done }
    override suspend fun setFreeScansUsed(used: Int) = AppResult.Success(Unit).also { usedState.value = used }
    override suspend fun setBonusScans(count: Int) = AppResult.Success(Unit).also { bonusState.value = count }
}

class FakeEntitlements(pro: Boolean = false) : EntitlementRepository {
    val state = MutableStateFlow(pro)
    override val isPro: Flow<Boolean> = state
    override suspend fun refresh(): AppResult<Boolean> = AppResult.Success(state.value)
    override suspend fun restore(): AppResult<Boolean> = AppResult.Success(state.value)
}

class FakeEngine(var result: AppResult<CoinIdentification>) : IdentifyEngine {
    var calls = 0
    var lastImages: List<ImageInput> = emptyList()
    override suspend fun <T> identify(spec: IdentifySpec<T>, images: List<ImageInput>): AppResult<T> {
        calls++
        lastImages = images
        @Suppress("UNCHECKED_CAST")
        return result as AppResult<T>
    }
}

class FakeCollectionRepository : CollectionRepository {
    val items = mutableMapOf<String, CollectionItem>()
    override fun observeFolders(): Flow<AppResult<List<Folder>>> = flowOf(AppResult.Success(emptyList()))
    override fun observeFolder(folderId: String): Flow<AppResult<FolderDetail>> = flowOf(AppResult.Failure(AppError.NotFound()))
    override fun observeItems(): Flow<AppResult<List<ValuedItem>>> = flowOf(AppResult.Success(emptyList()))
    override fun observeItem(itemId: String): Flow<AppResult<ValuedItem>> = flowOf(AppResult.Failure(AppError.NotFound()))
    override fun observeSummary(): Flow<AppResult<CollectionSummary>> = flowOf(AppResult.Success(CollectionSummary()))
    override suspend fun createFolder(name: String): AppResult<String> = AppResult.Success("f1")
    override suspend fun renameFolder(folderId: String, name: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteFolder(folderId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun addItem(item: CollectionItem): AppResult<String> {
        items[item.id] = item
        return AppResult.Success(item.id)
    }
    override suspend fun updateItem(item: CollectionItem): AppResult<Unit> = AppResult.Success(Unit).also { items[item.id] = item }
    override suspend fun deleteItem(itemId: String): AppResult<Unit> = AppResult.Success(Unit).also { items.remove(itemId) }
}
