package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * تمثيل كيان الوحدة المخزنية (StockItemEntity) المعتمدة بنسبة 100% على UUIDv7.
 */
@Entity(tableName = "stock_items")
data class StockItemEntity(
    @PrimaryKey
    override val uuid: String = AppUuid.generate(),
    val partUuid: String,
    val locationUuid: String? = null,
    val quantity: Double = 1.0,
    val serial: String = "",
    val batch: String = "",
    val statusCode: Int = 10,
    val packaging: String = "Box",
    val expiryDate: String = "",
    val notes: String = "",
    val purchasePrice: Double = 0.0,
    val purchasePriceCurrency: String = "USD",
    val purchaseOrderUuid: String? = null,
    val supplierPartUuid: String = "",
    val salesOrderUuid: String? = null,
    val customerUuid: String = "",
    val buildUuid: String? = null,
    val isBuilding: Boolean = false,
    val parentStockItemUuid: String? = null,
    val stocktakeDate: String = "",
    val stocktakeUserUuid: String? = null,
    val reviewNeeded: Boolean = false,
    val deleteOnDeplete: Boolean = false,
    val link: String = "",
    val metadata: String = "{}",
    val version: Int = 1,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastModifiedByDeviceUuid: String? = null
) : SyncableEntity
