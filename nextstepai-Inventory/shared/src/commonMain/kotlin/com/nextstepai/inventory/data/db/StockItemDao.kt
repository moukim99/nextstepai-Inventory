package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات المخزون الفعلي (StockItemDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockItemDao {

    private val selectColumns = """
        uuid, partId, locationId, locationUuid, quantity, serial, batch, statusCode, packaging,
        purchasePrice, purchasePriceCurrency, purchaseOrderId, supplierPartId,
        salesOrderId, customerId, buildId, isBuilding, parentId, parentUuid, expiryDate,
        stocktakeDate, stocktakeUserId, reviewNeeded, deleteOnDeplete, link,
        notes, metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getStockItemsPaged(
        partId: Long? = null,
        locationId: Long? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StockItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_items
            WHERE isDeleted = 0
              AND (? IS NULL OR partId = ?)
              AND (? IS NULL OR locationId = ?)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (partId != null) stmt.bindLong(1, partId) else stmt.bindNull(1)
            if (partId != null) stmt.bindLong(2, partId) else stmt.bindNull(2)
            if (locationId != null) stmt.bindLong(3, locationId) else stmt.bindNull(3)
            if (locationId != null) stmt.bindLong(4, locationId) else stmt.bindNull(4)
            stmt.bindLong(5, limit.toLong())
            stmt.bindLong(6, offset.toLong())
            while (stmt.step()) {
                results.add(mapStockItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun getPendingSyncStockItems(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<StockItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemEntity>()
        conn.prepare("""
            SELECT $selectColumns
            FROM stock_items
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapStockItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: StockItemEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_items (
                uuid, partId, locationId, locationUuid, quantity, serial, batch, statusCode, packaging,
                purchasePrice, purchasePriceCurrency, purchaseOrderId, supplierPartId,
                salesOrderId, customerId, buildId, isBuilding, parentId, parentUuid, expiryDate,
                stocktakeDate, stocktakeUserId, reviewNeeded, deleteOnDeplete, link,
                notes, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.partId)
            if (entity.locationId != null) stmt.bindLong(3, entity.locationId) else stmt.bindNull(3)
            if (entity.locationUuid != null) stmt.bindText(4, entity.locationUuid) else stmt.bindNull(4)
            stmt.bindDouble(5, entity.quantity)
            stmt.bindText(6, entity.serial)
            stmt.bindText(7, entity.batch)
            stmt.bindLong(8, entity.statusCode.toLong())
            stmt.bindText(9, entity.packaging)
            stmt.bindDouble(10, entity.purchasePrice)
            stmt.bindText(11, entity.purchasePriceCurrency)
            if (entity.purchaseOrderId != null) stmt.bindLong(12, entity.purchaseOrderId) else stmt.bindNull(12)
            if (entity.supplierPartId != null) stmt.bindLong(13, entity.supplierPartId) else stmt.bindNull(13)
            if (entity.salesOrderId != null) stmt.bindLong(14, entity.salesOrderId) else stmt.bindNull(14)
            if (entity.customerId != null) stmt.bindLong(15, entity.customerId) else stmt.bindNull(15)
            if (entity.buildId != null) stmt.bindLong(16, entity.buildId) else stmt.bindNull(16)
            stmt.bindLong(17, if (entity.isBuilding) 1L else 0L)
            if (entity.parentId != null) stmt.bindLong(18, entity.parentId) else stmt.bindNull(18)
            if (entity.parentUuid != null) stmt.bindText(19, entity.parentUuid) else stmt.bindNull(19)
            stmt.bindText(20, entity.expiryDate)
            stmt.bindText(21, entity.stocktakeDate)
            if (entity.stocktakeUserId != null) stmt.bindLong(22, entity.stocktakeUserId) else stmt.bindNull(22)
            stmt.bindLong(23, if (entity.reviewNeeded) 1L else 0L)
            stmt.bindLong(24, if (entity.deleteOnDeplete) 1L else 0L)
            stmt.bindText(25, entity.link)
            stmt.bindText(26, entity.notes)
            stmt.bindText(27, entity.metadata)
            stmt.bindText(28, entity.syncStatus.name)
            stmt.bindLong(29, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(30, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun insertStockItemForPO(partId: Long, qty: Double, purchasePrice: Double, currency: String, batch: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val uuid = "stock-po-${Clock.System.now().toEpochMilliseconds()}"
        conn.prepare("""
            INSERT INTO stock_items (
                uuid, partId, quantity, purchasePrice, purchasePriceCurrency, batch, statusCode, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, 10, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindLong(2, partId)
            stmt.bindDouble(3, qty)
            stmt.bindDouble(4, purchasePrice)
            stmt.bindText(5, currency)
            stmt.bindText(6, batch)
            stmt.bindLong(7, Clock.System.now().toEpochMilliseconds())
            stmt.step()
        }
    }

    suspend fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE stock_items SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    private fun mapStockItemEntity(stmt: SQLiteStatement): StockItemEntity {
        return StockItemEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            partId = runCatching { stmt.getLong(1) }.getOrDefault(0L),
            locationId = runCatching { if (stmt.isNull(2)) null else stmt.getLong(2) }.getOrNull(),
            locationUuid = runCatching { if (stmt.isNull(3)) null else stmt.getText(3) }.getOrNull(),
            quantity = runCatching { stmt.getDouble(4) }.getOrDefault(1.0),
            serial = runCatching { stmt.getText(5) }.getOrDefault(""),
            batch = runCatching { stmt.getText(6) }.getOrDefault(""),
            statusCode = runCatching { stmt.getLong(7).toInt() }.getOrDefault(10),
            packaging = runCatching { stmt.getText(8) }.getOrDefault("Box"),
            purchasePrice = runCatching { stmt.getDouble(9) }.getOrDefault(0.0),
            purchasePriceCurrency = runCatching { stmt.getText(10) }.getOrDefault("USD"),
            purchaseOrderId = runCatching { if (stmt.isNull(11)) null else stmt.getLong(11) }.getOrNull(),
            supplierPartId = runCatching { if (stmt.isNull(12)) null else stmt.getLong(12) }.getOrNull(),
            salesOrderId = runCatching { if (stmt.isNull(13)) null else stmt.getLong(13) }.getOrNull(),
            customerId = runCatching { if (stmt.isNull(14)) null else stmt.getLong(14) }.getOrNull(),
            buildId = runCatching { if (stmt.isNull(15)) null else stmt.getLong(15) }.getOrNull(),
            isBuilding = runCatching { stmt.getLong(16) != 0L }.getOrDefault(false),
            parentId = runCatching { if (stmt.isNull(17)) null else stmt.getLong(17) }.getOrNull(),
            parentUuid = runCatching { if (stmt.isNull(18)) null else stmt.getText(18) }.getOrNull(),
            expiryDate = runCatching { stmt.getText(19) }.getOrDefault(""),
            stocktakeDate = runCatching { stmt.getText(20) }.getOrDefault(""),
            stocktakeUserId = runCatching { if (stmt.isNull(21)) null else stmt.getLong(21) }.getOrNull(),
            reviewNeeded = runCatching { stmt.getLong(22) != 0L }.getOrDefault(false),
            deleteOnDeplete = runCatching { stmt.getLong(23) != 0L }.getOrDefault(false),
            link = runCatching { stmt.getText(24) }.getOrDefault(""),
            notes = runCatching { stmt.getText(25) }.getOrDefault(""),
            metadata = runCatching { stmt.getText(26) }.getOrDefault("{}"),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(27)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(28) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(29) }.getOrDefault(0L)
        )
    }
}
