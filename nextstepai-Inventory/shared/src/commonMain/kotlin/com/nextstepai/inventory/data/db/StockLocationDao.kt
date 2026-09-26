package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات مواقع التخزين (StockLocationDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockLocationDao {

    private val selectColumns = """
        uuid, locationId, name, description, parentId, parentUuid, structural, external,
        locationType, ownerId, icon, customIcon, address, customCapacity, isBulkGenerated, level, lft, rght, treeId,
        metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getAllLocations(): List<StockLocationEntity> {
        return runCatching {
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
            results.toList()
        }.getOrDefault(emptyList())
    }

    suspend fun insertOrUpdate(entity: StockLocationEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_locations (
                uuid, locationId, name, description, parentId, parentUuid, structural, external,
                locationType, ownerId, icon, customIcon, address, customCapacity, isBulkGenerated, level, lft, rght, treeId,
                metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            bindLocationEntity(stmt, entity)
            stmt.step()
        }
    }

    suspend fun insertBatchLocations(entities: List<StockLocationEntity>): Int {
        val conn = SqliteDatabaseManager.getConnection()
        var insertedCount = 0
        conn.prepare("""
            INSERT OR REPLACE INTO stock_locations (
                uuid, locationId, name, description, parentId, parentUuid, structural, external,
                locationType, ownerId, icon, customIcon, address, customCapacity, isBulkGenerated, level, lft, rght, treeId,
                metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            for (entity in entities) {
                bindLocationEntity(stmt, entity)
                stmt.step()
                stmt.reset()
                insertedCount++
            }
        }
        return insertedCount
    }

    suspend fun softDeleteLocation(locationId: Long, updatedAt: Long = Clock.System.now().toEpochMilliseconds()) {
        runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = """
                UPDATE stock_locations
                SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ?
                WHERE locationId = ? OR uuid = ?
            """.trimIndent()
            conn.prepare(sql).use { stmt ->
                stmt.bindLong(1, updatedAt)
                stmt.bindLong(2, locationId)
                stmt.bindText(3, "location-$locationId")
                stmt.step()
            }
        }
    }

    /**
     * استعلام لجلب عدد المواقع الرئيسية (Root/Sites) التي ليس لها موقع أب.
     * SELECT COUNT(*) FROM stock_locations WHERE parentUuid IS NULL AND isDeleted = 0
     */
    suspend fun getRootSitesCount(): Int {
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = "SELECT COUNT(*) FROM stock_locations WHERE parentUuid IS NULL AND isDeleted = 0"
            conn.prepare(sql).use { stmt ->
                if (stmt.step()) stmt.getLong(0).toInt() else 0
            }
        }.getOrDefault(0)
    }

    /**
     * استعلام لجلب عدد المستودعات التابعة لموقع معين أو إجمالي المستودعات.
     * SELECT COUNT(*) FROM stock_locations WHERE locationType = 'WAREHOUSE' AND isDeleted = 0
     */
    suspend fun getWarehouseCount(parentUuid: String? = null): Int {
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = if (parentUuid != null) {
                "SELECT COUNT(*) FROM stock_locations WHERE locationType = 'WAREHOUSE' AND parentUuid = ? AND isDeleted = 0"
            } else {
                "SELECT COUNT(*) FROM stock_locations WHERE locationType = 'WAREHOUSE' AND isDeleted = 0"
            }
            conn.prepare(sql).use { stmt ->
                if (parentUuid != null) stmt.bindText(1, parentUuid)
                if (stmt.step()) stmt.getLong(0).toInt() else 0
            }
        }.getOrDefault(0)
    }

    /**
     * دالة للتحقق من وجود موقع/مستودع أساسي حالي مسجل مسبقاً (findPrimaryLocation(type)).
     */
    suspend fun findPrimaryLocation(type: String): StockLocationEntity? {
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val isExternal = type.equals("EXTERNAL", ignoreCase = true)
            val isSite = type.equals("SITE", ignoreCase = true) || type.equals("ROOT", ignoreCase = true)

            val sql = when {
                isExternal -> "SELECT $selectColumns FROM stock_locations WHERE external = 1 AND isDeleted = 0 AND metadata LIKE '%\"isPrimary\":true%' ORDER BY updatedAt DESC LIMIT 1"
                isSite -> "SELECT $selectColumns FROM stock_locations WHERE (locationType = 'SITE' OR parentUuid IS NULL) AND external = 0 AND isDeleted = 0 AND metadata LIKE '%\"isPrimary\":true%' ORDER BY updatedAt DESC LIMIT 1"
                else -> "SELECT $selectColumns FROM stock_locations WHERE locationType = ? AND external = 0 AND isDeleted = 0 AND metadata LIKE '%\"isPrimary\":true%' ORDER BY updatedAt DESC LIMIT 1"
            }
            conn.prepare(sql).use { stmt ->
                if (!isExternal && !isSite) {
                    stmt.bindText(1, type)
                }
                if (stmt.step()) {
                    mapStockLocationEntity(stmt)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun bindLocationEntity(stmt: SQLiteStatement, entity: StockLocationEntity) {
        stmt.bindText(1, entity.uuid)
        stmt.bindLong(2, entity.locationId)
        stmt.bindText(3, entity.name)
        stmt.bindText(4, entity.description)
        if (entity.parentId != null) stmt.bindLong(5, entity.parentId) else stmt.bindNull(5)
        if (entity.parentUuid != null) stmt.bindText(6, entity.parentUuid) else stmt.bindNull(6)
        stmt.bindLong(7, if (entity.structural) 1L else 0L)
        stmt.bindLong(8, if (entity.external) 1L else 0L)
        stmt.bindText(9, entity.locationType)
        if (entity.ownerId != null) stmt.bindLong(10, entity.ownerId) else stmt.bindNull(10)
        stmt.bindText(11, entity.icon)
        stmt.bindText(12, entity.customIcon)
        stmt.bindText(13, entity.address)
        if (entity.customCapacity != null) stmt.bindDouble(14, entity.customCapacity) else stmt.bindNull(14)
        stmt.bindLong(15, if (entity.isBulkGenerated) 1L else 0L)
        stmt.bindLong(16, entity.level.toLong())
        stmt.bindLong(17, entity.lft.toLong())
        stmt.bindLong(18, entity.rght.toLong())
        stmt.bindLong(19, entity.treeId.toLong())
        stmt.bindText(20, entity.metadata)
        stmt.bindText(21, entity.syncStatus.name)
        stmt.bindLong(22, if (entity.isDeleted) 1L else 0L)
        stmt.bindLong(23, entity.updatedAt)
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
            locationType = runCatching { stmt.getText(8) }.getOrDefault("SHELF"),
            ownerId = runCatching { if (stmt.isNull(9)) null else stmt.getLong(9) }.getOrNull(),
            icon = runCatching { stmt.getText(10) }.getOrDefault("warehouse"),
            customIcon = runCatching { stmt.getText(11) }.getOrDefault(""),
            address = runCatching { stmt.getText(12) }.getOrDefault(""),
            customCapacity = runCatching { if (stmt.isNull(13)) null else stmt.getDouble(13) }.getOrNull(),
            isBulkGenerated = runCatching { stmt.getLong(14) != 0L }.getOrDefault(false),
            level = runCatching { stmt.getLong(15).toInt() }.getOrDefault(0),
            lft = runCatching { stmt.getLong(16).toInt() }.getOrDefault(0),
            rght = runCatching { stmt.getLong(17).toInt() }.getOrDefault(0),
            treeId = runCatching { stmt.getLong(18).toInt() }.getOrDefault(1),
            metadata = runCatching { stmt.getText(19) }.getOrDefault("{}"),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(20)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(21) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(22) }.getOrDefault(0L)
        )
    }
}
