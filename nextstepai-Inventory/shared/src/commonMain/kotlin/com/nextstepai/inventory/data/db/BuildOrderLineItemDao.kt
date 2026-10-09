package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات بنود ومخرجات أوامر الإنتاج والتصنيع (BuildOrderLineItemDao).
 */
@Dao
class BuildOrderLineItemDao {

    private val selectColumns = """
        uuid, id, buildId, buildUuid, bomItemId, bomItemUuid, subPartId, subPartName, quantity, allocatedQuantity, consumedQuantity, notes, phaseUuid, unitCost, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    fun getLineItemsForBuild(buildId: Long): List<BuildOrderLineItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildOrderLineItemEntity>()

        val sql = """
            SELECT $selectColumns
            FROM build_order_line_items
            WHERE isDeleted = 0 AND buildId = ?
            ORDER BY updatedAt ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, buildId)
            while (stmt.step()) {
                results.add(mapLineItemEntity(stmt))
            }
        }
        return results
    }

    fun getLineItemsForBuildUuid(buildUuid: String): List<BuildOrderLineItemEntity> {
        if (buildUuid.isBlank()) return emptyList()
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildOrderLineItemEntity>()

        val sql = """
            SELECT $selectColumns
            FROM build_order_line_items
            WHERE isDeleted = 0 AND buildUuid = ?
            ORDER BY updatedAt ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, buildUuid)
            while (stmt.step()) {
                results.add(mapLineItemEntity(stmt))
            }
        }
        return results
    }

    fun getPendingSyncLineItems(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<BuildOrderLineItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildOrderLineItemEntity>()

        conn.prepare("""
            SELECT $selectColumns
            FROM build_order_line_items
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapLineItemEntity(stmt))
            }
        }
        return results
    }

    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE build_order_line_items SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    fun insertOrUpdate(entity: BuildOrderLineItemEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO build_order_line_items (
                uuid, id, buildId, buildUuid, bomItemId, bomItemUuid, subPartId, subPartName, quantity, allocatedQuantity, consumedQuantity, notes, phaseUuid, unitCost, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.id)
            stmt.bindLong(3, entity.buildId)
            stmt.bindText(4, entity.buildUuid)
            stmt.bindLong(5, entity.bomItemId)
            stmt.bindText(6, entity.bomItemUuid)
            stmt.bindLong(7, entity.subPartId)
            stmt.bindText(8, entity.subPartName)
            stmt.bindDouble(9, entity.quantity)
            stmt.bindDouble(10, entity.allocatedQuantity)
            stmt.bindDouble(11, entity.consumedQuantity)
            stmt.bindText(12, entity.notes)
            if (entity.phaseUuid != null) stmt.bindText(13, entity.phaseUuid) else stmt.bindNull(13)
            stmt.bindDouble(14, entity.unitCost)
            stmt.bindText(15, entity.syncStatus.name)
            stmt.bindLong(16, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(17, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapLineItemEntity(stmt: SQLiteStatement): BuildOrderLineItemEntity {
        return BuildOrderLineItemEntity(
            uuid = stmt.getText(0),
            id = stmt.getLong(1),
            buildId = stmt.getLong(2),
            buildUuid = stmt.getText(3),
            bomItemId = stmt.getLong(4),
            bomItemUuid = stmt.getText(5),
            subPartId = stmt.getLong(6),
            subPartName = stmt.getText(7),
            quantity = stmt.getDouble(8),
            allocatedQuantity = stmt.getDouble(9),
            consumedQuantity = stmt.getDouble(10),
            notes = stmt.getText(11),
            phaseUuid = if (stmt.isNull(12)) null else stmt.getText(12),
            unitCost = runCatching { stmt.getDouble(13) }.getOrDefault(0.0),
            syncStatus = SyncStatus.fromString(stmt.getText(14)),
            isDeleted = stmt.getLong(15) != 0L,
            updatedAt = stmt.getLong(16)
        )
    }
}

