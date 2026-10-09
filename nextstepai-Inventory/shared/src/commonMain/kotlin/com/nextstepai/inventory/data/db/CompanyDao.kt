package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات الشركات (CompanyDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class CompanyDao {

    private val selectColumns = """
        uuid, name, description, website, phone, email, isSupplier, isManufacturer, isCustomer, active, currency, logoPath, notes, metadata, parentUuid, syncStatus, isDeleted, updatedAt, version, id
    """.trimIndent()

    fun getCompaniesPaged(
        supplierOnly: Boolean = false,
        manufacturerOnly: Boolean = false,
        customerOnly: Boolean = false,
        limit: Int = 20,
        offset: Int = 0
    ): List<CompanyEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<CompanyEntity>()

        val sql = """
            SELECT $selectColumns
            FROM companies
            WHERE isDeleted = 0
              AND (? = 0 OR isSupplier = 1)
              AND (? = 0 OR isManufacturer = 1)
              AND (? = 0 OR isCustomer = 1)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, if (supplierOnly) 1L else 0L)
            stmt.bindLong(2, if (manufacturerOnly) 1L else 0L)
            stmt.bindLong(3, if (customerOnly) 1L else 0L)
            stmt.bindLong(4, limit.toLong())
            stmt.bindLong(5, offset.toLong())
            while (stmt.step()) {
                results.add(mapCompanyEntity(stmt))
            }
        }
        return results
    }

    fun getCompanyByUuid(uuid: String): CompanyEntity? {
        if (uuid.isBlank()) return null
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT $selectColumns
            FROM companies
            WHERE uuid = ? AND isDeleted = 0
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, uuid)
            if (stmt.step()) {
                return mapCompanyEntity(stmt)
            }
        }
        return null
    }

    fun getCompanyByName(name: String): CompanyEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT $selectColumns
            FROM companies
            WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND isDeleted = 0
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, name)
            if (stmt.step()) {
                return mapCompanyEntity(stmt)
            }
        }
        return null
    }

    fun delete(uuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("UPDATE companies SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, Clock.System.now().toEpochMilliseconds())
            stmt.bindText(2, uuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed > 0
    }

    fun getPendingSyncCompanies(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<CompanyEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<CompanyEntity>()
        conn.prepare("""
            SELECT $selectColumns
            FROM companies
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapCompanyEntity(stmt))
            }
        }
        return results
    }

    fun insert(entity: CompanyEntity): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT INTO companies (uuid, name, description, website, phone, email, isSupplier, isManufacturer, isCustomer, active, currency, logoPath, notes, metadata, parentUuid, syncStatus, isDeleted, updatedAt, version, id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.name)
            stmt.bindText(3, entity.description)
            stmt.bindText(4, entity.website)
            stmt.bindText(5, entity.phone)
            stmt.bindText(6, entity.email)
            stmt.bindLong(7, if (entity.isSupplier) 1L else 0L)
            stmt.bindLong(8, if (entity.isManufacturer) 1L else 0L)
            stmt.bindLong(9, if (entity.isCustomer) 1L else 0L)
            stmt.bindLong(10, if (entity.active) 1L else 0L)
            stmt.bindText(11, entity.currency)
            if (entity.logoPath != null) stmt.bindText(12, entity.logoPath) else stmt.bindNull(12)
            stmt.bindText(13, entity.notes)
            stmt.bindText(14, entity.metadata)
            if (entity.parentUuid != null) stmt.bindText(15, entity.parentUuid) else stmt.bindNull(15)
            stmt.bindText(16, entity.syncStatus.name)
            stmt.bindLong(17, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(18, entity.updatedAt)
            stmt.bindLong(19, entity.version.toLong())
            stmt.bindLong(20, entity.id)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed > 0
    }

    fun update(entity: CompanyEntity): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE companies
            SET name = ?, description = ?, website = ?, phone = ?, email = ?, isSupplier = ?, isManufacturer = ?, isCustomer = ?, active = ?, currency = ?, logoPath = ?, notes = ?, metadata = ?, parentUuid = ?, syncStatus = 'PENDING', version = version + 1, updatedAt = ?
            WHERE uuid = ? AND isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.name)
            stmt.bindText(2, entity.description)
            stmt.bindText(3, entity.website)
            stmt.bindText(4, entity.phone)
            stmt.bindText(5, entity.email)
            stmt.bindLong(6, if (entity.isSupplier) 1L else 0L)
            stmt.bindLong(7, if (entity.isManufacturer) 1L else 0L)
            stmt.bindLong(8, if (entity.isCustomer) 1L else 0L)
            stmt.bindLong(9, if (entity.active) 1L else 0L)
            stmt.bindText(10, entity.currency)
            if (entity.logoPath != null) stmt.bindText(11, entity.logoPath) else stmt.bindNull(11)
            stmt.bindText(12, entity.notes)
            stmt.bindText(13, entity.metadata)
            if (entity.parentUuid != null) stmt.bindText(14, entity.parentUuid) else stmt.bindNull(14)
            stmt.bindLong(15, entity.updatedAt)
            stmt.bindText(16, entity.uuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed > 0
    }

    fun insertOrUpdate(entity: CompanyEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO companies (uuid, name, description, website, phone, email, isSupplier, isManufacturer, isCustomer, active, currency, logoPath, notes, metadata, parentUuid, syncStatus, isDeleted, updatedAt, id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.name)
            stmt.bindText(3, entity.description)
            stmt.bindText(4, entity.website)
            stmt.bindText(5, entity.phone)
            stmt.bindText(6, entity.email)
            stmt.bindLong(7, if (entity.isSupplier) 1L else 0L)
            stmt.bindLong(8, if (entity.isManufacturer) 1L else 0L)
            stmt.bindLong(9, if (entity.isCustomer) 1L else 0L)
            stmt.bindLong(10, if (entity.active) 1L else 0L)
            stmt.bindText(11, entity.currency)
            if (entity.logoPath != null) stmt.bindText(12, entity.logoPath) else stmt.bindNull(12)
            stmt.bindText(13, entity.notes)
            stmt.bindText(14, entity.metadata)
            if (entity.parentUuid != null) stmt.bindText(15, entity.parentUuid) else stmt.bindNull(15)
            stmt.bindText(16, entity.syncStatus.name)
            stmt.bindLong(17, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(18, entity.updatedAt)
            stmt.bindLong(19, entity.id)
            stmt.step()
        }
    }

    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE companies SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    private fun mapCompanyEntity(stmt: SQLiteStatement): CompanyEntity {
        return CompanyEntity(
            uuid = stmt.getText(0),
            name = stmt.getText(1),
            description = stmt.getText(2),
            website = stmt.getText(3),
            phone = stmt.getText(4),
            email = stmt.getText(5),
            isSupplier = stmt.getLong(6) != 0L,
            isManufacturer = stmt.getLong(7) != 0L,
            isCustomer = stmt.getLong(8) != 0L,
            active = stmt.getLong(9) != 0L,
            currency = stmt.getText(10),
            logoPath = if (stmt.isNull(11)) null else stmt.getText(11),
            notes = stmt.getText(12),
            metadata = stmt.getText(13),
            parentUuid = if (stmt.isNull(14)) null else stmt.getText(14),
            syncStatus = SyncStatus.fromString(stmt.getText(15)),
            isDeleted = stmt.getLong(16) != 0L,
            updatedAt = stmt.getLong(17),
            version = runCatching { stmt.getLong(18).toInt() }.getOrDefault(1),
            id = runCatching { stmt.getLong(19) }.getOrDefault(0L)
        )
    }
}
