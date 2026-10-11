package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement

/**
 * كائن الوصول لبيانات حجوزات ومخصصات القطع (PartAllocationDao) باستعلامات معلّمة صريحة.
 */
@Dao
class PartAllocationDao {

    /**
     * جلب كافة الحجوزات النشطة لقطعة محددة مرتبة من الأحدث إلى الأقدم.
     */
    fun getActiveAllocationsForPart(partId: Long): List<PartAllocationEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartAllocationEntity>()
        conn.prepare("""
            SELECT COALESCE(NULLIF(id, 0), rowid) AS id,
                   COALESCE(NULLIF(partId, 0), ?),
                   allocatedQuantity, allocationType, referenceType, referenceId,
                   referenceTitle, status, createdAt, createdByUserId, notes
            FROM part_allocations
            WHERE (partId = ? OR partUuid = 'part-' || ?) AND status = 'ACTIVE'
            ORDER BY createdAt DESC
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            stmt.bindLong(2, partId)
            stmt.bindLong(3, partId)
            while (stmt.step()) {
                results.add(mapAllocation(stmt))
            }
        }
        return results
    }

    /**
     * جلب كافة الحجوزات (النشطة والمحررة والمستهلكة) لقطعة محددة.
     */
    fun getAllAllocationsForPart(partId: Long): List<PartAllocationEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartAllocationEntity>()
        conn.prepare("""
            SELECT COALESCE(NULLIF(id, 0), rowid) AS id,
                   COALESCE(NULLIF(partId, 0), ?),
                   allocatedQuantity, allocationType, referenceType, referenceId,
                   referenceTitle, status, createdAt, createdByUserId, notes
            FROM part_allocations
            WHERE (partId = ? OR partUuid = 'part-' || ?)
            ORDER BY createdAt DESC
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            stmt.bindLong(2, partId)
            stmt.bindLong(3, partId)
            while (stmt.step()) {
                results.add(mapAllocation(stmt))
            }
        }
        return results
    }

    /**
     * حساب إجمالي الكمية المحجوزة حجزاً مؤكداً (HARD Committed) لقطعة محددة.
     */
    fun getTotalCommittedQuantity(partId: Long): Double {
        val conn = SqliteDatabaseManager.getConnection()
        var total = 0.0
        conn.prepare("""
            SELECT SUM(allocatedQuantity)
            FROM part_allocations
            WHERE (partId = ? OR partUuid = 'part-' || ?) AND status = 'ACTIVE' AND allocationType = 'HARD'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            stmt.bindLong(2, partId)
            if (stmt.step() && !stmt.isNull(0)) {
                total = stmt.getDouble(0)
            }
        }
        return total
    }

    /**
     * حساب إجمالي الكمية المحجوزة حجزاً مبدئياً (SOFT Reserved) لقطعة محددة.
     */
    fun getTotalSoftQuantity(partId: Long): Double {
        val conn = SqliteDatabaseManager.getConnection()
        var total = 0.0
        conn.prepare("""
            SELECT SUM(allocatedQuantity)
            FROM part_allocations
            WHERE (partId = ? OR partUuid = 'part-' || ?) AND status = 'ACTIVE' AND allocationType = 'SOFT'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            stmt.bindLong(2, partId)
            if (stmt.step() && !stmt.isNull(0)) {
                total = stmt.getDouble(0)
            }
        }
        return total
    }

    /**
     * إدراج سجل حجز جديد في القاعدة وتوليد معرف فريد.
     */
    fun insertAllocation(entity: PartAllocationEntity): Long {
        val conn = SqliteDatabaseManager.getConnection()
        var generatedId = 0L
        val generatedUuid = "alloc-${entity.partId}-${entity.createdAt}-${(1000..9999).random()}"
        val partUuid = "part-${entity.partId}"
        val refUuid = if (entity.referenceId.isNotBlank()) entity.referenceId else "ref-${entity.partId}"
        val userUuid = entity.createdByUserId.ifBlank { "1" }

        conn.prepare("""
            INSERT INTO part_allocations (
                uuid, partId, partUuid, allocatedQuantity, allocationType, referenceType, referenceId,
                referenceUuid, referenceTitle, status, createdAt, createdByUserId, createdByUserUuid, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, generatedUuid)
            stmt.bindLong(2, entity.partId)
            stmt.bindText(3, partUuid)
            stmt.bindDouble(4, entity.allocatedQuantity)
            stmt.bindText(5, entity.allocationType)
            stmt.bindText(6, entity.referenceType)
            stmt.bindText(7, entity.referenceId)
            stmt.bindText(8, refUuid)
            stmt.bindText(9, entity.referenceTitle)
            stmt.bindText(10, entity.status)
            stmt.bindLong(11, entity.createdAt)
            stmt.bindText(12, entity.createdByUserId)
            stmt.bindText(13, userUuid)
            if (entity.notes != null) stmt.bindText(14, entity.notes) else stmt.bindNull(14)
            stmt.step()
        }
        conn.prepare("SELECT last_insert_rowid()").use { stmt ->
            if (stmt.step()) generatedId = stmt.getLong(0)
        }
        if (generatedId > 0) {
            runCatching {
                conn.prepare("UPDATE part_allocations SET id = ? WHERE (id IS NULL OR id = 0) AND rowid = ?").use { stmt ->
                    stmt.bindLong(1, generatedId)
                    stmt.bindLong(2, generatedId)
                    stmt.step()
                }
            }
        }
        return generatedId
    }

    /**
     * تحديث حالة الحجز إلى RELEASED عند فك الحجز.
     */
    fun releaseAllocation(id: Long): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE part_allocations
            SET status = 'RELEASED'
            WHERE (id = ? OR rowid = ?) AND status = 'ACTIVE'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, id)
            stmt.bindLong(2, id)
            stmt.step()
        }
        return true
    }

    /**
     * تحويل حالة الحجز آلياً إلى CONSUMED عند الصرف الفعلي للمخزون.
     */
    fun consumeAllocation(id: Long): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE part_allocations
            SET status = 'CONSUMED'
            WHERE (id = ? OR rowid = ?) AND status = 'ACTIVE'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, id)
            stmt.bindLong(2, id)
            stmt.step()
        }
        return true
    }

    /**
     * جلب سجل حجز فريد حسب معرّفه.
     */
    fun getAllocationById(id: Long): PartAllocationEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        var entity: PartAllocationEntity? = null
        conn.prepare("""
            SELECT COALESCE(NULLIF(id, 0), rowid) AS id,
                   partId, allocatedQuantity, allocationType, referenceType, referenceId,
                   referenceTitle, status, createdAt, createdByUserId, notes
            FROM part_allocations
            WHERE (id = ? OR rowid = ?)
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, id)
            stmt.bindLong(2, id)
            if (stmt.step()) {
                entity = mapAllocation(stmt)
            }
        }
        return entity
    }

    private fun mapAllocation(stmt: SQLiteStatement): PartAllocationEntity {
        return PartAllocationEntity(
            id = stmt.getLong(0),
            partId = stmt.getLong(1),
            allocatedQuantity = stmt.getDouble(2),
            allocationType = stmt.getText(3),
            referenceType = stmt.getText(4),
            referenceId = stmt.getText(5),
            referenceTitle = stmt.getText(6),
            status = stmt.getText(7),
            createdAt = stmt.getLong(8),
            createdByUserId = stmt.getText(9),
            notes = if (stmt.isNull(10)) null else stmt.getText(10)
        )
    }
}
