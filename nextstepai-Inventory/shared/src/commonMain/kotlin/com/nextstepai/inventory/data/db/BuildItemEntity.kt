package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان تخصيص المخزون لأمر التصنيع (BuildItemEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "build_items")
data class BuildItemEntity(
    @PrimaryKey
    override val uuid: String,
    val id: Long = 0L,
    val buildId: Long,
    val buildLineId: Long? = null,
    val stockItemId: Long,
    val stockItemName: String = "",
    val quantity: Double = 1.0,
    val installIntoStockItemId: Long? = null,
    val notes: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
