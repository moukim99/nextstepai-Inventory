package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات مواقع التخزين (StockLocationDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockLocationDao {

    private val selectColumns = """
        uuid, locationId, name, description, parentId, parentUuid, structural, external,
        locationTypeId, ownerId, icon, customIcon, level, lft, rght, treeId,
        metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getAllLocations(): List<StockLocationEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockLocationEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_locations
            WHERE isDeleted = 0
            ORDER BY treeId ASC, level ASC, name ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            while (stmt.step()) {
                results.add(mapStockLocationEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: StockLocationEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_locations (
                uuid, locationId, name, description, parentId, parentUuid, structural, external,
                locationTypeId, ownerId, icon, customIcon, level, lft, rght, treeId,
                metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.locationId)
            stmt.bindText(3, entity.name)
            stmt.bindText(4, entity.description)
            if (entity.parentId != null) stmt.bindLong(5, entity.parentId) else stmt.bindNull(5)
            if (entity.parentUuid != null) stmt.bindText(6, entity.parentUuid) else stmt.bindNull(6)
            stmt.bindLong(7, if (entity.structural) 1L else 0L)
            stmt.bindLong(8, if (entity.external) 1L else 0L)
            if (entity.locationTypeId != null) stmt.bindLong(9, entity.locationTypeId) else stmt.bindNull(9)
            if (entity.ownerId != null) stmt.bindLong(10, entity.ownerId) else stmt.bindNull(10)
            stmt.bindText(11, entity.icon)
            stmt.bindText(12, entity.customIcon)
            stmt.bindLong(13, entity.level.toLong())
            stmt.bindLong(14, entity.lft.toLong())
            stmt.bindLong(15, entity.rght.toLong())
            stmt.bindLong(16, entity.treeId.toLong())
            stmt.bindText(17, entity.metadata)
            stmt.bindText(18, entity.syncStatus.name)
            stmt.bindLong(19, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(20, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapStockLocationEntity(stmt: SQLiteStatement): StockLocationEntity {
        return StockLocationEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            locationId = runCatching { stmt.getLong(1) }.getOrDefault(0L),
            name = runCatching { stmt.getText(2) }.getOrDefault(""),
            description = runCatching { stmt.getText(3) }.getOrDefault(""),
            parentId = runCatching { if (stmt.isNull(4)) null else stmt.getLong(4) }.getOrNull(),
            parentUuid = runCatching { if (stmt.isNull(5)) null else stmt.getText(5) }.getOrNull(),
            structural = runCatching { stmt.getLong(6) != 0L }.getOrDefault(false),
            external = runCatching { stmt.getLong(7) != 0L }.getOrDefault(false),
            locationTypeId = runCatching { if (stmt.isNull(8)) null else stmt.getLong(8) }.getOrNull(),
            ownerId = runCatching { if (stmt.isNull(9)) null else stmt.getLong(9) }.getOrNull(),
            icon = runCatching { stmt.getText(10) }.getOrDefault("warehouse"),
            customIcon = runCatching { stmt.getText(11) }.getOrDefault(""),
            level = runCatching { stmt.getLong(12).toInt() }.getOrDefault(0),
            lft = runCatching { stmt.getLong(13).toInt() }.getOrDefault(0),
            rght = runCatching { stmt.getLong(14).toInt() }.getOrDefault(0),
            treeId = runCatching { stmt.getLong(15).toInt() }.getOrDefault(1),
            metadata = runCatching { stmt.getText(16) }.getOrDefault("{}"),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(17)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(18) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(19) }.getOrDefault(0L)
        )
    }
}
