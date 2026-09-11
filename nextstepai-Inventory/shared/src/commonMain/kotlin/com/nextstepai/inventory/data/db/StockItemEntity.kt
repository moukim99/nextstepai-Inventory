package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان الوحدة المخزنية (StockItemEntity) المادية في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "stock_items")
data class StockItemEntity(
    @PrimaryKey
    override val uuid: String,
    val partId: Long,
    val locationId: Long? = null,
    val quantity: Double = 1.0,
    val serial: String = "",
    val batch: String = "",
    val statusCode: Int = 10,
    val packaging: String = "Box",
    val expiryDate: String = "",
    val notes: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
