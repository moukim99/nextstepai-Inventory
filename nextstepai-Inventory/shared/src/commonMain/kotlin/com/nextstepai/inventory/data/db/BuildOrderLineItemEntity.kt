package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان بند ومخرجات أمر التصنيع (BuildOrderLineItemEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "build_order_line_items")
data class BuildOrderLineItemEntity(
    @PrimaryKey
    override val uuid: String,
    val id: Long = 0L,
    val buildId: Long,
    val bomItemId: Long,
    val subPartId: Long = 0L,
    val subPartName: String = "",
    val quantity: Double = 1.0,
    val allocatedQuantity: Double = 0.0,
    val consumedQuantity: Double = 0.0,
    val notes: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
