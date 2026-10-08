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
        uuid, id, buildId, buildUuid, buildLineId, buildLineUuid, stockItemId, stockItemUuid, stockItemName, quantity, installIntoStockItemId, installIntoStockItemUuid, notes, syncStatus, isDeleted, updatedAt
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

    suspend fun getBuildItemsForBuildUuid(buildUuid: String): List<BuildItemEntity> {
        if (buildUuid.isBlank()) return emptyList()
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildItemEntity>()

        val sql = """
            SELECT $selectColumns
            FROM build_items
            WHERE isDeleted = 0 AND buildUuid = ?
            ORDER BY updatedAt ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, buildUuid)
            while (stmt.step()) {
                results.add(mapBuildItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun getPendingSyncBuildItems(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<BuildItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildItemEntity>()

        conn.prepare("""
            SELECT $selectColumns
            FROM build_items
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapBuildItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE build_items SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    suspend fun insertOrUpdate(entity: BuildItemEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO build_items (
                uuid, id, buildId, buildUuid, buildLineId, buildLineUuid, stockItemId, stockItemUuid, stockItemName, quantity, installIntoStockItemId, installIntoStockItemUuid, notes, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.id)
            stmt.bindLong(3, entity.buildId)
            stmt.bindText(4, entity.buildUuid)
            if (entity.buildLineId != null) stmt.bindLong(5, entity.buildLineId) else stmt.bindNull(5)
            stmt.bindText(6, entity.buildLineUuid)
            stmt.bindLong(7, entity.stockItemId)
            stmt.bindText(8, entity.stockItemUuid)
            stmt.bindText(9, entity.stockItemName)
            stmt.bindDouble(10, entity.quantity)
            if (entity.installIntoStockItemId != null) stmt.bindLong(11, entity.installIntoStockItemId) else stmt.bindNull(11)
            stmt.bindText(12, entity.installIntoStockItemUuid)
            stmt.bindText(13, entity.notes)
            stmt.bindText(14, entity.syncStatus.name)
            stmt.bindLong(15, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(16, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapBuildItemEntity(stmt: SQLiteStatement): BuildItemEntity {
        return BuildItemEntity(
            uuid = stmt.getText(0),
            id = stmt.getLong(1),
            buildId = stmt.getLong(2),
            buildUuid = stmt.getText(3),
            buildLineId = if (stmt.isNull(4)) null else stmt.getLong(4),
            buildLineUuid = stmt.getText(5),
            stockItemId = stmt.getLong(6),
            stockItemUuid = stmt.getText(7),
            stockItemName = stmt.getText(8),
            quantity = stmt.getDouble(9),
            installIntoStockItemId = if (stmt.isNull(10)) null else stmt.getLong(10),
            installIntoStockItemUuid = stmt.getText(11),
            notes = stmt.getText(12),
            syncStatus = SyncStatus.fromString(stmt.getText(13)),
            isDeleted = stmt.getLong(14) != 0L,
            updatedAt = stmt.getLong(15)
        )
    }
}

