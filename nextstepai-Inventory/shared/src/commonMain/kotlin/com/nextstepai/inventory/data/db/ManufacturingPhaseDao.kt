package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات المراحل التصنيعية (ManufacturingPhaseDao) باستعلامات SQLite معلّمة.
 */
@Dao
class ManufacturingPhaseDao {

    fun getAllPhases(partUuid: String? = null): List<ManufacturingPhaseEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<ManufacturingPhaseEntity>()
        val sql = if (partUuid != null) {
            """
                SELECT uuid, id, partUuid, name, sequenceOrder, description, isSystemDefault, syncStatus, isDeleted, updatedAt
                FROM manufacturing_phases
                WHERE isDeleted = 0 AND (partUuid IS NULL OR partUuid = ?)
                ORDER BY sequenceOrder ASC, name ASC
            """.trimIndent()
        } else {
            """
                SELECT uuid, id, partUuid, name, sequenceOrder, description, isSystemDefault, syncStatus, isDeleted, updatedAt
                FROM manufacturing_phases
                WHERE isDeleted = 0
                ORDER BY sequenceOrder ASC, name ASC
            """.trimIndent()
        }

        conn.prepare(sql).use { stmt ->
            if (partUuid != null) {
                stmt.bindText(1, partUuid)
            }
            while (stmt.step()) {
                results.add(mapManufacturingPhaseEntity(stmt))
            }
        }
        return results
    }

    fun insertOrUpdate(entity: ManufacturingPhaseEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            INSERT OR REPLACE INTO manufacturing_phases (
                uuid, id, partUuid, name, sequenceOrder, description, isSystemDefault, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, entity.uuid)
            if (entity.id != 0L) stmt.bindLong(2, entity.id) else stmt.bindNull(2)
            if (entity.partUuid != null) stmt.bindText(3, entity.partUuid) else stmt.bindNull(3)
            stmt.bindText(4, entity.name)
            stmt.bindLong(5, entity.sequenceOrder.toLong())
            stmt.bindText(6, entity.description)
            stmt.bindLong(7, if (entity.isSystemDefault) 1L else 0L)
            stmt.bindText(8, entity.syncStatus.name)
            stmt.bindLong(9, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(10, entity.updatedAt)
            stmt.step()
        }
    }

    fun deletePhase(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        
        // 1. تعليم المرحلة كمحذوفة في manufacturing_phases
        conn.prepare("""
            UPDATE manufacturing_phases 
            SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? 
            WHERE uuid = ?
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }

        // 2. معالجة البنود المرتبطة بالمرحلة في bom_items وتفريغ phaseUuid لتجنب تعارض التكامل
        conn.prepare("""
            UPDATE bom_items 
            SET phaseUuid = NULL, syncStatus = 'PENDING', updatedAt = ? 
            WHERE phaseUuid = ?
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }

    private fun mapManufacturingPhaseEntity(stmt: SQLiteStatement): ManufacturingPhaseEntity {
        return ManufacturingPhaseEntity(
            uuid = stmt.getText(0),
            id = stmt.getLong(1),
            partUuid = if (stmt.isNull(2)) null else stmt.getText(2),
            name = stmt.getText(3),
            sequenceOrder = stmt.getLong(4).toInt(),
            description = stmt.getText(5),
            isSystemDefault = stmt.getLong(6) != 0L,
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(7)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(8) != 0L,
            updatedAt = stmt.getLong(9)
        )
    }
}
