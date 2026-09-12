package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان سجل التتبع والحركات (StockItemTrackingEntity) في قاعدة بيانات Room المحلية يدعم المرجع المحلي stockItemUuid.
 */
@Entity(tableName = "stock_item_tracking")
data class StockItemTrackingEntity(
    @PrimaryKey
    override val uuid: String,
    val trackingId: Long,
    val stockItemId: Long,
    val stockItemUuid: String = "",
    val date: String,
    val trackingTypeCode: Int,
    val userId: Long? = null,
    val label: String = "",
    val notes: String = "",
    val deltas: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
