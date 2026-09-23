package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات القطع (PartDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 * يُحفظ ويُستعلم بشكل دائم ببارامترات ربط مميكنة لمنع التجميع اليدوي للنصوص SQL.
 */
@Dao
class PartDao {

    private val selectColumns = """
        uuid, id, name, ipn, description, revision, keywords, categoryId, units,
        assembly, component, isTemplate, variantOfId, trackable, purchaseable,
        salable, virtual, active, locked, minimumStock, maximumStock,
        defaultLocationId, defaultExpiryDays, totalInStock, localImagePath, link,
        syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getPartsPaged(limit: Int = 20, offset: Int = 0): List<PartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartEntity>()
        conn.prepare("""
            SELECT $selectColumns
            FROM parts
            WHERE isDeleted = 0
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, limit.toLong())
            stmt.bindLong(2, offset.toLong())
            while (stmt.step()) {
                results.add(mapPartEntity(stmt))
            }
        }
        return results
    }

    suspend fun getParentAssemblies(): List<PartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartEntity>()
        conn.prepare("""
            SELECT $selectColumns
            FROM parts
            WHERE assembly = 1 AND isDeleted = 0 AND active = 1
            ORDER BY name ASC
        """.trimIndent()).use { stmt ->
            while (stmt.step()) {
                results.add(mapPartEntity(stmt))
            }
        }
        return results
    }

    suspend fun getEligibleSubParts(parentPartId: Long): List<PartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartEntity>()
        conn.prepare("""
            SELECT $selectColumns
            FROM parts
            WHERE component = 1
              AND id != ?
              AND id NOT IN (SELECT subPartId FROM bom_items WHERE partId = ? AND isDeleted = 0)
              AND isDeleted = 0
              AND active = 1
            ORDER BY name ASC
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, parentPartId)
            stmt.bindLong(2, parentPartId)
            while (stmt.step()) {
                results.add(mapPartEntity(stmt))
            }
        }
        return results
    }

    suspend fun getPendingSyncParts(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<PartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartEntity>()
        conn.prepare("""
            SELECT $selectColumns
            FROM parts
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapPartEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: PartEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO parts (
                uuid, id, name, ipn, description, revision, keywords, categoryId, units,
                assembly, component, isTemplate, variantOfId, trackable, purchaseable,
                salable, virtual, active, locked, minimumStock, maximumStock,
                defaultLocationId, defaultExpiryDays, totalInStock, localImagePath, link,
                syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.id)
            stmt.bindText(3, entity.name)
            stmt.bindText(4, entity.ipn)
            stmt.bindText(5, entity.description)
            stmt.bindText(6, entity.revision)
            stmt.bindText(7, entity.keywords)
            if (entity.categoryId != null) stmt.bindLong(8, entity.categoryId) else stmt.bindNull(8)
            stmt.bindText(9, entity.units)
            stmt.bindLong(10, if (entity.assembly) 1L else 0L)
            stmt.bindLong(11, if (entity.component) 1L else 0L)
            stmt.bindLong(12, if (entity.isTemplate) 1L else 0L)
            if (entity.variantOfId != null) stmt.bindLong(13, entity.variantOfId) else stmt.bindNull(13)
            stmt.bindLong(14, if (entity.trackable) 1L else 0L)
            stmt.bindLong(15, if (entity.purchaseable) 1L else 0L)
            stmt.bindLong(16, if (entity.salable) 1L else 0L)
            stmt.bindLong(17, if (entity.virtual) 1L else 0L)
            stmt.bindLong(18, if (entity.active) 1L else 0L)
            stmt.bindLong(19, if (entity.locked) 1L else 0L)
            stmt.bindDouble(20, entity.minimumStock)
            if (entity.maximumStock != null) stmt.bindDouble(21, entity.maximumStock) else stmt.bindNull(21)
            if (entity.defaultLocationId != null) stmt.bindLong(22, entity.defaultLocationId) else stmt.bindNull(22)
            if (entity.defaultExpiryDays != null) stmt.bindLong(23, entity.defaultExpiryDays.toLong()) else stmt.bindNull(23)
            stmt.bindDouble(24, entity.totalInStock)
            if (entity.localImagePath != null) stmt.bindText(25, entity.localImagePath) else stmt.bindNull(25)
            stmt.bindText(26, entity.link)
            stmt.bindText(27, entity.syncStatus.name)
            stmt.bindLong(28, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(29, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE parts SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    suspend fun addStockToPart(partId: Long, qty: Double) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("UPDATE parts SET totalInStock = totalInStock + ?, updatedAt = ? WHERE id = ?").use { stmt ->
            stmt.bindDouble(1, qty)
            stmt.bindLong(2, Clock.System.now().toEpochMilliseconds())
            stmt.bindLong(3, partId)
            stmt.step()
        }
    }

    suspend fun getActiveCount(): Int {
        val conn = SqliteDatabaseManager.getConnection()
        var count = 0
        conn.prepare("SELECT COUNT(*) FROM parts WHERE isDeleted = 0").use { stmt ->
            if (stmt.step()) {
                count = stmt.getLong(0).toInt()
            }
        }
        return count
    }

    private fun mapPartEntity(stmt: SQLiteStatement): PartEntity {
        return PartEntity(
            uuid = stmt.getText(0),
            id = stmt.getLong(1),
            name = stmt.getText(2),
            ipn = stmt.getText(3),
            description = stmt.getText(4),
            revision = stmt.getText(5),
            keywords = stmt.getText(6),
            categoryId = if (stmt.isNull(7)) null else stmt.getLong(7),
            units = stmt.getText(8),
            assembly = stmt.getLong(9) != 0L,
            component = stmt.getLong(10) != 0L,
            isTemplate = stmt.getLong(11) != 0L,
            variantOfId = if (stmt.isNull(12)) null else stmt.getLong(12),
            trackable = stmt.getLong(13) != 0L,
            purchaseable = stmt.getLong(14) != 0L,
            salable = stmt.getLong(15) != 0L,
            virtual = stmt.getLong(16) != 0L,
            active = stmt.getLong(17) != 0L,
            locked = stmt.getLong(18) != 0L,
            minimumStock = stmt.getDouble(19),
            maximumStock = if (stmt.isNull(20)) null else stmt.getDouble(20),
            defaultLocationId = if (stmt.isNull(21)) null else stmt.getLong(21),
            defaultExpiryDays = if (stmt.isNull(22)) null else stmt.getLong(22).toInt(),
            totalInStock = stmt.getDouble(23),
            localImagePath = if (stmt.isNull(24)) null else stmt.getText(24),
            link = stmt.getText(25),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(26)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(27) != 0L,
            updatedAt = stmt.getLong(28)
        )
    }
}
