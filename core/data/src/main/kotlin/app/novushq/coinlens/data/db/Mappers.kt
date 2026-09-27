package app.novushq.coinlens.data.db

import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.ValuedItem
import java.time.Instant

internal fun ScanEntity.toDomain() = ScanRecord(
    id = id,
    createdAt = Instant.ofEpochMilli(createdAt),
    obversePath = obversePath,
    reversePath = reversePath,
    identification = identification,
)

internal fun ScanRecord.toEntity() = ScanEntity(
    id = id,
    createdAt = createdAt.toEpochMilli(),
    obversePath = obversePath,
    reversePath = reversePath,
    identification = identification,
)

internal fun FolderEntity.toDomain(itemCount: Int = 0, totalValue: Money = Money.ZERO) = Folder(
    id = id,
    name = name,
    createdAt = Instant.ofEpochMilli(createdAt),
    itemCount = itemCount,
    totalValue = totalValue,
)

internal fun CollectionItemEntity.toDomain() = CollectionItem(
    id = id,
    scanId = scanId,
    folderId = folderId,
    grade = Grade.entries.firstOrNull { it.name == grade },
    gradeNotes = gradeNotes,
    purchasePrice = purchasePriceCents?.let { Money(it, purchaseCurrency ?: Money.USD) },
    quantity = quantity.coerceAtLeast(1),
    addedAt = Instant.ofEpochMilli(addedAt),
)

internal fun CollectionItem.toEntity() = CollectionItemEntity(
    id = id,
    scanId = scanId,
    folderId = folderId,
    grade = grade?.name,
    gradeNotes = gradeNotes,
    purchasePriceCents = purchasePrice?.cents,
    purchaseCurrency = purchasePrice?.currency,
    quantity = quantity,
    addedAt = addedAt.toEpochMilli(),
)

internal fun ItemWithScan.toDomain() = ValuedItem(item = item.toDomain(), scan = scan.toDomain())
