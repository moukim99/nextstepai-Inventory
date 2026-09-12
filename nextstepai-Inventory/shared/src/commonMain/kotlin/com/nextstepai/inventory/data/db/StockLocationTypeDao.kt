package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أنواع مواقع التخزين (StockLocationTypeDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockLocationTypeDao {

    private val selectColumns = """
        uuid, typeId, name, description, icon, customIcon, metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getAllLocationTypes(): List<StockLocationTypeEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockLocationTypeEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_location_types
            WHERE isDeleted = 0
            ORDER BY name ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            while (stmt.step()) {
                results.add(mapStockLocationTypeEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: StockLocationTypeEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_location_types (
                uuid, typeId, name, description, icon, customIcon, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.typeId)
            stmt.bindText(3, entity.name)
            stmt.bindText(4, entity.description)
            stmt.bindText(5, entity.icon)
            stmt.bindText(6, entity.customIcon)
            stmt.bindText(7, entity.metadata)
            stmt.bindText(8, entity.syncStatus.name)
            stmt.bindLong(9, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(10, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapStockLocationTypeEntity(stmt: SQLiteStatement): StockLocationTypeEntity {
        return StockLocationTypeEntity(
            uuid = stmt.getText(0),
            typeId = stmt.getLong(1),
            name = stmt.getText(2),
            description = stmt.getText(3),
            icon = stmt.getText(4),
            customIcon = stmt.getText(5),
            metadata = stmt.getText(6),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(7)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(8) != 0L,
            updatedAt = stmt.getLong(9)
        )
    }
}
