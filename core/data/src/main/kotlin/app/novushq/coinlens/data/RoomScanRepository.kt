package app.novushq.coinlens.data

import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.flatMap
import app.novushq.coinlens.common.safeCall
import app.novushq.coinlens.data.db.ScanDao
import app.novushq.coinlens.data.db.toDomain
import app.novushq.coinlens.data.db.toEntity
import app.novushq.coinlens.domain.ImageStore
import app.novushq.coinlens.domain.ScanRepository
import app.novushq.coinlens.model.ScanRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RoomScanRepository(
    private val dao: ScanDao,
    private val images: ImageStore,
    private val dispatchers: DispatcherProvider,
) : ScanRepository {

    override fun observeRecent(limit: Int): Flow<AppResult<List<ScanRecord>>> =
        dao.observeRecent(limit)
            .map<_, AppResult<List<ScanRecord>>> { rows -> AppResult.Success(rows.map { it.toDomain() }) }
            .catch { emit(AppResult.Failure(storageError(it))) }
            .flowOn(dispatchers.default)

    override fun observe(id: String): Flow<AppResult<ScanRecord>> =
        dao.observe(id)
            .map { row -> row?.let { AppResult.Success(it.toDomain()) } ?: AppResult.Failure(AppError.NotFound("Scan not found")) }
            .catch { emit(AppResult.Failure(storageError(it))) }
            .flowOn(dispatchers.default)

    override suspend fun get(id: String): AppResult<ScanRecord> =
        safeCall(::storageError) { dao.get(id) }.flatMap { row ->
            row?.let { AppResult.Success(it.toDomain()) } ?: AppResult.Failure(AppError.NotFound("Scan not found"))
        }

    override suspend fun save(record: ScanRecord): AppResult<String> =
        safeCall(::storageError) {
            dao.upsert(record.toEntity())
            record.id
        }

    override suspend fun delete(id: String): AppResult<Unit> = get(id).flatMap { record ->
        safeCall(::storageError) { dao.delete(id) }.flatMap {
            listOfNotNull(record.obversePath, record.reversePath).forEach { images.delete(it) }
            AppResult.Success(Unit)
        }
    }
}

internal fun storageError(e: Throwable): AppError = AppError.Storage(e.message ?: "Storage failure", e)
