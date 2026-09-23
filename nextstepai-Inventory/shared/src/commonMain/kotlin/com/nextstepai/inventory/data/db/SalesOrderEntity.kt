package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان أمر البيع والطلب المخصص للعميل (SalesOrderEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "sales_orders")
data class SalesOrderEntity(
    @PrimaryKey
    override val uuid: String,
    val reference: String,
    val customerId: Long = 0L,
    val customerUuid: String = "",
    val customerName: String = "",
    val statusCode: Int = 10,
    val description: String = "",
    val orderCurrency: String = "USD",
    val targetDate: String = "",
    val totalPrice: Double = 0.0,
    val sourceType: String = "MANUAL",
    val sourceReferenceUuid: String? = null,
    val notes: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity

/**
 * تمثيل كيان بند أمر البيع (SalesOrderLineEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "sales_order_lines")
data class SalesOrderLineEntity(
    @PrimaryKey
    override val uuid: String,
    val orderUuid: String,
    val orderId: Long = 0L,
    val partId: Long = 0L,
    val partUuid: String = "",
    val partName: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val allocatedQuantity: Double = 0.0,
    val shippedQuantity: Double = 0.0,
    val notes: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
