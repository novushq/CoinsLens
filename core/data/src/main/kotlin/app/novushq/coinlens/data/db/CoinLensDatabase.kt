package app.novushq.coinlens.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import app.novushq.coinlens.model.CoinIdentification
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

@Database(
    entities = [ScanEntity::class, FolderEntity::class, CollectionItemEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CoinLensDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun folderDao(): FolderDao
    abstract fun itemDao(): CollectionItemDao

    companion object {
        private const val NAME = "coinlens.db"

        // Never fallbackToDestructiveMigration: add a Migration plus a schema test (DATA-001).
        fun build(context: Context): CoinLensDatabase =
            Room.databaseBuilder(context, CoinLensDatabase::class.java, NAME).build()
    }
}

class Converters {
    @TypeConverter
    fun identificationToJson(value: CoinIdentification): String = json.encodeToString(CoinIdentification.serializer(), value)

    @TypeConverter
    fun jsonToIdentification(value: String): CoinIdentification = json.decodeFromString(CoinIdentification.serializer(), value)

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    fun observe(id: String): Flow<ScanEntity?>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun get(id: String): ScanEntity?

    @Upsert
    suspend fun upsert(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: String): Int
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun observe(id: String): Flow<FolderEntity?>

    @Insert
    suspend fun insert(folder: FolderEntity)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String): Int

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String): Int
}

@Dao
interface CollectionItemDao {
    @Transaction
    @Query("SELECT * FROM collection_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ItemWithScan>>

    @Transaction
    @Query("SELECT * FROM collection_items WHERE folderId = :folderId ORDER BY addedAt DESC")
    fun observeByFolder(folderId: String): Flow<List<ItemWithScan>>

    @Transaction
    @Query("SELECT * FROM collection_items WHERE id = :id")
    fun observe(id: String): Flow<ItemWithScan?>

    @Insert
    suspend fun insert(item: CollectionItemEntity)

    @Update
    suspend fun update(item: CollectionItemEntity): Int

    @Query("DELETE FROM collection_items WHERE id = :id")
    suspend fun delete(id: String): Int
}
