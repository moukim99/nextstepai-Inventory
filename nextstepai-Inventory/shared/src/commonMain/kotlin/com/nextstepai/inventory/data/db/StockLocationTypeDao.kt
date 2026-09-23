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
        uuid, typeId, name, description, icon, customIcon, length, width, height, maxWeight, maxVolume, metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getAllLocationTypes(): List<StockLocationTypeEntity> {
        return runCatching {
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
            results.toList()
        }.getOrDefault(emptyList())
    }

    suspend fun insertOrUpdate(entity: StockLocationTypeEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_location_types (
                uuid, typeId, name, description, icon, customIcon, length, width, height, maxWeight, maxVolume, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.typeId)
            stmt.bindText(3, entity.name)
            stmt.bindText(4, entity.description)
            stmt.bindText(5, entity.icon)
            stmt.bindText(6, entity.customIcon)
            stmt.bindDouble(7, entity.length)
            stmt.bindDouble(8, entity.width)
            stmt.bindDouble(9, entity.height)
            stmt.bindDouble(10, entity.maxWeight)
            stmt.bindDouble(11, entity.maxVolume)
            stmt.bindText(12, entity.metadata)
            stmt.bindText(13, entity.syncStatus.name)
            stmt.bindLong(14, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(15, entity.updatedAt)
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
            length = runCatching { stmt.getDouble(6) }.getOrDefault(0.0),
            width = runCatching { stmt.getDouble(7) }.getOrDefault(0.0),
            height = runCatching { stmt.getDouble(8) }.getOrDefault(0.0),
            maxWeight = runCatching { stmt.getDouble(9) }.getOrDefault(0.0),
            maxVolume = runCatching { stmt.getDouble(10) }.getOrDefault(0.0),
            metadata = stmt.getText(11),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(12)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(13) != 0L,
            updatedAt = stmt.getLong(14)
        )
    }
}
