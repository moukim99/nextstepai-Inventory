package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أوامر الإنتاج والتصنيع (BuildOrderDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class BuildOrderDao {

    suspend fun getBuildOrdersPaged(
        partId: Long? = null,
        statusCode: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<BuildOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildOrderEntity>()

        val sql = """
            SELECT uuid, reference, title, partId, partName, quantity, completedQuantity, statusCode, batch, targetDate, syncStatus, isDeleted, updatedAt
            FROM build_orders
            WHERE isDeleted = 0
              AND (? IS NULL OR partId = ?)
              AND (? IS NULL OR statusCode = ?)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (partId != null) stmt.bindLong(1, partId) else stmt.bindNull(1)
            if (partId != null) stmt.bindLong(2, partId) else stmt.bindNull(2)
            if (statusCode != null) stmt.bindLong(3, statusCode.toLong()) else stmt.bindNull(3)
            if (statusCode != null) stmt.bindLong(4, statusCode.toLong()) else stmt.bindNull(4)
            stmt.bindLong(5, limit.toLong())
            stmt.bindLong(6, offset.toLong())
            while (stmt.step()) {
                results.add(mapBuildEntity(stmt))
            }
        }
        return results
    }

    suspend fun getPendingSyncBuilds(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<BuildOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildOrderEntity>()

        conn.prepare("""
            SELECT uuid, reference, title, partId, partName, quantity, completedQuantity, statusCode, batch, targetDate, syncStatus, isDeleted, updatedAt
            FROM build_orders
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapBuildEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: BuildOrderEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO build_orders (uuid, reference, title, partId, partName, quantity, completedQuantity, statusCode, batch, targetDate, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.reference)
            stmt.bindText(3, entity.title)
            stmt.bindLong(4, entity.partId)
            stmt.bindText(5, entity.partName)
            stmt.bindDouble(6, entity.quantity)
            stmt.bindDouble(7, entity.completedQuantity)
            stmt.bindLong(8, entity.statusCode.toLong())
            stmt.bindText(9, entity.batch)
            stmt.bindText(10, entity.targetDate)
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
            conn.prepare("UPDATE build_orders SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    private fun mapBuildEntity(stmt: SQLiteStatement): BuildOrderEntity {
        return BuildOrderEntity(
            uuid = stmt.getText(0),
            reference = stmt.getText(1),
            title = stmt.getText(2),
            partId = stmt.getLong(3),
            partName = stmt.getText(4),
            quantity = stmt.getDouble(5),
            completedQuantity = stmt.getDouble(6),
            statusCode = stmt.getLong(7).toInt(),
            batch = stmt.getText(8),
            targetDate = stmt.getText(9),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(10)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(11) != 0L,
            updatedAt = stmt.getLong(12)
        )
    }
}
