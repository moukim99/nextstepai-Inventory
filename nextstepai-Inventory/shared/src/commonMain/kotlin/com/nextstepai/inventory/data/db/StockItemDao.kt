package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات المخزون الفعلي (StockItemDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockItemDao {

    suspend fun getStockItemsPaged(
        partId: Long? = null,
        locationId: Long? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<StockItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemEntity>()

        val sql = """
            SELECT uuid, partId, locationId, quantity, serial, batch, statusCode, packaging, expiryDate, notes, syncStatus, isDeleted, updatedAt
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
            SELECT uuid, partId, locationId, quantity, serial, batch, statusCode, packaging, expiryDate, notes, syncStatus, isDeleted, updatedAt
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
            INSERT OR REPLACE INTO stock_items (uuid, partId, locationId, quantity, serial, batch, statusCode, packaging, expiryDate, notes, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.partId)
            if (entity.locationId != null) stmt.bindLong(3, entity.locationId) else stmt.bindNull(3)
            stmt.bindDouble(4, entity.quantity)
            stmt.bindText(5, entity.serial)
            stmt.bindText(6, entity.batch)
            stmt.bindLong(7, entity.statusCode.toLong())
            stmt.bindText(8, entity.packaging)
            stmt.bindText(9, entity.expiryDate)
            stmt.bindText(10, entity.notes)
            stmt.bindText(11, entity.syncStatus.name)
            stmt.bindLong(12, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(13, entity.updatedAt)
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
            expiryDate = stmt.getText(8),
            notes = stmt.getText(9),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(10)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(11) != 0L,
            updatedAt = stmt.getLong(12)
        )
    }
}
