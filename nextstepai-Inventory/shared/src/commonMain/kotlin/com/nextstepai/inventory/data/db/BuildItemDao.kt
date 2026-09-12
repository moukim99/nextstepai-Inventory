package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات تخصيصات وتتبع سجلات سحب المخزون لأمر التصنيع (BuildItemDao).
 */
@Dao
class BuildItemDao {

    private val selectColumns = """
        uuid, id, buildId, buildLineId, stockItemId, stockItemName, quantity, installIntoStockItemId, notes, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getBuildItemsForBuild(buildId: Long): List<BuildItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildItemEntity>()

        val sql = """
            SELECT $selectColumns
            FROM build_items
            WHERE isDeleted = 0 AND buildId = ?
            ORDER BY updatedAt ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, buildId)
            while (stmt.step()) {
                results.add(mapBuildItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: BuildItemEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO build_items (
                uuid, id, buildId, buildLineId, stockItemId, stockItemName, quantity, installIntoStockItemId, notes, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.id)
            stmt.bindLong(3, entity.buildId)
            if (entity.buildLineId != null) stmt.bindLong(4, entity.buildLineId) else stmt.bindNull(4)
            stmt.bindLong(5, entity.stockItemId)
            stmt.bindText(6, entity.stockItemName)
            stmt.bindDouble(7, entity.quantity)
            if (entity.installIntoStockItemId != null) stmt.bindLong(8, entity.installIntoStockItemId) else stmt.bindNull(8)
            stmt.bindText(9, entity.notes)
            stmt.bindText(10, entity.syncStatus.name)
            stmt.bindLong(11, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(12, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapBuildItemEntity(stmt: SQLiteStatement): BuildItemEntity {
        return BuildItemEntity(
            uuid = stmt.getText(0),
            id = stmt.getLong(1),
            buildId = stmt.getLong(2),
            buildLineId = if (stmt.isNull(3)) null else stmt.getLong(3),
            stockItemId = stmt.getLong(4),
            stockItemName = stmt.getText(5),
            quantity = stmt.getDouble(6),
            installIntoStockItemId = if (stmt.isNull(7)) null else stmt.getLong(7),
            notes = stmt.getText(8),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(9)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(10) != 0L,
            updatedAt = stmt.getLong(11)
        )
    }
}
