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
        uuid, id, buildId, bomItemId, subPartId, subPartName, quantity, allocatedQuantity, consumedQuantity, notes, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getLineItemsForBuild(buildId: Long): List<BuildOrderLineItemEntity> {
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

    suspend fun insertOrUpdate(entity: BuildOrderLineItemEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO build_order_line_items (
                uuid, id, buildId, bomItemId, subPartId, subPartName, quantity, allocatedQuantity, consumedQuantity, notes, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.id)
            stmt.bindLong(3, entity.buildId)
            stmt.bindLong(4, entity.bomItemId)
            stmt.bindLong(5, entity.subPartId)
            stmt.bindText(6, entity.subPartName)
            stmt.bindDouble(7, entity.quantity)
            stmt.bindDouble(8, entity.allocatedQuantity)
            stmt.bindDouble(9, entity.consumedQuantity)
            stmt.bindText(10, entity.notes)
            stmt.bindText(11, entity.syncStatus.name)
            stmt.bindLong(12, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(13, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapLineItemEntity(stmt: SQLiteStatement): BuildOrderLineItemEntity {
        return BuildOrderLineItemEntity(
            uuid = stmt.getText(0),
            id = stmt.getLong(1),
            buildId = stmt.getLong(2),
            bomItemId = stmt.getLong(3),
            subPartId = stmt.getLong(4),
            subPartName = stmt.getText(5),
            quantity = stmt.getDouble(6),
            allocatedQuantity = stmt.getDouble(7),
            consumedQuantity = stmt.getDouble(8),
            notes = stmt.getText(9),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(10)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(11) != 0L,
            updatedAt = stmt.getLong(12)
        )
    }
}
