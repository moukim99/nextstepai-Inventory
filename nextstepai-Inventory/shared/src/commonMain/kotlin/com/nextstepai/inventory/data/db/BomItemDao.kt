package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات BOM (BomItemDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class BomItemDao {

    suspend fun getBomItemsPaged(partId: Long? = null, limit: Int = 20, offset: Int = 0): List<BomItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BomItemEntity>()
        val sql = if (partId != null) {
            """
                SELECT uuid, partId, subPartId, quantity, reference, optional, consumable, allowVariants, inherited, note, checksum, phaseUuid, syncStatus, isDeleted, updatedAt
                FROM bom_items
                WHERE isDeleted = 0 AND partId = ?
                ORDER BY updatedAt DESC
                LIMIT ? OFFSET ?
            """.trimIndent()
        } else {
            """
                SELECT uuid, partId, subPartId, quantity, reference, optional, consumable, allowVariants, inherited, note, checksum, phaseUuid, syncStatus, isDeleted, updatedAt
                FROM bom_items
                WHERE isDeleted = 0
                ORDER BY updatedAt DESC
                LIMIT ? OFFSET ?
            """.trimIndent()
        }

        conn.prepare(sql).use { stmt ->
            if (partId != null) {
                stmt.bindLong(1, partId)
                stmt.bindLong(2, limit.toLong())
                stmt.bindLong(3, offset.toLong())
            } else {
                stmt.bindLong(1, limit.toLong())
                stmt.bindLong(2, offset.toLong())
            }
            while (stmt.step()) {
                results.add(mapBomItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun getPendingSyncBomItems(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<BomItemEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BomItemEntity>()
        conn.prepare("""
            SELECT uuid, partId, subPartId, quantity, reference, optional, consumable, allowVariants, inherited, note, checksum, phaseUuid, syncStatus, isDeleted, updatedAt
            FROM bom_items
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapBomItemEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: BomItemEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO bom_items (uuid, partId, subPartId, quantity, reference, optional, consumable, allowVariants, inherited, note, checksum, phaseUuid, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.partId)
            stmt.bindLong(3, entity.subPartId)
            stmt.bindDouble(4, entity.quantity)
            stmt.bindText(5, entity.reference)
            stmt.bindLong(6, if (entity.optional) 1L else 0L)
            stmt.bindLong(7, if (entity.consumable) 1L else 0L)
            stmt.bindLong(8, if (entity.allowVariants) 1L else 0L)
            stmt.bindLong(9, if (entity.inherited) 1L else 0L)
            stmt.bindText(10, entity.note)
            stmt.bindText(11, entity.checksum)
            if (entity.phaseUuid != null) stmt.bindText(12, entity.phaseUuid) else stmt.bindNull(12)
            stmt.bindText(13, entity.syncStatus.name)
            stmt.bindLong(14, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(15, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun softDeleteByIdOrUuid(id: Long, uuid: String, updatedAt: Long) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("UPDATE bom_items SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ? OR rowid = ?").use { stmt ->
            stmt.bindLong(1, updatedAt)
            stmt.bindText(2, uuid)
            stmt.bindLong(3, id)
            stmt.step()
        }
    }

    suspend fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE bom_items SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    private fun mapBomItemEntity(stmt: SQLiteStatement): BomItemEntity {
        return BomItemEntity(
            uuid = stmt.getText(0),
            partId = stmt.getLong(1),
            subPartId = stmt.getLong(2),
            quantity = stmt.getDouble(3),
            reference = stmt.getText(4),
            optional = stmt.getLong(5) != 0L,
            consumable = stmt.getLong(6) != 0L,
            allowVariants = stmt.getLong(7) != 0L,
            inherited = stmt.getLong(8) != 0L,
            note = stmt.getText(9),
            checksum = stmt.getText(10),
            phaseUuid = if (stmt.isNull(11)) null else stmt.getText(11),
            syncStatus = SyncStatus.fromString(stmt.getText(12)),
            isDeleted = stmt.getLong(13) != 0L,
            updatedAt = stmt.getLong(14)
        )
    }
}
