package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات تصنيفات القطع (PartCategoryDao) المعتمد بنسبة 100% على SQLite.
 */
@Dao
class PartCategoryDao {

    private val selectColumns = """
        uuid, id, name, parentId, parentUuid, description, structural, defaultLocationId, defaultLocationUuid,
        version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
    """.trimIndent()

    fun getAllCategories(): List<PartCategoryEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartCategoryEntity>()
        val sql = """
            SELECT $selectColumns
            FROM part_categories
            WHERE isDeleted = 0
            ORDER BY name ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            while (stmt.step()) {
                results.add(mapCategoryEntity(stmt))
            }
        }
        return results
    }

    fun getCategoryByUuid(uuid: String): PartCategoryEntity? {
        if (uuid.isBlank()) return null
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT $selectColumns
            FROM part_categories
            WHERE isDeleted = 0 AND uuid = ?
            LIMIT 1
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, uuid)
            if (stmt.step()) {
                return mapCategoryEntity(stmt)
            }
        }
        return null
    }

    fun getCategoryById(id: Long): PartCategoryEntity? {
        val paddedUuid = "cat-${id.toString().padStart(3, '0')}"
        val unpaddedUuid = "cat-$id"
        val direct = getCategoryByUuid(paddedUuid) ?: getCategoryByUuid(unpaddedUuid)
        if (direct != null) return direct

        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT $selectColumns
            FROM part_categories
            WHERE isDeleted = 0 AND (id = ? OR uuid = ? OR uuid = ? OR uuid LIKE ? OR uuid LIKE ?)
            LIMIT 1
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, id)
            stmt.bindText(2, paddedUuid)
            stmt.bindText(3, unpaddedUuid)
            stmt.bindText(4, "%-$id")
            stmt.bindText(5, "%-${id.toString().padStart(3, '0')}")
            if (stmt.step()) {
                return mapCategoryEntity(stmt)
            }
        }
        return null
    }

    fun insertOrUpdate(
        uuid: String,
        id: Long,
        name: String,
        parentId: Long? = null,
        parentUuid: String? = null,
        description: String = "",
        structural: Boolean = false,
        defaultLocationId: Long? = null,
        defaultLocationUuid: String? = null,
        syncStatus: SyncStatus = SyncStatus.PENDING
    ) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        val sql = """
            INSERT INTO part_categories (
                uuid, id, name, parentId, parentUuid, description, structural, defaultLocationId, defaultLocationUuid,
                version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 0, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                id = CASE WHEN excluded.id > 0 THEN excluded.id ELSE part_categories.id END,
                name = excluded.name,
                parentId = excluded.parentId,
                parentUuid = excluded.parentUuid,
                description = excluded.description,
                structural = excluded.structural,
                defaultLocationId = excluded.defaultLocationId,
                defaultLocationUuid = excluded.defaultLocationUuid,
                syncStatus = excluded.syncStatus,
                isDeleted = 0,
                updatedAt = excluded.updatedAt
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindLong(2, id)
            stmt.bindText(3, name)
            if (parentId != null) stmt.bindLong(4, parentId) else stmt.bindNull(4)
            if (parentUuid != null) stmt.bindText(5, parentUuid) else stmt.bindNull(5)
            stmt.bindText(6, description)
            stmt.bindLong(7, if (structural) 1L else 0L)
            if (defaultLocationId != null) stmt.bindLong(8, defaultLocationId) else stmt.bindNull(8)
            if (defaultLocationUuid != null) stmt.bindText(9, defaultLocationUuid) else stmt.bindNull(9)
            stmt.bindText(10, syncStatus.name)
            stmt.bindLong(11, now)
            stmt.step()
        }
    }

    fun deleteCategory(uuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        val sql = "UPDATE part_categories SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ? AND isDeleted = 0"
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
        var changes = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) changes = stmt.getLong(0).toInt()
        }
        return changes > 0
    }

    private fun mapCategoryEntity(stmt: SQLiteStatement): PartCategoryEntity {
        return PartCategoryEntity(
            uuid = stmt.getText(0),
            id = runCatching { stmt.getLong(1) }.getOrDefault(0L),
            name = stmt.getText(2),
            parentId = runCatching { if (stmt.isNull(3)) null else stmt.getLong(3) }.getOrNull(),
            parentUuid = if (stmt.isNull(4)) null else stmt.getText(4),
            description = stmt.getText(5),
            structural = stmt.getLong(6) != 0L,
            defaultLocationId = runCatching { if (stmt.isNull(7)) null else stmt.getLong(7) }.getOrNull(),
            defaultLocationUuid = if (stmt.isNull(8)) null else stmt.getText(8),
            version = stmt.getLong(9).toInt(),
            syncStatus = SyncStatus.fromString(stmt.getText(10)),
            isDeleted = stmt.getLong(11) != 0L,
            updatedAt = stmt.getLong(12),
            lastModifiedByDeviceUuid = if (stmt.isNull(13)) null else stmt.getText(13)
        )
    }
}
