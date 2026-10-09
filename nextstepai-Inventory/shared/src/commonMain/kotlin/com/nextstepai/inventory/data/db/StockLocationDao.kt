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
        uuid, id, parentId, ownerId, name, description, parentUuid, structural, external, locationTypeUuid,
        locationType, customCapacity, isBulkGenerated, address, icon, customIcon, level,
        lft, rght, treeId, metadata, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
    """.trimIndent()

    fun getAllLocations(): List<StockLocationEntity> {
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

    fun getLocationByUuid(uuid: String): StockLocationEntity? {
        if (uuid.isBlank()) return null
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val numericSuffix = uuid.removePrefix("location-").removePrefix("loc-").toLongOrNull()
            val paddedUuid = numericSuffix?.let { "loc-${it.toString().padStart(3, '0')}" } ?: uuid
            val shortUuid = numericSuffix?.let { "loc-$it" } ?: uuid
            val sql = "SELECT $selectColumns FROM stock_locations WHERE (uuid = ? OR uuid = ? OR uuid = ?) AND isDeleted = 0 LIMIT 1"
            conn.prepare(sql).use { stmt ->
                stmt.bindText(1, uuid)
                stmt.bindText(2, paddedUuid)
                stmt.bindText(3, shortUuid)
                if (stmt.step()) mapStockLocationEntity(stmt) else null
            }
        }.getOrNull()
    }

    fun getLocationById(id: Long): StockLocationEntity? {
        if (id <= 0L) return null
        val formatted = id.toString()
        val padded = formatted.padStart(3, '0')
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = "SELECT $selectColumns FROM stock_locations WHERE (id = ? OR locationId = ? OR uuid = ? OR uuid = ? OR uuid = ? OR uuid = ?) AND isDeleted = 0 LIMIT 1"
            conn.prepare(sql).use { stmt ->
                stmt.bindLong(1, id)
                stmt.bindLong(2, id)
                stmt.bindText(3, "loc-$formatted")
                stmt.bindText(4, "loc-$padded")
                stmt.bindText(5, "location-$formatted")
                stmt.bindText(6, "location-$padded")
                if (stmt.step()) mapStockLocationEntity(stmt) else null
            }
        }.getOrNull() ?: getAllLocations().firstOrNull {
            it.id == id ||
            it.uuid == "loc-$formatted" ||
            it.uuid == "loc-$padded" ||
            it.uuid == "location-$formatted" ||
            it.uuid == "location-$padded" ||
            it.uuid.removePrefix("loc-").removePrefix("location-").toLongOrNull() == id
        }
    }

    fun insertOrUpdate(entity: StockLocationEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT INTO stock_locations (
                uuid, id, locationId, parentId, ownerId, name, description, parentUuid, structural, external, locationTypeUuid,
                locationType, customCapacity, isBulkGenerated, address, icon, customIcon, level,
                lft, rght, treeId, metadata, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                id = CASE WHEN excluded.id > 0 THEN excluded.id ELSE stock_locations.id END,
                locationId = CASE WHEN excluded.locationId > 0 THEN excluded.locationId ELSE stock_locations.locationId END,
                parentId = CASE WHEN excluded.parentId IS NOT NULL THEN excluded.parentId ELSE stock_locations.parentId END,
                ownerId = CASE WHEN excluded.ownerId IS NOT NULL THEN excluded.ownerId ELSE stock_locations.ownerId END,
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

    fun insertBatchLocations(entities: List<StockLocationEntity>): Int {
        val conn = SqliteDatabaseManager.getConnection()
        var insertedCount = 0
        conn.prepare("""
            INSERT INTO stock_locations (
                uuid, id, locationId, parentId, ownerId, name, description, parentUuid, structural, external, locationTypeUuid,
                locationType, customCapacity, isBulkGenerated, address, icon, customIcon, level,
                lft, rght, treeId, metadata, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                id = CASE WHEN excluded.id > 0 THEN excluded.id ELSE stock_locations.id END,
                locationId = CASE WHEN excluded.locationId > 0 THEN excluded.locationId ELSE stock_locations.locationId END,
                parentId = CASE WHEN excluded.parentId IS NOT NULL THEN excluded.parentId ELSE stock_locations.parentId END,
                ownerId = CASE WHEN excluded.ownerId IS NOT NULL THEN excluded.ownerId ELSE stock_locations.ownerId END,
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

    fun softDeleteLocation(uuid: String, updatedAt: Long = Clock.System.now().toEpochMilliseconds()) {
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

    fun getRootSitesCount(): Int {
        return runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val sql = "SELECT COUNT(*) FROM stock_locations WHERE parentUuid IS NULL AND isDeleted = 0"
            conn.prepare(sql).use { stmt ->
                if (stmt.step()) stmt.getLong(0).toInt() else 0
            }
        }.getOrDefault(0)
    }

    fun getWarehouseCount(parentUuid: String? = null): Int {
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

    fun findPrimaryLocation(type: String): StockLocationEntity? {
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
        val locId = if (entity.id > 0L) entity.id else {
            entity.uuid.removePrefix("location-").removePrefix("loc-").toLongOrNull() ?: 0L
        }
        stmt.bindText(1, entity.uuid)
        stmt.bindLong(2, locId)
        stmt.bindLong(3, locId)
        if (entity.parentId != null) stmt.bindLong(4, entity.parentId) else stmt.bindNull(4)
        if (entity.ownerId != null) stmt.bindLong(5, entity.ownerId) else stmt.bindNull(5)
        stmt.bindText(6, entity.name)
        stmt.bindText(7, entity.description)
        if (entity.parentUuid != null) stmt.bindText(8, entity.parentUuid) else stmt.bindNull(8)
        stmt.bindLong(9, if (entity.structural) 1L else 0L)
        stmt.bindLong(10, if (entity.external) 1L else 0L)
        if (entity.locationTypeUuid != null) stmt.bindText(11, entity.locationTypeUuid) else stmt.bindNull(11)
        stmt.bindText(12, entity.locationType)
        if (entity.customCapacity != null) stmt.bindDouble(13, entity.customCapacity) else stmt.bindNull(13)
        stmt.bindLong(14, if (entity.isBulkGenerated) 1L else 0L)
        stmt.bindText(15, entity.address)
        stmt.bindText(16, entity.icon)
        stmt.bindText(17, entity.customIcon)
        stmt.bindLong(18, entity.level.toLong())
        stmt.bindLong(19, entity.lft.toLong())
        stmt.bindLong(20, entity.rght.toLong())
        stmt.bindLong(21, entity.treeId.toLong())
        stmt.bindText(22, entity.metadata)
        stmt.bindLong(23, entity.version.toLong())
        stmt.bindText(24, entity.syncStatus.name)
        stmt.bindLong(25, if (entity.isDeleted) 1L else 0L)
        stmt.bindLong(26, entity.updatedAt)
        if (entity.lastModifiedByDeviceUuid != null) stmt.bindText(27, entity.lastModifiedByDeviceUuid) else stmt.bindNull(27)
    }

    private fun mapStockLocationEntity(stmt: SQLiteStatement): StockLocationEntity {
        val u = runCatching { stmt.getText(0) }.getOrDefault("")
        val rawId = runCatching { stmt.getLong(1) }.getOrDefault(0L)
        val finalId = if (rawId > 0L) rawId else {
            u.removePrefix("location-").removePrefix("loc-").toLongOrNull() ?: 0L
        }
        val pId = runCatching { if (stmt.isNull(2)) null else stmt.getLong(2) }.getOrNull()
        val oId = runCatching { if (stmt.isNull(3)) null else stmt.getLong(3) }.getOrNull()

        return StockLocationEntity(
            uuid = u,
            id = finalId,
            parentId = pId,
            ownerId = oId,
            name = runCatching { stmt.getText(4) }.getOrDefault(""),
            description = runCatching { stmt.getText(5) }.getOrDefault(""),
            parentUuid = runCatching { if (stmt.isNull(6)) null else stmt.getText(6) }.getOrNull(),
            structural = runCatching { stmt.getLong(7) != 0L }.getOrDefault(false),
            external = runCatching { stmt.getLong(8) != 0L }.getOrDefault(false),
            locationTypeUuid = runCatching { if (stmt.isNull(9)) null else stmt.getText(9) }.getOrNull(),
            locationType = runCatching { stmt.getText(10) }.getOrDefault("SHELF"),
            customCapacity = runCatching { if (stmt.isNull(11)) null else stmt.getDouble(11) }.getOrNull(),
            isBulkGenerated = runCatching { stmt.getLong(12) != 0L }.getOrDefault(false),
            address = runCatching { stmt.getText(13) }.getOrDefault(""),
            icon = runCatching { stmt.getText(14) }.getOrDefault("warehouse"),
            customIcon = runCatching { stmt.getText(15) }.getOrDefault(""),
            level = runCatching { stmt.getLong(16).toInt() }.getOrDefault(0),
            lft = runCatching { stmt.getLong(17).toInt() }.getOrDefault(0),
            rght = runCatching { stmt.getLong(18).toInt() }.getOrDefault(0),
            treeId = runCatching { stmt.getLong(19).toInt() }.getOrDefault(1),
            metadata = runCatching { stmt.getText(20) }.getOrDefault("{}"),
            version = runCatching { stmt.getLong(21).toInt() }.getOrDefault(1),
            syncStatus = SyncStatus.fromString(stmt.getText(22)),
            isDeleted = runCatching { stmt.getLong(23) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(24) }.getOrDefault(0L),
            lastModifiedByDeviceUuid = runCatching { if (stmt.isNull(25)) null else stmt.getText(25) }.getOrNull()
        )
    }
}
