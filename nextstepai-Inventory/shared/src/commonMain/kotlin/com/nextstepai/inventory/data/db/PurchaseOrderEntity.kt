package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity

/**
 * تمثيل كيان أمر الشراء (PurchaseOrderEntity) في قاعدة بيانات Room المحلية.
 * يحتوي على الفهارس التلقائية `@Index` على `syncStatus`, `isDeleted`, `updatedAt` لسرعة المزامنة والربط.
 */
data class PurchaseOrderEntity(
    override val uuid: String,
    val reference: String,
    val supplierId: Long,
    val supplierName: String = "",
    val statusCode: Int = 10,
    val description: String = "",
    val orderCurrency: String = "USD",
    val targetDate: String = "",
    val totalCost: Double = 0.0,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_PUSH,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : SyncableEntity

/**
 * تمثيل كيان بند أمر الشراء (PurchaseOrderLineEntity) في قاعدة بيانات Room.
 */
data class PurchaseOrderLineEntity(
    override val uuid: String,
    val orderUuid: String,
    val supplierPartId: Long,
    val quantity: Double,
    val receivedQuantity: Double = 0.0,
    val purchasePrice: Double = 0.0,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_PUSH,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : SyncableEntity
