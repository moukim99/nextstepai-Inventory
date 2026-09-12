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
        uuid, locationId, name, description, parentId, structural, external,
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
                uuid, locationId, name, description, parentId, structural, external,
                locationTypeId, ownerId, icon, customIcon, level, lft, rght, treeId,
                metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.locationId)
            stmt.bindText(3, entity.name)
            stmt.bindText(4, entity.description)
            if (entity.parentId != null) stmt.bindLong(5, entity.parentId) else stmt.bindNull(5)
            stmt.bindLong(6, if (entity.structural) 1L else 0L)
            stmt.bindLong(7, if (entity.external) 1L else 0L)
            if (entity.locationTypeId != null) stmt.bindLong(8, entity.locationTypeId) else stmt.bindNull(8)
            if (entity.ownerId != null) stmt.bindLong(9, entity.ownerId) else stmt.bindNull(9)
            stmt.bindText(10, entity.icon)
            stmt.bindText(11, entity.customIcon)
            stmt.bindLong(12, entity.level.toLong())
            stmt.bindLong(13, entity.lft.toLong())
            stmt.bindLong(14, entity.rght.toLong())
            stmt.bindLong(15, entity.treeId.toLong())
            stmt.bindText(16, entity.metadata)
            stmt.bindText(17, entity.syncStatus.name)
            stmt.bindLong(18, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(19, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapStockLocationEntity(stmt: SQLiteStatement): StockLocationEntity {
        return StockLocationEntity(
            uuid = stmt.getText(0),
            locationId = stmt.getLong(1),
            name = stmt.getText(2),
            description = stmt.getText(3),
            parentId = if (stmt.isNull(4)) null else stmt.getLong(4),
            structural = stmt.getLong(5) != 0L,
            external = stmt.getLong(6) != 0L,
            locationTypeId = if (stmt.isNull(7)) null else stmt.getLong(7),
            ownerId = if (stmt.isNull(8)) null else stmt.getLong(8),
            icon = stmt.getText(9),
            customIcon = stmt.getText(10),
            level = stmt.getLong(11).toInt(),
            lft = stmt.getLong(12).toInt(),
            rght = stmt.getLong(13).toInt(),
            treeId = stmt.getLong(14).toInt(),
            metadata = stmt.getText(15),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(16)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(17) != 0L,
            updatedAt = stmt.getLong(18)
        )
    }
}
