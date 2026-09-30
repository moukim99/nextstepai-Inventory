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
    suspend fun getActiveAllocationsForPart(partId: Long): List<PartAllocationEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartAllocationEntity>()
        conn.prepare("""
            SELECT id, partId, allocatedQuantity, allocationType, referenceType, referenceId,
                   referenceTitle, status, createdAt, createdByUserId, notes
            FROM part_allocations
            WHERE partId = ? AND status = 'ACTIVE'
            ORDER BY createdAt DESC
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            while (stmt.step()) {
                results.add(mapAllocation(stmt))
            }
        }
        return results
    }

    /**
     * جلب كافة الحجوزات (النشطة والمحررة والمستهلكة) لقطعة محددة.
     */
    suspend fun getAllAllocationsForPart(partId: Long): List<PartAllocationEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartAllocationEntity>()
        conn.prepare("""
            SELECT id, partId, allocatedQuantity, allocationType, referenceType, referenceId,
                   referenceTitle, status, createdAt, createdByUserId, notes
            FROM part_allocations
            WHERE partId = ?
            ORDER BY createdAt DESC
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            while (stmt.step()) {
                results.add(mapAllocation(stmt))
            }
        }
        return results
    }

    /**
     * حساب إجمالي الكمية المحجوزة حجزاً مؤكداً (HARD Committed) لقطعة محددة.
     */
    suspend fun getTotalCommittedQuantity(partId: Long): Double {
        val conn = SqliteDatabaseManager.getConnection()
        var total = 0.0
        conn.prepare("""
            SELECT SUM(allocatedQuantity)
            FROM part_allocations
            WHERE partId = ? AND status = 'ACTIVE' AND allocationType = 'HARD'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            if (stmt.step() && !stmt.isNull(0)) {
                total = stmt.getDouble(0)
            }
        }
        return total
    }

    /**
     * حساب إجمالي الكمية المحجوزة حجزاً مبدئياً (SOFT Reserved) لقطعة محددة.
     */
    suspend fun getTotalSoftQuantity(partId: Long): Double {
        val conn = SqliteDatabaseManager.getConnection()
        var total = 0.0
        conn.prepare("""
            SELECT SUM(allocatedQuantity)
            FROM part_allocations
            WHERE partId = ? AND status = 'ACTIVE' AND allocationType = 'SOFT'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, partId)
            if (stmt.step() && !stmt.isNull(0)) {
                total = stmt.getDouble(0)
            }
        }
        return total
    }

    /**
     * إدراج سجل حجز جديد في القاعدة وتوليد معرف فريد.
     */
    suspend fun insertAllocation(entity: PartAllocationEntity): Long {
        val conn = SqliteDatabaseManager.getConnection()
        var generatedId = 0L
        conn.prepare("""
            INSERT INTO part_allocations (
                partId, allocatedQuantity, allocationType, referenceType, referenceId,
                referenceTitle, status, createdAt, createdByUserId, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, entity.partId)
            stmt.bindDouble(2, entity.allocatedQuantity)
            stmt.bindText(3, entity.allocationType)
            stmt.bindText(4, entity.referenceType)
            stmt.bindText(5, entity.referenceId)
            stmt.bindText(6, entity.referenceTitle)
            stmt.bindText(7, entity.status)
            stmt.bindLong(8, entity.createdAt)
            stmt.bindText(9, entity.createdByUserId)
            if (entity.notes != null) stmt.bindText(10, entity.notes) else stmt.bindNull(10)
            stmt.step()
        }
        conn.prepare("SELECT last_insert_rowid()").use { stmt ->
            if (stmt.step()) generatedId = stmt.getLong(0)
        }
        return generatedId
    }

    /**
     * تحديث حالة الحجز إلى RELEASED عند فك الحجز.
     */
    suspend fun releaseAllocation(id: Long): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE part_allocations
            SET status = 'RELEASED'
            WHERE id = ? AND status = 'ACTIVE'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, id)
            stmt.step()
        }
        return true
    }

    /**
     * تحويل حالة الحجز آلياً إلى CONSUMED عند الصرف الفعلي للمخزون.
     */
    suspend fun consumeAllocation(id: Long): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE part_allocations
            SET status = 'CONSUMED'
            WHERE id = ? AND status = 'ACTIVE'
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, id)
            stmt.step()
        }
        return true
    }

    /**
     * جلب سجل حجز فريد حسب معرّفه.
     */
    suspend fun getAllocationById(id: Long): PartAllocationEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        var entity: PartAllocationEntity? = null
        conn.prepare("""
            SELECT id, partId, allocatedQuantity, allocationType, referenceType, referenceId,
                   referenceTitle, status, createdAt, createdByUserId, notes
            FROM part_allocations
            WHERE id = ?
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, id)
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
