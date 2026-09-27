package app.novushq.coinlens.data

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.flatMap
import app.novushq.coinlens.common.safeCall
import app.novushq.coinlens.data.db.CollectionItemDao
import app.novushq.coinlens.data.db.FolderDao
import app.novushq.coinlens.data.db.FolderEntity
import app.novushq.coinlens.data.db.toDomain
import app.novushq.coinlens.data.db.toEntity
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.domain.ComputeCollectionValueUseCase
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.CollectionSummary
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.FolderDetail
import app.novushq.coinlens.model.ValuedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

/** Folder counts and totals are derived on read with [ComputeCollectionValueUseCase], never stored. */
class RoomCollectionRepository(
    private val folders: FolderDao,
    private val items: CollectionItemDao,
    private val computeValue: ComputeCollectionValueUseCase,
    private val dispatchers: DispatcherProvider,
    private val clock: () -> Instant = Instant::now,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : CollectionRepository {

    private val valuedItems: Flow<List<ValuedItem>> = items.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeFolders(): Flow<AppResult<List<Folder>>> =
        combine(folders.observeAll(), valuedItems) { folderRows, all ->
            val byFolder = all.groupBy { it.item.folderId }
            AppResult.Success(folderRows.map { it.withTotals(byFolder[it.id].orEmpty()) }) as AppResult<List<Folder>>
        }.guarded()

    override fun observeFolder(folderId: String): Flow<AppResult<FolderDetail>> =
        combine(folders.observe(folderId), items.observeByFolder(folderId)) { folder, rows ->
            if (folder == null) {
                AppResult.Failure(AppError.NotFound("Folder not found"))
            } else {
                val valued = rows.map { it.toDomain() }
                AppResult.Success(FolderDetail(folder.withTotals(valued), valued))
            }
        }.guarded()

    override fun observeItems(): Flow<AppResult<List<ValuedItem>>> =
        valuedItems.map<_, AppResult<List<ValuedItem>>> { AppResult.Success(it) }.guarded()

    override fun observeItem(itemId: String): Flow<AppResult<ValuedItem>> =
        items.observe(itemId).map { row ->
            row?.let { AppResult.Success(it.toDomain()) } ?: AppResult.Failure(AppError.NotFound("Item not found"))
        }.guarded()

    override fun observeSummary(): Flow<AppResult<CollectionSummary>> =
        valuedItems.map<_, AppResult<CollectionSummary>> { AppResult.Success(computeValue(it)) }.guarded()

    override suspend fun createFolder(name: String): AppResult<String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AppResult.Failure(AppError.Validation("name", "Folder name is required"))
        val id = newId()
        return safeCall(::storageError) {
            folders.insert(FolderEntity(id = id, name = trimmed, createdAt = clock().toEpochMilli()))
            id
        }
    }

    override suspend fun renameFolder(folderId: String, name: String): AppResult<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AppResult.Failure(AppError.Validation("name", "Folder name is required"))
        return safeCall(::storageError) { folders.rename(folderId, trimmed) }.requireRow("Folder not found")
    }

    override suspend fun deleteFolder(folderId: String): AppResult<Unit> =
        safeCall(::storageError) { folders.delete(folderId) }.requireRow("Folder not found")

    override suspend fun addItem(item: CollectionItem): AppResult<String> =
        safeCall(::storageError) {
            items.insert(item.toEntity())
            item.id
        }

    override suspend fun updateItem(item: CollectionItem): AppResult<Unit> =
        safeCall(::storageError) { items.update(item.toEntity()) }.requireRow("Item not found")

    override suspend fun deleteItem(itemId: String): AppResult<Unit> =
        safeCall(::storageError) { items.delete(itemId) }.requireRow("Item not found")

    private fun FolderEntity.withTotals(folderItems: List<ValuedItem>): Folder {
        val summary = computeValue(folderItems)
        return toDomain(itemCount = summary.itemCount, totalValue = summary.totalMid)
    }

    private fun AppResult<Int>.requireRow(notFound: String): AppResult<Unit> = flatMap { rows ->
        if (rows > 0) AppResult.Success(Unit) else AppResult.Failure(AppError.NotFound(notFound))
    }

    private fun <T> Flow<AppResult<T>>.guarded(): Flow<AppResult<T>> =
        catch { emit(AppResult.Failure(storageError(it))) }.flowOn(dispatchers.default)
}
