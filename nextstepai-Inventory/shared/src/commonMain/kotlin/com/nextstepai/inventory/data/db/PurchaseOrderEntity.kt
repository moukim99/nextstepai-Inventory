package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان أمر الشراء (PurchaseOrderEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey
    override val uuid: String,
    val reference: String,
    val supplierId: Long,
    val supplierName: String = "",
    val statusCode: Int = 10,
    val description: String = "",
    val orderCurrency: String = "USD",
    val targetDate: String = "",
    val totalCost: Double = 0.0,
    val sourceType: String = "MANUAL",
    val sourceReferenceUuid: String? = null,
    val destinationLocationUuid: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity

/**
 * تمثيل كيان بند أمر الشراء (PurchaseOrderLineEntity) في قاعدة بيانات Room.
 */
@Entity(tableName = "purchase_order_lines")
data class PurchaseOrderLineEntity(
    @PrimaryKey
    override val uuid: String,
    val orderUuid: String,
    val supplierPartId: Long,
    val quantity: Double,
    val receivedQuantity: Double = 0.0,
    val purchasePrice: Double = 0.0,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
