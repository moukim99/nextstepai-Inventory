package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان الوحدة المخزنية (StockItemEntity) المادية في قاعدة بيانات Room المحلية محتوياً على الـ 24 حقل الكاملة بدعم المفاتيح المرجعية المحلية (parentUuid).
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
    val purchasePrice: Double = 0.0,
    val purchasePriceCurrency: String = "USD",
    val purchaseOrderId: Long? = null,
    val supplierPartId: Long? = null,
    val salesOrderId: Long? = null,
    val customerId: Long? = null,
    val buildId: Long? = null,
    val isBuilding: Boolean = false,
    val parentId: Long? = null,
    val parentUuid: String? = null,
    val expiryDate: String = "",
    val stocktakeDate: String = "",
    val stocktakeUserId: Long? = null,
    val reviewNeeded: Boolean = false,
    val deleteOnDeplete: Boolean = false,
    val link: String = "",
    val notes: String = "",
    val metadata: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
