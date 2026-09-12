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
        uuid, trackingId, stockItemId, stockItemUuid, date, trackingTypeCode, userId, label, notes,
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
                uuid, trackingId, stockItemId, stockItemUuid, date, trackingTypeCode, userId, label, notes,
                deltas, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.trackingId)
            stmt.bindLong(3, entity.stockItemId)
            stmt.bindText(4, entity.stockItemUuid)
            stmt.bindText(5, entity.date)
            stmt.bindLong(6, entity.trackingTypeCode.toLong())
            if (entity.userId != null) stmt.bindLong(7, entity.userId) else stmt.bindNull(7)
            stmt.bindText(8, entity.label)
            stmt.bindText(9, entity.notes)
            stmt.bindText(10, entity.deltas)
            stmt.bindText(11, entity.syncStatus.name)
            stmt.bindLong(12, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(13, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapStockItemTrackingEntity(stmt: SQLiteStatement): StockItemTrackingEntity {
        return StockItemTrackingEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            trackingId = runCatching { stmt.getLong(1) }.getOrDefault(0L),
            stockItemId = runCatching { stmt.getLong(2) }.getOrDefault(0L),
            stockItemUuid = runCatching { stmt.getText(3) }.getOrDefault(""),
            date = runCatching { stmt.getText(4) }.getOrDefault(""),
            trackingTypeCode = runCatching { stmt.getLong(5).toInt() }.getOrDefault(10),
            userId = runCatching { if (stmt.isNull(6)) null else stmt.getLong(6) }.getOrNull(),
            label = runCatching { stmt.getText(7) }.getOrDefault(""),
            notes = runCatching { stmt.getText(8) }.getOrDefault(""),
            deltas = runCatching { stmt.getText(9) }.getOrDefault("{}"),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(10)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(11) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(12) }.getOrDefault(0L)
        )
    }
}
