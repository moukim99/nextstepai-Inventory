package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * تمثيل كيان سجل التتبع والحركات (StockItemTrackingEntity) في قاعدة البيانات المعتمد كـ Append-only Ledger.
 */
@Entity(tableName = "stock_item_tracking")
data class StockItemTrackingEntity(
    @PrimaryKey
    val uuid: String = AppUuid.generate(),
    val stockItemUuid: String,
    val trackingTypeCode: Int = 10,
    val label: String = "",
    val notes: String = "",
    val deltas: String = "{}",
    val userUuid: String? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastModifiedByDeviceUuid: String? = null
)
