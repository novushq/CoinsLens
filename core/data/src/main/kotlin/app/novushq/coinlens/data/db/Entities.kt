package app.novushq.coinlens.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import app.novushq.coinlens.model.CoinIdentification

/** [identification] is stored as one JSON column (see [Converters]) so the AI shape can grow without migrations. */
@Entity(tableName = "scans", indices = [Index("createdAt")])
data class ScanEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val obversePath: String,
    val reversePath: String?,
    val identification: CoinIdentification,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

/** Deleting a scan removes its items (CASCADE); deleting a folder leaves its items unsorted (SET NULL). */
@Entity(
    tableName = "collection_items",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("scanId"), Index("folderId")],
)
data class CollectionItemEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val folderId: String?,
    val grade: String?,
    val gradeNotes: String,
    val purchasePriceCents: Long?,
    val purchaseCurrency: String?,
    val quantity: Int,
    val addedAt: Long,
)

data class ItemWithScan(
    @Embedded val item: CollectionItemEntity,
    @Relation(parentColumn = "scanId", entityColumn = "id")
    val scan: ScanEntity,
)
