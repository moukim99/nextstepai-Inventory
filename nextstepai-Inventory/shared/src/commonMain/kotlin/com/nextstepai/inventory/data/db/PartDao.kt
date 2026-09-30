package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات القطع (PartDao) المعتمد بنسبة 100% على معرّفات UUIDv7 واستعلامات SQL صريحة المعاملات.
 * يتضمن زيادة الـ version تلقائياً محلياً (`version = version + 1`) وتصفية الحذف المنطقي `isDeleted = 0` في كافة القراءات.
 */
@Dao
class PartDao {

    private val selectColumns = """
        uuid, name, ipn, description, categoryUuid, units, minimumStock, maximumStock,
        totalInStock, revision, keywords, assembly, component, isTemplate, variantOfUuid,
        trackable, purchaseable, salable, virtual, active, locked, defaultLocationUuid,
        defaultExpiryDays, link, localImagePath, metadata, version, syncStatus, isDeleted,
        updatedAt, lastModifiedByDeviceUuid
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

    suspend fun getPartByUuid(uuid: String): PartEntity? {
        if (uuid.isBlank()) return null
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            SELECT $selectColumns
            FROM parts
            WHERE uuid = ? AND isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            if (stmt.step()) {
                return mapPartEntity(stmt)
            }
        }
        return null
    }

    suspend fun getPartsByCategoryUuid(categoryUuid: String?): List<PartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartEntity>()
        val sql = if (categoryUuid == null) {
            "SELECT $selectColumns FROM parts WHERE categoryUuid IS NULL AND isDeleted = 0 ORDER BY name ASC"
        } else {
            "SELECT $selectColumns FROM parts WHERE categoryUuid = ? AND isDeleted = 0 ORDER BY name ASC"
        }
        conn.prepare(sql).use { stmt ->
            if (categoryUuid != null) {
                stmt.bindText(1, categoryUuid)
            }
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

    suspend fun getEligibleSubParts(parentPartUuid: String): List<PartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartEntity>()
        conn.prepare("""
            SELECT $selectColumns
            FROM parts
            WHERE component = 1
              AND uuid != ?
              AND uuid NOT IN (SELECT subPartUuid FROM bom_items WHERE partUuid = ? AND isDeleted = 0)
              AND isDeleted = 0
              AND active = 1
            ORDER BY name ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, parentPartUuid)
            stmt.bindText(2, parentPartUuid)
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

    /**
     * إدراج أو تحديث قطعة مادية مع تطبيق زيادة الـ version تلقائياً عند التعديل المحلي.
     */
    suspend fun insertOrUpdate(entity: PartEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT INTO parts (
                uuid, name, ipn, description, categoryUuid, units, minimumStock, maximumStock,
                totalInStock, revision, keywords, assembly, component, isTemplate, variantOfUuid,
                trackable, purchaseable, salable, virtual, active, locked, defaultLocationUuid,
                defaultExpiryDays, link, localImagePath, metadata, version, syncStatus, isDeleted,
                updatedAt, lastModifiedByDeviceUuid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                ipn = excluded.ipn,
                description = excluded.description,
                categoryUuid = excluded.categoryUuid,
                units = excluded.units,
                minimumStock = excluded.minimumStock,
                maximumStock = excluded.maximumStock,
                totalInStock = excluded.totalInStock,
                revision = excluded.revision,
                keywords = excluded.keywords,
                assembly = excluded.assembly,
                component = excluded.component,
                isTemplate = excluded.isTemplate,
                variantOfUuid = excluded.variantOfUuid,
                trackable = excluded.trackable,
                purchaseable = excluded.purchaseable,
                salable = excluded.salable,
                virtual = excluded.virtual,
                active = excluded.active,
                locked = excluded.locked,
                defaultLocationUuid = excluded.defaultLocationUuid,
                defaultExpiryDays = excluded.defaultExpiryDays,
                link = excluded.link,
                localImagePath = excluded.localImagePath,
                metadata = excluded.metadata,
                version = parts.version + 1,
                syncStatus = excluded.syncStatus,
                isDeleted = excluded.isDeleted,
                updatedAt = excluded.updatedAt,
                lastModifiedByDeviceUuid = excluded.lastModifiedByDeviceUuid
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.name)
            stmt.bindText(3, entity.ipn)
            stmt.bindText(4, entity.description)
            if (entity.categoryUuid != null) stmt.bindText(5, entity.categoryUuid) else stmt.bindNull(5)
            stmt.bindText(6, entity.units)
            stmt.bindDouble(7, entity.minimumStock)
            if (entity.maximumStock != null) stmt.bindDouble(8, entity.maximumStock) else stmt.bindNull(8)
            stmt.bindDouble(9, entity.totalInStock)
            stmt.bindText(10, entity.revision)
            stmt.bindText(11, entity.keywords)
            stmt.bindLong(12, if (entity.assembly) 1L else 0L)
            stmt.bindLong(13, if (entity.component) 1L else 0L)
            stmt.bindLong(14, if (entity.isTemplate) 1L else 0L)
            if (entity.variantOfUuid != null) stmt.bindText(15, entity.variantOfUuid) else stmt.bindNull(15)
            stmt.bindLong(16, if (entity.trackable) 1L else 0L)
            stmt.bindLong(17, if (entity.purchaseable) 1L else 0L)
            stmt.bindLong(18, if (entity.salable) 1L else 0L)
            stmt.bindLong(19, if (entity.virtual) 1L else 0L)
            stmt.bindLong(20, if (entity.active) 1L else 0L)
            stmt.bindLong(21, if (entity.locked) 1L else 0L)
            if (entity.defaultLocationUuid != null) stmt.bindText(22, entity.defaultLocationUuid) else stmt.bindNull(22)
            if (entity.defaultExpiryDays != null) stmt.bindLong(23, entity.defaultExpiryDays.toLong()) else stmt.bindNull(23)
            stmt.bindText(24, entity.link)
            if (entity.localImagePath != null) stmt.bindText(25, entity.localImagePath) else stmt.bindNull(25)
            stmt.bindText(26, entity.metadata)
            stmt.bindLong(27, entity.version.toLong())
            stmt.bindText(28, entity.syncStatus.name)
            stmt.bindLong(29, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(30, entity.updatedAt)
            if (entity.lastModifiedByDeviceUuid != null) stmt.bindText(31, entity.lastModifiedByDeviceUuid) else stmt.bindNull(31)
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

    suspend fun addStockToPart(partUuid: String, qty: Double) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("UPDATE parts SET totalInStock = totalInStock + ?, version = version + 1, updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindDouble(1, qty)
            stmt.bindLong(2, Clock.System.now().toEpochMilliseconds())
            stmt.bindText(3, partUuid)
            stmt.step()
        }
    }

    suspend fun softDeleteByUuid(uuid: String, updatedAtMs: Long = Clock.System.now().toEpochMilliseconds()) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("UPDATE parts SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, updatedAtMs)
            stmt.bindText(2, uuid)
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
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            name = runCatching { stmt.getText(1) }.getOrDefault(""),
            ipn = runCatching { stmt.getText(2) }.getOrDefault(""),
            description = runCatching { stmt.getText(3) }.getOrDefault(""),
            categoryUuid = runCatching { if (stmt.isNull(4)) null else stmt.getText(4) }.getOrNull(),
            units = runCatching { stmt.getText(5) }.getOrDefault("pcs"),
            minimumStock = runCatching { stmt.getDouble(6) }.getOrDefault(0.0),
            maximumStock = runCatching { if (stmt.isNull(7)) null else stmt.getDouble(7) }.getOrNull(),
            totalInStock = runCatching { stmt.getDouble(8) }.getOrDefault(0.0),
            revision = runCatching { stmt.getText(9) }.getOrDefault(""),
            keywords = runCatching { stmt.getText(10) }.getOrDefault(""),
            assembly = runCatching { stmt.getLong(11) != 0L }.getOrDefault(false),
            component = runCatching { stmt.getLong(12) != 0L }.getOrDefault(true),
            isTemplate = runCatching { stmt.getLong(13) != 0L }.getOrDefault(false),
            variantOfUuid = runCatching { if (stmt.isNull(14)) null else stmt.getText(14) }.getOrNull(),
            trackable = runCatching { stmt.getLong(15) != 0L }.getOrDefault(false),
            purchaseable = runCatching { stmt.getLong(16) != 0L }.getOrDefault(true),
            salable = runCatching { stmt.getLong(17) != 0L }.getOrDefault(false),
            virtual = runCatching { stmt.getLong(18) != 0L }.getOrDefault(false),
            active = runCatching { stmt.getLong(19) != 0L }.getOrDefault(true),
            locked = runCatching { stmt.getLong(20) != 0L }.getOrDefault(false),
            defaultLocationUuid = runCatching { if (stmt.isNull(21)) null else stmt.getText(21) }.getOrNull(),
            defaultExpiryDays = runCatching { if (stmt.isNull(22)) null else stmt.getLong(22).toInt() }.getOrNull(),
            link = runCatching { stmt.getText(23) }.getOrDefault(""),
            localImagePath = runCatching { if (stmt.isNull(24)) null else stmt.getText(24) }.getOrNull(),
            metadata = runCatching { stmt.getText(25) }.getOrDefault("{}"),
            version = runCatching { stmt.getLong(26).toInt() }.getOrDefault(1),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(27)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(28) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(29) }.getOrDefault(0L),
            lastModifiedByDeviceUuid = runCatching { if (stmt.isNull(30)) null else stmt.getText(30) }.getOrNull()
        )
    }
}
