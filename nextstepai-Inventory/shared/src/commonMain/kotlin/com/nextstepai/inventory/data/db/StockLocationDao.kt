package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات مواقع التخزين (StockLocationDao) المعتمد بنسبة 100% على UUIDv7.
 */
@Dao
class StockLocationDao {

    private val selectColumns = """
        uuid, name, description, parentUuid, structural, external, locationTypeUuid,
        locationType, customCapacity, isBulkGenerated, address, icon, customIcon, level,
        lft, rght, treeId, metadata, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
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

    suspend fun getLocationByUuid(uuid: String): StockLocationEntity? {
        if (uuid.isBlank()) return null
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = "SELECT $selectColumns FROM stock_locations WHERE uuid = ? AND isDeleted = 0 LIMIT 1"
            conn.prepare(sql).use { stmt ->
                stmt.bindText(1, uuid)
                if (stmt.step()) mapStockLocationEntity(stmt) else null
            }
        }.getOrNull()
    }

    suspend fun insertOrUpdate(entity: StockLocationEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT INTO stock_locations (
                uuid, name, description, parentUuid, structural, external, locationTypeUuid,
                locationType, customCapacity, isBulkGenerated, address, icon, customIcon, level,
                lft, rght, treeId, metadata, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                description = excluded.description,
                parentUuid = excluded.parentUuid,
                structural = excluded.structural,
                external = excluded.external,
                locationTypeUuid = excluded.locationTypeUuid,
                locationType = excluded.locationType,
                customCapacity = excluded.customCapacity,
                isBulkGenerated = excluded.isBulkGenerated,
                address = excluded.address,
                icon = excluded.icon,
                customIcon = excluded.customIcon,
                level = excluded.level,
                lft = excluded.lft,
                rght = excluded.rght,
                treeId = excluded.treeId,
                metadata = excluded.metadata,
                version = stock_locations.version + 1,
                syncStatus = excluded.syncStatus,
                isDeleted = excluded.isDeleted,
                updatedAt = excluded.updatedAt,
                lastModifiedByDeviceUuid = excluded.lastModifiedByDeviceUuid
        """.trimIndent()).use { stmt ->
            bindLocationEntity(stmt, entity)
            stmt.step()
        }
    }

    suspend fun insertBatchLocations(entities: List<StockLocationEntity>): Int {
        val conn = SqliteDatabaseManager.getConnection()
        var insertedCount = 0
        conn.prepare("""
            INSERT INTO stock_locations (
                uuid, name, description, parentUuid, structural, external, locationTypeUuid,
                locationType, customCapacity, isBulkGenerated, address, icon, customIcon, level,
                lft, rght, treeId, metadata, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                description = excluded.description,
                parentUuid = excluded.parentUuid,
                structural = excluded.structural,
                external = excluded.external,
                locationTypeUuid = excluded.locationTypeUuid,
                locationType = excluded.locationType,
                customCapacity = excluded.customCapacity,
                isBulkGenerated = excluded.isBulkGenerated,
                address = excluded.address,
                icon = excluded.icon,
                customIcon = excluded.customIcon,
                level = excluded.level,
                lft = excluded.lft,
                rght = excluded.rght,
                treeId = excluded.treeId,
                metadata = excluded.metadata,
                version = stock_locations.version + 1,
                syncStatus = excluded.syncStatus,
                isDeleted = excluded.isDeleted,
                updatedAt = excluded.updatedAt,
                lastModifiedByDeviceUuid = excluded.lastModifiedByDeviceUuid
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

    suspend fun softDeleteLocation(uuid: String, updatedAt: Long = Clock.System.now().toEpochMilliseconds()) {
        runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = """
                UPDATE stock_locations
                SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ?
                WHERE uuid = ?
            """.trimIndent()
            conn.prepare(sql).use { stmt ->
                stmt.bindLong(1, updatedAt)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    suspend fun getRootSitesCount(): Int {
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = "SELECT COUNT(*) FROM stock_locations WHERE parentUuid IS NULL AND isDeleted = 0"
            conn.prepare(sql).use { stmt ->
                if (stmt.step()) stmt.getLong(0).toInt() else 0
            }
        }.getOrDefault(0)
    }

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
        stmt.bindText(2, entity.name)
        stmt.bindText(3, entity.description)
        if (entity.parentUuid != null) stmt.bindText(4, entity.parentUuid) else stmt.bindNull(4)
        stmt.bindLong(5, if (entity.structural) 1L else 0L)
        stmt.bindLong(6, if (entity.external) 1L else 0L)
        if (entity.locationTypeUuid != null) stmt.bindText(7, entity.locationTypeUuid) else stmt.bindNull(7)
        stmt.bindText(8, entity.locationType)
        if (entity.customCapacity != null) stmt.bindDouble(9, entity.customCapacity) else stmt.bindNull(9)
        stmt.bindLong(10, if (entity.isBulkGenerated) 1L else 0L)
        stmt.bindText(11, entity.address)
        stmt.bindText(12, entity.icon)
        stmt.bindText(13, entity.customIcon)
        stmt.bindLong(14, entity.level.toLong())
        stmt.bindLong(15, entity.lft.toLong())
        stmt.bindLong(16, entity.rght.toLong())
        stmt.bindLong(17, entity.treeId.toLong())
        stmt.bindText(18, entity.metadata)
        stmt.bindLong(19, entity.version.toLong())
        stmt.bindText(20, entity.syncStatus.name)
        stmt.bindLong(21, if (entity.isDeleted) 1L else 0L)
        stmt.bindLong(22, entity.updatedAt)
        if (entity.lastModifiedByDeviceUuid != null) stmt.bindText(23, entity.lastModifiedByDeviceUuid) else stmt.bindNull(23)
    }

    private fun mapStockLocationEntity(stmt: SQLiteStatement): StockLocationEntity {
        return StockLocationEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            name = runCatching { stmt.getText(1) }.getOrDefault(""),
            description = runCatching { stmt.getText(2) }.getOrDefault(""),
            parentUuid = runCatching { if (stmt.isNull(3)) null else stmt.getText(3) }.getOrNull(),
            structural = runCatching { stmt.getLong(4) != 0L }.getOrDefault(false),
            external = runCatching { stmt.getLong(5) != 0L }.getOrDefault(false),
            locationTypeUuid = runCatching { if (stmt.isNull(6)) null else stmt.getText(6) }.getOrNull(),
            locationType = runCatching { stmt.getText(7) }.getOrDefault("SHELF"),
            customCapacity = runCatching { if (stmt.isNull(8)) null else stmt.getDouble(8) }.getOrNull(),
            isBulkGenerated = runCatching { stmt.getLong(9) != 0L }.getOrDefault(false),
            address = runCatching { stmt.getText(10) }.getOrDefault(""),
            icon = runCatching { stmt.getText(11) }.getOrDefault("warehouse"),
            customIcon = runCatching { stmt.getText(12) }.getOrDefault(""),
            level = runCatching { stmt.getLong(13).toInt() }.getOrDefault(0),
            lft = runCatching { stmt.getLong(14).toInt() }.getOrDefault(0),
            rght = runCatching { stmt.getLong(15).toInt() }.getOrDefault(0),
            treeId = runCatching { stmt.getLong(16).toInt() }.getOrDefault(1),
            metadata = runCatching { stmt.getText(17) }.getOrDefault("{}"),
            version = runCatching { stmt.getLong(18).toInt() }.getOrDefault(1),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(19)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(20) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(21) }.getOrDefault(0L),
            lastModifiedByDeviceUuid = runCatching { if (stmt.isNull(22)) null else stmt.getText(22) }.getOrNull()
        )
    }
}
