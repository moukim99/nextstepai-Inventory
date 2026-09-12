package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات سجلات التتبع المخزني (StockItemTrackingDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockItemTrackingDao {

    private val selectColumns = """
        uuid, trackingId, stockItemId, date, trackingTypeCode, userId, label, notes,
        deltas, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getTrackingForStockItem(stockItemId: Long): List<StockItemTrackingEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemTrackingEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_item_tracking
            WHERE isDeleted = 0 AND stockItemId = ?
            ORDER BY updatedAt DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, stockItemId)
            while (stmt.step()) {
                results.add(mapStockItemTrackingEntity(stmt))
            }
        }
        return results
    }

    suspend fun getAllTrackingLogs(): List<StockItemTrackingEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemTrackingEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_item_tracking
            WHERE isDeleted = 0
            ORDER BY updatedAt DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            while (stmt.step()) {
                results.add(mapStockItemTrackingEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: StockItemTrackingEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_item_tracking (
                uuid, trackingId, stockItemId, date, trackingTypeCode, userId, label, notes,
                deltas, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.trackingId)
            stmt.bindLong(3, entity.stockItemId)
            stmt.bindText(4, entity.date)
            stmt.bindLong(5, entity.trackingTypeCode.toLong())
            if (entity.userId != null) stmt.bindLong(6, entity.userId) else stmt.bindNull(6)
            stmt.bindText(7, entity.label)
            stmt.bindText(8, entity.notes)
            stmt.bindText(9, entity.deltas)
            stmt.bindText(10, entity.syncStatus.name)
            stmt.bindLong(11, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(12, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapStockItemTrackingEntity(stmt: SQLiteStatement): StockItemTrackingEntity {
        return StockItemTrackingEntity(
            uuid = stmt.getText(0),
            trackingId = stmt.getLong(1),
            stockItemId = stmt.getLong(2),
            date = stmt.getText(3),
            trackingTypeCode = stmt.getLong(4).toInt(),
            userId = if (stmt.isNull(5)) null else stmt.getLong(5),
            label = stmt.getText(6),
            notes = stmt.getText(7),
            deltas = stmt.getText(8),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(9)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(10) != 0L,
            updatedAt = stmt.getLong(11)
        )
    }
}
