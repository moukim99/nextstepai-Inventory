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
        uuid, partId, partUuid, locationId, locationUuid, quantity, serial, batch, statusCode, packaging,
        purchasePrice, purchasePriceCurrency, purchaseOrderId, supplierPartId,
        salesOrderId, customerId, buildId, isBuilding, parentId, parentUuid, expiryDate,
        stocktakeDate, stocktakeUserId, reviewNeeded, deleteOnDeplete, link,
        notes, metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getStockItemsPaged(
        partId: Long? = null,
        partUuid: String? = null,
        locationId: Long? = null,
        locationUuid: String? = null,
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
              AND (? IS NULL OR partUuid = ?)
              AND (? IS NULL OR locationId = ?)
              AND (? IS NULL OR locationUuid = ?)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (partId != null) stmt.bindLong(1, partId) else stmt.bindNull(1)
            if (partId != null) stmt.bindLong(2, partId) else stmt.bindNull(2)
            if (partUuid != null) stmt.bindText(3, partUuid) else stmt.bindNull(3)
            if (partUuid != null) stmt.bindText(4, partUuid) else stmt.bindNull(4)
            if (locationId != null) stmt.bindLong(5, locationId) else stmt.bindNull(5)
            if (locationId != null) stmt.bindLong(6, locationId) else stmt.bindNull(6)
            if (locationUuid != null) stmt.bindText(7, locationUuid) else stmt.bindNull(7)
            if (locationUuid != null) stmt.bindText(8, locationUuid) else stmt.bindNull(8)
            stmt.bindLong(9, limit.toLong())
            stmt.bindLong(10, offset.toLong())
            while (stmt.step()) {
                results.add(mapStockItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun getStockItemByUuid(uuid: String): StockItemEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        var result: StockItemEntity? = null
        val sql = "SELECT $selectColumns FROM stock_items WHERE uuid = ? AND isDeleted = 0 LIMIT 1"
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, uuid)
            if (stmt.step()) {
                result = mapStockItemEntity(stmt)
            }
        }
        return result
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
                uuid, partId, partUuid, locationId, locationUuid, quantity, serial, batch, statusCode, packaging,
                purchasePrice, purchasePriceCurrency, purchaseOrderId, supplierPartId,
                salesOrderId, customerId, buildId, isBuilding, parentId, parentUuid, expiryDate,
                stocktakeDate, stocktakeUserId, reviewNeeded, deleteOnDeplete, link,
                notes, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.partId)
            if (entity.partUuid != null) stmt.bindText(3, entity.partUuid) else stmt.bindNull(3)
            if (entity.locationId != null) stmt.bindLong(4, entity.locationId) else stmt.bindNull(4)
            if (entity.locationUuid != null) stmt.bindText(5, entity.locationUuid) else stmt.bindNull(5)
            stmt.bindDouble(6, entity.quantity)
            stmt.bindText(7, entity.serial)
            stmt.bindText(8, entity.batch)
            stmt.bindLong(9, entity.statusCode.toLong())
            stmt.bindText(10, entity.packaging)
            stmt.bindDouble(11, entity.purchasePrice)
            stmt.bindText(12, entity.purchasePriceCurrency)
            if (entity.purchaseOrderId != null) stmt.bindLong(13, entity.purchaseOrderId) else stmt.bindNull(13)
            if (entity.supplierPartId != null) stmt.bindLong(14, entity.supplierPartId) else stmt.bindNull(14)
            if (entity.salesOrderId != null) stmt.bindLong(15, entity.salesOrderId) else stmt.bindNull(15)
            if (entity.customerId != null) stmt.bindLong(16, entity.customerId) else stmt.bindNull(16)
            if (entity.buildId != null) stmt.bindLong(17, entity.buildId) else stmt.bindNull(17)
            stmt.bindLong(18, if (entity.isBuilding) 1L else 0L)
            if (entity.parentId != null) stmt.bindLong(19, entity.parentId) else stmt.bindNull(19)
            if (entity.parentUuid != null) stmt.bindText(20, entity.parentUuid) else stmt.bindNull(20)
            stmt.bindText(21, entity.expiryDate)
            stmt.bindText(22, entity.stocktakeDate)
            if (entity.stocktakeUserId != null) stmt.bindLong(23, entity.stocktakeUserId) else stmt.bindNull(23)
            stmt.bindLong(24, if (entity.reviewNeeded) 1L else 0L)
            stmt.bindLong(25, if (entity.deleteOnDeplete) 1L else 0L)
            stmt.bindText(26, entity.link)
            stmt.bindText(27, entity.notes)
            stmt.bindText(28, entity.metadata)
            stmt.bindText(29, entity.syncStatus.name)
            stmt.bindLong(30, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(31, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun insertStockItemForPO(partId: Long, qty: Double, purchasePrice: Double, currency: String, batch: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val uuid = "stock-po-${Clock.System.now().toEpochMilliseconds()}"
        conn.prepare("""
            INSERT INTO stock_items (
                uuid, partId, partUuid, quantity, purchasePrice, purchasePriceCurrency, batch, statusCode, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 10, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindLong(2, partId)
            stmt.bindText(3, "part-$partId")
            stmt.bindDouble(4, qty)
            stmt.bindDouble(5, purchasePrice)
            stmt.bindText(6, currency)
            stmt.bindText(7, batch)
            stmt.bindLong(8, Clock.System.now().toEpochMilliseconds())
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
            partUuid = runCatching { if (stmt.isNull(2)) null else stmt.getText(2) }.getOrNull(),
            locationId = runCatching { if (stmt.isNull(3)) null else stmt.getLong(3) }.getOrNull(),
            locationUuid = runCatching { if (stmt.isNull(4)) null else stmt.getText(4) }.getOrNull(),
            quantity = runCatching { stmt.getDouble(5) }.getOrDefault(1.0),
            serial = runCatching { stmt.getText(6) }.getOrDefault(""),
            batch = runCatching { stmt.getText(7) }.getOrDefault(""),
            statusCode = runCatching { stmt.getLong(8).toInt() }.getOrDefault(10),
            packaging = runCatching { stmt.getText(9) }.getOrDefault("Box"),
            purchasePrice = runCatching { stmt.getDouble(10) }.getOrDefault(0.0),
            purchasePriceCurrency = runCatching { stmt.getText(11) }.getOrDefault("USD"),
            purchaseOrderId = runCatching { if (stmt.isNull(12)) null else stmt.getLong(12) }.getOrNull(),
            supplierPartId = runCatching { if (stmt.isNull(13)) null else stmt.getLong(13) }.getOrNull(),
            salesOrderId = runCatching { if (stmt.isNull(14)) null else stmt.getLong(14) }.getOrNull(),
            customerId = runCatching { if (stmt.isNull(15)) null else stmt.getLong(15) }.getOrNull(),
            buildId = runCatching { if (stmt.isNull(16)) null else stmt.getLong(16) }.getOrNull(),
            isBuilding = runCatching { stmt.getLong(17) != 0L }.getOrDefault(false),
            parentId = runCatching { if (stmt.isNull(18)) null else stmt.getLong(18) }.getOrNull(),
            parentUuid = runCatching { if (stmt.isNull(19)) null else stmt.getText(19) }.getOrNull(),
            expiryDate = runCatching { stmt.getText(20) }.getOrDefault(""),
            stocktakeDate = runCatching { stmt.getText(21) }.getOrDefault(""),
            stocktakeUserId = runCatching { if (stmt.isNull(22)) null else stmt.getLong(22) }.getOrNull(),
            reviewNeeded = runCatching { stmt.getLong(23) != 0L }.getOrDefault(false),
            deleteOnDeplete = runCatching { stmt.getLong(24) != 0L }.getOrDefault(false),
            link = runCatching { stmt.getText(25) }.getOrDefault(""),
            notes = runCatching { stmt.getText(26) }.getOrDefault(""),
            metadata = runCatching { stmt.getText(27) }.getOrDefault("{}"),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(28)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(29) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(30) }.getOrDefault(0L)
        )
    }
}
