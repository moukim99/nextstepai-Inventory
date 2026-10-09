package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات المخزون الفعلي (StockItemDao) المعتمد بنسبة 100% على معرّفات UUIDv7.
 */
@Dao
class StockItemDao {

    private val selectColumns = """
        uuid, partUuid, locationUuid, quantity, serial, batch, statusCode, packaging,
        expiryDate, notes, purchasePrice, purchasePriceCurrency, purchaseOrderUuid, supplierPartUuid,
        salesOrderUuid, customerUuid, buildUuid, isBuilding, parentStockItemUuid, stocktakeDate,
        stocktakeUserUuid, reviewNeeded, deleteOnDeplete, link, metadata, version, syncStatus, isDeleted,
        updatedAt, lastModifiedByDeviceUuid
    """.trimIndent()

    fun getStockItemsPaged(
        partUuid: String? = null,
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
              AND (? IS NULL OR partUuid = ?)
              AND (? IS NULL OR locationUuid = ?)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (partUuid != null) stmt.bindText(1, partUuid) else stmt.bindNull(1)
            if (partUuid != null) stmt.bindText(2, partUuid) else stmt.bindNull(2)
            if (locationUuid != null) stmt.bindText(3, locationUuid) else stmt.bindNull(3)
            if (locationUuid != null) stmt.bindText(4, locationUuid) else stmt.bindNull(4)
            stmt.bindLong(5, limit.toLong())
            stmt.bindLong(6, offset.toLong())
            while (stmt.step()) {
                results.add(mapStockItemEntity(stmt))
            }
        }
        return results
    }

    fun getStockItemByUuid(uuid: String): StockItemEntity? {
        if (uuid.isBlank()) return null
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

    fun getPendingSyncStockItems(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<StockItemEntity> {
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

    fun insertOrUpdate(entity: StockItemEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        val numericId = entity.uuid.removePrefix("stock-").toLongOrNull() ?: 0L
        val numericPartId = entity.partUuid.removePrefix("part-").toLongOrNull() ?: 0L
        val numericLocationId = entity.locationUuid?.removePrefix("loc-")?.removePrefix("location-")?.toLongOrNull()

        conn.prepare("""
            INSERT INTO stock_items (
                uuid, id, partId, locationId, partUuid, locationUuid, quantity, serial, batch, statusCode, packaging,
                expiryDate, notes, purchasePrice, purchasePriceCurrency, purchaseOrderUuid, supplierPartUuid,
                salesOrderUuid, customerUuid, buildUuid, isBuilding, parentStockItemUuid, stocktakeDate,
                stocktakeUserUuid, reviewNeeded, deleteOnDeplete, link, metadata, version, syncStatus, isDeleted,
                updatedAt, lastModifiedByDeviceUuid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                id = CASE WHEN excluded.id > 0 THEN excluded.id ELSE stock_items.id END,
                partId = CASE WHEN excluded.partId > 0 THEN excluded.partId ELSE stock_items.partId END,
                locationId = CASE WHEN excluded.locationId IS NOT NULL THEN excluded.locationId ELSE stock_items.locationId END,
                partUuid = excluded.partUuid,
                locationUuid = excluded.locationUuid,
                quantity = excluded.quantity,
                serial = excluded.serial,
                batch = excluded.batch,
                statusCode = excluded.statusCode,
                packaging = excluded.packaging,
                expiryDate = excluded.expiryDate,
                notes = excluded.notes,
                purchasePrice = excluded.purchasePrice,
                purchasePriceCurrency = excluded.purchasePriceCurrency,
                purchaseOrderUuid = excluded.purchaseOrderUuid,
                supplierPartUuid = excluded.supplierPartUuid,
                salesOrderUuid = excluded.salesOrderUuid,
                customerUuid = excluded.customerUuid,
                buildUuid = excluded.buildUuid,
                isBuilding = excluded.isBuilding,
                parentStockItemUuid = excluded.parentStockItemUuid,
                stocktakeDate = excluded.stocktakeDate,
                stocktakeUserUuid = excluded.stocktakeUserUuid,
                reviewNeeded = excluded.reviewNeeded,
                deleteOnDeplete = excluded.deleteOnDeplete,
                link = excluded.link,
                metadata = excluded.metadata,
                version = stock_items.version + 1,
                syncStatus = excluded.syncStatus,
                isDeleted = excluded.isDeleted,
                updatedAt = excluded.updatedAt,
                lastModifiedByDeviceUuid = excluded.lastModifiedByDeviceUuid
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, numericId)
            stmt.bindLong(3, numericPartId)
            if (numericLocationId != null) stmt.bindLong(4, numericLocationId) else stmt.bindNull(4)
            stmt.bindText(5, entity.partUuid)
            if (entity.locationUuid != null) stmt.bindText(6, entity.locationUuid) else stmt.bindNull(6)
            stmt.bindDouble(7, entity.quantity)
            stmt.bindText(8, entity.serial)
            stmt.bindText(9, entity.batch)
            stmt.bindLong(10, entity.statusCode.toLong())
            stmt.bindText(11, entity.packaging)
            stmt.bindText(12, entity.expiryDate)
            stmt.bindText(13, entity.notes)
            stmt.bindDouble(14, entity.purchasePrice)
            stmt.bindText(15, entity.purchasePriceCurrency)
            if (entity.purchaseOrderUuid != null) stmt.bindText(16, entity.purchaseOrderUuid) else stmt.bindNull(16)
            stmt.bindText(17, entity.supplierPartUuid)
            if (entity.salesOrderUuid != null) stmt.bindText(18, entity.salesOrderUuid) else stmt.bindNull(18)
            stmt.bindText(19, entity.customerUuid)
            if (entity.buildUuid != null) stmt.bindText(20, entity.buildUuid) else stmt.bindNull(20)
            stmt.bindLong(21, if (entity.isBuilding) 1L else 0L)
            if (entity.parentStockItemUuid != null) stmt.bindText(22, entity.parentStockItemUuid) else stmt.bindNull(22)
            stmt.bindText(23, entity.stocktakeDate)
            if (entity.stocktakeUserUuid != null) stmt.bindText(24, entity.stocktakeUserUuid) else stmt.bindNull(24)
            stmt.bindLong(25, if (entity.reviewNeeded) 1L else 0L)
            stmt.bindLong(26, if (entity.deleteOnDeplete) 1L else 0L)
            stmt.bindText(27, entity.link)
            stmt.bindText(28, entity.metadata)
            stmt.bindLong(29, entity.version.toLong())
            stmt.bindText(30, entity.syncStatus.name)
            stmt.bindLong(31, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(32, entity.updatedAt)
            if (entity.lastModifiedByDeviceUuid != null) stmt.bindText(33, entity.lastModifiedByDeviceUuid) else stmt.bindNull(33)
            stmt.step()
        }
    }

    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
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

    fun getAvailableStockItemsForPart(partId: Long, partUuid: String? = null): List<StockItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemEntity>()
        val p1 = partUuid ?: ""
        val p2 = "part-$partId"
        val p3 = "part-uuid-$partId"
        val sql = """
            SELECT $selectColumns
            FROM stock_items
            WHERE isDeleted = 0
              AND quantity > 0
              AND statusCode = 10
              AND (
                  partId = ?
                  OR partUuid = ?
                  OR partUuid = ?
                  OR partUuid = ?
              )
            ORDER BY expiryDate ASC, updatedAt ASC
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, partId)
            stmt.bindText(2, p1)
            stmt.bindText(3, p2)
            stmt.bindText(4, p3)
            while (stmt.step()) {
                results.add(mapStockItemEntity(stmt))
            }
        }
        return results
    }

    fun updateStockItemQuantity(uuid: String, newQuantity: Double, updatedAt: Long) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE stock_items
            SET quantity = ?,
                syncStatus = 'PENDING',
                updatedAt = ?
            WHERE uuid = ?
        """.trimIndent()).use { stmt ->
            stmt.bindDouble(1, newQuantity)
            stmt.bindLong(2, updatedAt)
            stmt.bindText(3, uuid)
            stmt.step()
        }
    }

    fun getStockItemById(id: Long): StockItemEntity? {
        val direct = getStockItemByUuid("stock-$id")
        if (direct != null) return direct
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "SELECT $selectColumns FROM stock_items WHERE isDeleted = 0 AND (id = ? OR uuid = ? OR uuid LIKE ?) LIMIT 1"
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, id)
            stmt.bindText(2, id.toString())
            stmt.bindText(3, "%-$id")
            if (stmt.step()) {
                return mapStockItemEntity(stmt)
            }
        }
        return null
    }

    fun updateStocktakeReconciliation(
        uuid: String,
        newQuantity: Double,
        stocktakeDate: String,
        stocktakeUserUuid: String?,
        updatedAt: Long,
        deleteOnDeplete: Boolean = false
    ) {
        val conn = SqliteDatabaseManager.getConnection()
        if (newQuantity <= 0.0001 && deleteOnDeplete) {
            val sql = """
                UPDATE stock_items
                SET quantity = 0.0,
                    isDeleted = 1,
                    stocktakeDate = ?,
                    stocktakeUserUuid = ?,
                    reviewNeeded = 0,
                    syncStatus = 'PENDING',
                    updatedAt = ?
                WHERE uuid = ?
            """.trimIndent()
            conn.prepare(sql).use { stmt ->
                stmt.bindText(1, stocktakeDate)
                if (stocktakeUserUuid != null) stmt.bindText(2, stocktakeUserUuid) else stmt.bindNull(2)
                stmt.bindLong(3, updatedAt)
                stmt.bindText(4, uuid)
                stmt.step()
            }
        } else {
            val sql = """
                UPDATE stock_items
                SET quantity = ?,
                    stocktakeDate = ?,
                    stocktakeUserUuid = ?,
                    reviewNeeded = 0,
                    syncStatus = 'PENDING',
                    updatedAt = ?
                WHERE uuid = ?
            """.trimIndent()
            conn.prepare(sql).use { stmt ->
                stmt.bindDouble(1, newQuantity)
                stmt.bindText(2, stocktakeDate)
                if (stocktakeUserUuid != null) stmt.bindText(3, stocktakeUserUuid) else stmt.bindNull(3)
                stmt.bindLong(4, updatedAt)
                stmt.bindText(5, uuid)
                stmt.step()
            }
        }
    }

    fun updateStockItemLocation(uuid: String, newLocationUuid: String, updatedAt: Long) {
        val conn = SqliteDatabaseManager.getConnection()
        val numericLocationId = newLocationUuid.removePrefix("loc-").removePrefix("location-").toLongOrNull()
        val sql = """
            UPDATE stock_items
            SET locationUuid = ?,
                locationId = ?,
                syncStatus = 'PENDING',
                updatedAt = ?
            WHERE uuid = ?
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, newLocationUuid)
            if (numericLocationId != null) stmt.bindLong(2, numericLocationId) else stmt.bindNull(2)
            stmt.bindLong(3, updatedAt)
            stmt.bindText(4, uuid)
            stmt.step()
        }
    }

    fun findStockItemByPartAndLocation(partUuid: String, locationUuid: String, batch: String, statusCode: Int): StockItemEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT $selectColumns
            FROM stock_items
            WHERE isDeleted = 0
              AND partUuid = ?
              AND locationUuid = ?
              AND batch = ?
              AND statusCode = ?
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, partUuid)
            stmt.bindText(2, locationUuid)
            stmt.bindText(3, batch)
            stmt.bindLong(4, statusCode.toLong())
            if (stmt.step()) {
                return mapStockItemEntity(stmt)
            }
        }
        return null
    }

    fun depleteStockItem(uuid: String, updatedAt: Long, deleteOnDeplete: Boolean = false) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = if (deleteOnDeplete) {
            "UPDATE stock_items SET quantity = 0.0, isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?"
        } else {
            "UPDATE stock_items SET quantity = 0.0, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?"
        }
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, updatedAt)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }

    private fun mapStockItemEntity(stmt: SQLiteStatement): StockItemEntity {
        return StockItemEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            partUuid = runCatching { stmt.getText(1) }.getOrDefault(""),
            locationUuid = runCatching { if (stmt.isNull(2)) null else stmt.getText(2) }.getOrNull(),
            quantity = runCatching { stmt.getDouble(3) }.getOrDefault(1.0),
            serial = runCatching { stmt.getText(4) }.getOrDefault(""),
            batch = runCatching { stmt.getText(5) }.getOrDefault(""),
            statusCode = runCatching { stmt.getLong(6).toInt() }.getOrDefault(10),
            packaging = runCatching { stmt.getText(7) }.getOrDefault("Box"),
            expiryDate = runCatching { stmt.getText(8) }.getOrDefault(""),
            notes = runCatching { stmt.getText(9) }.getOrDefault(""),
            purchasePrice = runCatching { stmt.getDouble(10) }.getOrDefault(0.0),
            purchasePriceCurrency = runCatching { stmt.getText(11) }.getOrDefault("USD"),
            purchaseOrderUuid = runCatching { if (stmt.isNull(12)) null else stmt.getText(12) }.getOrNull(),
            supplierPartUuid = runCatching { stmt.getText(13) }.getOrDefault(""),
            salesOrderUuid = runCatching { if (stmt.isNull(14)) null else stmt.getText(14) }.getOrNull(),
            customerUuid = runCatching { stmt.getText(15) }.getOrDefault(""),
            buildUuid = runCatching { if (stmt.isNull(16)) null else stmt.getText(16) }.getOrNull(),
            isBuilding = runCatching { stmt.getLong(17) != 0L }.getOrDefault(false),
            parentStockItemUuid = runCatching { if (stmt.isNull(18)) null else stmt.getText(18) }.getOrNull(),
            stocktakeDate = runCatching { stmt.getText(19) }.getOrDefault(""),
            stocktakeUserUuid = runCatching { if (stmt.isNull(20)) null else stmt.getText(20) }.getOrNull(),
            reviewNeeded = runCatching { stmt.getLong(21) != 0L }.getOrDefault(false),
            deleteOnDeplete = runCatching { stmt.getLong(22) != 0L }.getOrDefault(false),
            link = runCatching { stmt.getText(23) }.getOrDefault(""),
            metadata = runCatching { stmt.getText(24) }.getOrDefault("{}"),
            version = runCatching { stmt.getLong(25).toInt() }.getOrDefault(1),
            syncStatus = SyncStatus.fromString(stmt.getText(26)),
            isDeleted = runCatching { stmt.getLong(27) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(28) }.getOrDefault(0L),
            lastModifiedByDeviceUuid = runCatching { if (stmt.isNull(29)) null else stmt.getText(29) }.getOrNull()
        )
    }
}
