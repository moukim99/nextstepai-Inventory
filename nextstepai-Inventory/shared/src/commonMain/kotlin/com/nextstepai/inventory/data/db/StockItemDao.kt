package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات المخزون الفعلي (StockItemDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockItemDao {

    private val selectColumns = """
        uuid, partId, locationId, quantity, serial, batch, statusCode, packaging,
        purchasePrice, purchasePriceCurrency, purchaseOrderId, supplierPartId,
        salesOrderId, customerId, buildId, isBuilding, parentId, expiryDate,
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
                uuid, partId, locationId, quantity, serial, batch, statusCode, packaging,
                purchasePrice, purchasePriceCurrency, purchaseOrderId, supplierPartId,
                salesOrderId, customerId, buildId, isBuilding, parentId, expiryDate,
                stocktakeDate, stocktakeUserId, reviewNeeded, deleteOnDeplete, link,
                notes, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.partId)
            if (entity.locationId != null) stmt.bindLong(3, entity.locationId) else stmt.bindNull(3)
            stmt.bindDouble(4, entity.quantity)
            stmt.bindText(5, entity.serial)
            stmt.bindText(6, entity.batch)
            stmt.bindLong(7, entity.statusCode.toLong())
            stmt.bindText(8, entity.packaging)
            stmt.bindDouble(9, entity.purchasePrice)
            stmt.bindText(10, entity.purchasePriceCurrency)
            if (entity.purchaseOrderId != null) stmt.bindLong(11, entity.purchaseOrderId) else stmt.bindNull(11)
            if (entity.supplierPartId != null) stmt.bindLong(12, entity.supplierPartId) else stmt.bindNull(12)
            if (entity.salesOrderId != null) stmt.bindLong(13, entity.salesOrderId) else stmt.bindNull(13)
            if (entity.customerId != null) stmt.bindLong(14, entity.customerId) else stmt.bindNull(14)
            if (entity.buildId != null) stmt.bindLong(15, entity.buildId) else stmt.bindNull(15)
            stmt.bindLong(16, if (entity.isBuilding) 1L else 0L)
            if (entity.parentId != null) stmt.bindLong(17, entity.parentId) else stmt.bindNull(17)
            stmt.bindText(18, entity.expiryDate)
            stmt.bindText(19, entity.stocktakeDate)
            if (entity.stocktakeUserId != null) stmt.bindLong(20, entity.stocktakeUserId) else stmt.bindNull(20)
            stmt.bindLong(21, if (entity.reviewNeeded) 1L else 0L)
            stmt.bindLong(22, if (entity.deleteOnDeplete) 1L else 0L)
            stmt.bindText(23, entity.link)
            stmt.bindText(24, entity.notes)
            stmt.bindText(25, entity.metadata)
            stmt.bindText(26, entity.syncStatus.name)
            stmt.bindLong(27, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(28, entity.updatedAt)
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
            uuid = stmt.getText(0),
            partId = stmt.getLong(1),
            locationId = if (stmt.isNull(2)) null else stmt.getLong(2),
            quantity = stmt.getDouble(3),
            serial = stmt.getText(4),
            batch = stmt.getText(5),
            statusCode = stmt.getLong(6).toInt(),
            packaging = stmt.getText(7),
            purchasePrice = stmt.getDouble(8),
            purchasePriceCurrency = stmt.getText(9),
            purchaseOrderId = if (stmt.isNull(10)) null else stmt.getLong(10),
            supplierPartId = if (stmt.isNull(11)) null else stmt.getLong(11),
            salesOrderId = if (stmt.isNull(12)) null else stmt.getLong(12),
            customerId = if (stmt.isNull(13)) null else stmt.getLong(13),
            buildId = if (stmt.isNull(14)) null else stmt.getLong(14),
            isBuilding = stmt.getLong(15) != 0L,
            parentId = if (stmt.isNull(16)) null else stmt.getLong(16),
            expiryDate = stmt.getText(17),
            stocktakeDate = stmt.getText(18),
            stocktakeUserId = if (stmt.isNull(19)) null else stmt.getLong(19),
            reviewNeeded = stmt.getLong(20) != 0L,
            deleteOnDeplete = stmt.getLong(21) != 0L,
            link = stmt.getText(22),
            notes = stmt.getText(23),
            metadata = stmt.getText(24),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(25)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(26) != 0L,
            updatedAt = stmt.getLong(27)
        )
    }
}
