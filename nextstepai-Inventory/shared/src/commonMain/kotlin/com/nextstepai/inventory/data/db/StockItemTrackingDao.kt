package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات سجلات التتبع المخزني (StockItemTrackingDao) المعزول كـ Append-only Ledger.
 */
@Dao
class StockItemTrackingDao {

    private val selectColumns = """
        uuid, stockItemUuid, trackingTypeCode, label, notes, deltas, userUuid, createdAt,
        syncStatus, lastModifiedByDeviceUuid
    """.trimIndent()

    fun getTrackingForStockItem(stockItemUuid: String): List<StockItemTrackingEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemTrackingEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_item_tracking
            WHERE stockItemUuid = ?
            ORDER BY createdAt DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, stockItemUuid)
            while (stmt.step()) {
                results.add(mapStockItemTrackingEntity(stmt))
            }
        }
        return results
    }

    fun getAllTrackingLogs(): List<StockItemTrackingEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemTrackingEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_item_tracking
            ORDER BY createdAt DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            while (stmt.step()) {
                results.add(mapStockItemTrackingEntity(stmt))
            }
        }
        return results
    }

    fun insertOrUpdate(entity: StockItemTrackingEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_item_tracking (
                uuid, stockItemUuid, trackingTypeCode, label, notes, deltas, userUuid, createdAt,
                syncStatus, lastModifiedByDeviceUuid
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.stockItemUuid)
            stmt.bindLong(3, entity.trackingTypeCode.toLong())
            stmt.bindText(4, entity.label)
            stmt.bindText(5, entity.notes)
            stmt.bindText(6, entity.deltas)
            if (entity.userUuid != null) stmt.bindText(7, entity.userUuid) else stmt.bindNull(7)
            stmt.bindLong(8, entity.createdAt)
            stmt.bindText(9, entity.syncStatus.name)
            if (entity.lastModifiedByDeviceUuid != null) stmt.bindText(10, entity.lastModifiedByDeviceUuid) else stmt.bindNull(10)
            stmt.step()
        }
    }

    private fun mapStockItemTrackingEntity(stmt: SQLiteStatement): StockItemTrackingEntity {
        return StockItemTrackingEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            stockItemUuid = runCatching { stmt.getText(1) }.getOrDefault(""),
            trackingTypeCode = runCatching { stmt.getLong(2).toInt() }.getOrDefault(10),
            label = runCatching { stmt.getText(3) }.getOrDefault(""),
            notes = runCatching { stmt.getText(4) }.getOrDefault(""),
            deltas = runCatching { stmt.getText(5) }.getOrDefault("{}"),
            userUuid = runCatching { if (stmt.isNull(6)) null else stmt.getText(6) }.getOrNull(),
            createdAt = runCatching { stmt.getLong(7) }.getOrDefault(0L),
            syncStatus = SyncStatus.fromString(stmt.getText(8)),
            lastModifiedByDeviceUuid = runCatching { if (stmt.isNull(9)) null else stmt.getText(9) }.getOrNull()
        )
    }
}
