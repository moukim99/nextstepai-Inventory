package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات جهات الاتصال (ContactDao) باستعلامات صريحة.
 */
@Dao
class ContactDao {
    suspend fun getContactsForCompany(companyUuid: String): List<ContactEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<ContactEntity>()
        conn.prepare("""
            SELECT uuid, companyUuid, name, phone, email, role, syncStatus, isDeleted, updatedAt
            FROM contacts
            WHERE companyUuid = ? AND isDeleted = 0
            ORDER BY name ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, companyUuid)
            while (stmt.step()) {
                results.add(
                    ContactEntity(
                        uuid = stmt.getText(0),
                        companyUuid = stmt.getText(1),
                        name = stmt.getText(2),
                        phone = stmt.getText(3),
                        email = stmt.getText(4),
                        role = stmt.getText(5),
                        syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(6)) }.getOrDefault(SyncStatus.PENDING),
                        isDeleted = stmt.getLong(7) != 0L,
                        updatedAt = stmt.getLong(8)
                    )
                )
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: ContactEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO contacts (uuid, companyUuid, name, phone, email, role, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.companyUuid)
            stmt.bindText(3, entity.name)
            stmt.bindText(4, entity.phone)
            stmt.bindText(5, entity.email)
            stmt.bindText(6, entity.role)
            stmt.bindText(7, entity.syncStatus.name)
            stmt.bindLong(8, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(9, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE contacts SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }
}

/**
 * كائن الوصول لبيانات مرفقات الشركات (CompanyAttachmentDao) باستعلامات صريحة.
 */
@Dao
class CompanyAttachmentDao {
    suspend fun getForCompany(companyUuid: String): List<CompanyAttachmentEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<CompanyAttachmentEntity>()
        runCatching {
            conn.prepare("""
                SELECT uuid, companyUuid, documentType, attachmentPath, link, comment, uploadDate, userId, expiryDate, notifyOnExpiry, notificationDaysBefore, syncStatus, isDeleted, updatedAt
                FROM company_attachments
                WHERE companyUuid = ? AND isDeleted = 0
                ORDER BY uploadDate DESC
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, companyUuid)
                while (stmt.step()) {
                    results.add(
                        CompanyAttachmentEntity(
                            uuid = stmt.getText(0),
                            companyUuid = stmt.getText(1),
                            documentType = runCatching { stmt.getText(2) }.getOrDefault("سجل تجاري"),
                            attachmentPath = stmt.getText(3),
                            link = stmt.getText(4),
                            comment = stmt.getText(5),
                            uploadDate = stmt.getLong(6),
                            userId = if (stmt.isNull(7)) null else stmt.getLong(7),
                            expiryDate = runCatching { stmt.getText(8) }.getOrDefault(""),
                            notifyOnExpiry = runCatching { stmt.getLong(9) != 0L }.getOrDefault(true),
                            notificationDaysBefore = runCatching { stmt.getLong(10).toInt() }.getOrDefault(30),
                            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(11)) }.getOrDefault(SyncStatus.PENDING),
                            isDeleted = stmt.getLong(12) != 0L,
                            updatedAt = stmt.getLong(13)
                        )
                    )
                }
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: CompanyAttachmentEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        runCatching {
            conn.prepare("""
                INSERT OR REPLACE INTO company_attachments (uuid, companyUuid, documentType, attachmentPath, link, comment, uploadDate, userId, expiryDate, notifyOnExpiry, notificationDaysBefore, syncStatus, isDeleted, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, entity.uuid)
                stmt.bindText(2, entity.companyUuid)
                stmt.bindText(3, entity.documentType)
                stmt.bindText(4, entity.attachmentPath)
                stmt.bindText(5, entity.link)
                stmt.bindText(6, entity.comment)
                stmt.bindLong(7, entity.uploadDate)
                if (entity.userId != null) stmt.bindLong(8, entity.userId) else stmt.bindNull(8)
                stmt.bindText(9, entity.expiryDate)
                stmt.bindLong(10, if (entity.notifyOnExpiry) 1L else 0L)
                stmt.bindLong(11, entity.notificationDaysBefore.toLong())
                stmt.bindText(12, entity.syncStatus.name)
                stmt.bindLong(13, if (entity.isDeleted) 1L else 0L)
                stmt.bindLong(14, entity.updatedAt)
                stmt.step()
            }
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE company_attachments SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }
}

/**
 * كائن الوصول لبيانات العناوين (AddressDao) باستعلامات صريحة.
 */
@Dao
class AddressDao {
    suspend fun getAddressesForCompany(companyUuid: String): List<AddressEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<AddressEntity>()
        conn.prepare("""
            SELECT uuid, companyUuid, title, isPrimary, line1, line2, postalCode, city, province, country, shippingNotes, syncStatus, isDeleted, updatedAt
            FROM addresses
            WHERE companyUuid = ? AND isDeleted = 0
            ORDER BY isPrimary DESC, title ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, companyUuid)
            while (stmt.step()) {
                results.add(
                    AddressEntity(
                        uuid = stmt.getText(0),
                        companyUuid = stmt.getText(1),
                        title = stmt.getText(2),
                        isPrimary = stmt.getLong(3) != 0L,
                        line1 = stmt.getText(4),
                        line2 = stmt.getText(5),
                        postalCode = stmt.getText(6),
                        city = stmt.getText(7),
                        province = stmt.getText(8),
                        country = stmt.getText(9),
                        shippingNotes = stmt.getText(10),
                        syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(11)) }.getOrDefault(SyncStatus.PENDING),
                        isDeleted = stmt.getLong(12) != 0L,
                        updatedAt = stmt.getLong(13)
                    )
                )
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: AddressEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        if (entity.isPrimary) {
            conn.prepare("UPDATE addresses SET isPrimary = 0 WHERE companyUuid = ?").use { stmt ->
                stmt.bindText(1, entity.companyUuid)
                stmt.step()
            }
        }

        conn.prepare("""
            INSERT OR REPLACE INTO addresses (uuid, companyUuid, title, isPrimary, line1, line2, postalCode, city, province, country, shippingNotes, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.companyUuid)
            stmt.bindText(3, entity.title)
            stmt.bindLong(4, if (entity.isPrimary) 1L else 0L)
            stmt.bindText(5, entity.line1)
            stmt.bindText(6, entity.line2)
            stmt.bindText(7, entity.postalCode)
            stmt.bindText(8, entity.city)
            stmt.bindText(9, entity.province)
            stmt.bindText(10, entity.country)
            stmt.bindText(11, entity.shippingNotes)
            stmt.bindText(12, entity.syncStatus.name)
            stmt.bindLong(13, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(14, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE addresses SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }
}

/**
 * كائن الوصول لبيانات قطع المصنّع (ManufacturerPartDao) باستعلامات صريحة.
 */
@Dao
class ManufacturerPartDao {
    suspend fun getForCompany(manufacturerUuid: String): List<ManufacturerPartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<ManufacturerPartEntity>()
        conn.prepare("""
            SELECT uuid, partUuid, manufacturerUuid, mpn, description, link, metadata, syncStatus, isDeleted, updatedAt
            FROM manufacturer_parts
            WHERE manufacturerUuid = ? AND isDeleted = 0
            ORDER BY mpn ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, manufacturerUuid)
            while (stmt.step()) {
                results.add(mapEntity(stmt))
            }
        }
        return results
    }

    suspend fun getForPart(partUuid: String): List<ManufacturerPartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<ManufacturerPartEntity>()
        conn.prepare("""
            SELECT uuid, partUuid, manufacturerUuid, mpn, description, link, metadata, syncStatus, isDeleted, updatedAt
            FROM manufacturer_parts
            WHERE partUuid = ? AND isDeleted = 0
            ORDER BY mpn ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            while (stmt.step()) {
                results.add(mapEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: ManufacturerPartEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO manufacturer_parts (uuid, partUuid, manufacturerUuid, mpn, description, link, metadata, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.partUuid)
            stmt.bindText(3, entity.manufacturerUuid)
            stmt.bindText(4, entity.mpn)
            stmt.bindText(5, entity.description)
            stmt.bindText(6, entity.link)
            stmt.bindText(7, entity.metadata)
            stmt.bindText(8, entity.syncStatus.name)
            stmt.bindLong(9, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(10, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE manufacturer_parts SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }

    private fun mapEntity(stmt: SQLiteStatement): ManufacturerPartEntity {
        return ManufacturerPartEntity(
            uuid = stmt.getText(0),
            partUuid = stmt.getText(1),
            manufacturerUuid = stmt.getText(2),
            mpn = stmt.getText(3),
            description = stmt.getText(4),
            link = stmt.getText(5),
            metadata = stmt.getText(6),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(7)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(8) != 0L,
            updatedAt = stmt.getLong(9)
        )
    }
}

/**
 * كائن الوصول لبيانات معاملات قطع المصنّع (ManufacturerPartParameterDao) باستعلامات صريحة.
 */
@Dao
class ManufacturerPartParameterDao {
    suspend fun getForManufacturerPart(manufacturerPartUuid: String): List<ManufacturerPartParameterEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<ManufacturerPartParameterEntity>()
        conn.prepare("""
            SELECT uuid, manufacturerPartUuid, name, value, units, syncStatus, isDeleted, updatedAt
            FROM manufacturer_part_parameters
            WHERE manufacturerPartUuid = ? AND isDeleted = 0
            ORDER BY name ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, manufacturerPartUuid)
            while (stmt.step()) {
                results.add(
                    ManufacturerPartParameterEntity(
                        uuid = stmt.getText(0),
                        manufacturerPartUuid = stmt.getText(1),
                        name = stmt.getText(2),
                        value = stmt.getText(3),
                        units = stmt.getText(4),
                        syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(5)) }.getOrDefault(SyncStatus.PENDING),
                        isDeleted = stmt.getLong(6) != 0L,
                        updatedAt = stmt.getLong(7)
                    )
                )
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: ManufacturerPartParameterEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO manufacturer_part_parameters (uuid, manufacturerPartUuid, name, value, units, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.manufacturerPartUuid)
            stmt.bindText(3, entity.name)
            stmt.bindText(4, entity.value)
            stmt.bindText(5, entity.units)
            stmt.bindText(6, entity.syncStatus.name)
            stmt.bindLong(7, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(8, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE manufacturer_part_parameters SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }
}

/**
 * كائن الوصول لبيانات مرفقات قطع المصنّع (ManufacturerPartAttachmentDao) باستعلامات صريحة.
 */
@Dao
class ManufacturerPartAttachmentDao {
    suspend fun getForManufacturerPart(manufacturerPartUuid: String): List<ManufacturerPartAttachmentEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<ManufacturerPartAttachmentEntity>()
        conn.prepare("""
            SELECT uuid, manufacturerPartUuid, attachmentPath, link, comment, uploadDate, userId, syncStatus, isDeleted, updatedAt
            FROM manufacturer_part_attachments
            WHERE manufacturerPartUuid = ? AND isDeleted = 0
            ORDER BY uploadDate DESC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, manufacturerPartUuid)
            while (stmt.step()) {
                results.add(
                    ManufacturerPartAttachmentEntity(
                        uuid = stmt.getText(0),
                        manufacturerPartUuid = stmt.getText(1),
                        attachmentPath = stmt.getText(2),
                        link = stmt.getText(3),
                        comment = stmt.getText(4),
                        uploadDate = stmt.getLong(5),
                        userId = if (stmt.isNull(6)) null else stmt.getLong(6),
                        syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(7)) }.getOrDefault(SyncStatus.PENDING),
                        isDeleted = stmt.getLong(8) != 0L,
                        updatedAt = stmt.getLong(9)
                    )
                )
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: ManufacturerPartAttachmentEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO manufacturer_part_attachments (uuid, manufacturerPartUuid, attachmentPath, link, comment, uploadDate, userId, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.manufacturerPartUuid)
            stmt.bindText(3, entity.attachmentPath)
            stmt.bindText(4, entity.link)
            stmt.bindText(5, entity.comment)
            stmt.bindLong(6, entity.uploadDate)
            if (entity.userId != null) stmt.bindLong(7, entity.userId) else stmt.bindNull(7)
            stmt.bindText(8, entity.syncStatus.name)
            stmt.bindLong(9, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(10, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE manufacturer_part_attachments SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }
}

/**
 * كائن الوصول لبيانات قطع الموردين (SupplierPartDao) باستعلامات صريحة.
 */
@Dao
class SupplierPartDao {
    suspend fun getForCompany(supplierUuid: String): List<SupplierPartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<SupplierPartEntity>()
        conn.prepare("""
            SELECT uuid, partUuid, supplierUuid, sku, manufacturerPartUuid, description, link, note, packaging, packQuantity, availableForPurchase, active, metadata, syncStatus, isDeleted, updatedAt
            FROM supplier_parts
            WHERE supplierUuid = ? AND isDeleted = 0
            ORDER BY sku ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, supplierUuid)
            while (stmt.step()) {
                results.add(mapEntity(stmt))
            }
        }
        return results
    }

    suspend fun getForPart(partUuid: String): List<SupplierPartEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<SupplierPartEntity>()
        conn.prepare("""
            SELECT uuid, partUuid, supplierUuid, sku, manufacturerPartUuid, description, link, note, packaging, packQuantity, availableForPurchase, active, metadata, syncStatus, isDeleted, updatedAt
            FROM supplier_parts
            WHERE partUuid = ? AND isDeleted = 0
            ORDER BY sku ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            while (stmt.step()) {
                results.add(mapEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: SupplierPartEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO supplier_parts (uuid, partUuid, supplierUuid, sku, manufacturerPartUuid, description, link, note, packaging, packQuantity, availableForPurchase, active, metadata, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.partUuid)
            stmt.bindText(3, entity.supplierUuid)
            stmt.bindText(4, entity.sku)
            if (entity.manufacturerPartUuid != null) stmt.bindText(5, entity.manufacturerPartUuid) else stmt.bindNull(5)
            stmt.bindText(6, entity.description)
            stmt.bindText(7, entity.link)
            stmt.bindText(8, entity.note)
            stmt.bindText(9, entity.packaging)
            stmt.bindText(10, entity.packQuantity)
            stmt.bindLong(11, if (entity.availableForPurchase) 1L else 0L)
            stmt.bindLong(12, if (entity.active) 1L else 0L)
            stmt.bindText(13, entity.metadata)
            stmt.bindText(14, entity.syncStatus.name)
            stmt.bindLong(15, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(16, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE supplier_parts SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }

    private fun mapEntity(stmt: SQLiteStatement): SupplierPartEntity {
        return SupplierPartEntity(
            uuid = stmt.getText(0),
            partUuid = stmt.getText(1),
            supplierUuid = stmt.getText(2),
            sku = stmt.getText(3),
            manufacturerPartUuid = if (stmt.isNull(4)) null else stmt.getText(4),
            description = stmt.getText(5),
            link = stmt.getText(6),
            note = stmt.getText(7),
            packaging = stmt.getText(8),
            packQuantity = stmt.getText(9),
            availableForPurchase = stmt.getLong(10) != 0L,
            active = stmt.getLong(11) != 0L,
            metadata = stmt.getText(12),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(13)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(14) != 0L,
            updatedAt = stmt.getLong(15)
        )
    }
}

/**
 * كائن الوصول لبيانات شرائح أسعار الموردين (SupplierPriceBreakDao) باستعلامات صريحة.
 */
@Dao
class SupplierPriceBreakDao {
    suspend fun getForSupplierPart(supplierPartUuid: String): List<SupplierPriceBreakEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<SupplierPriceBreakEntity>()
        conn.prepare("""
            SELECT uuid, supplierPartUuid, quantity, price, priceCurrency, packQuantity, syncStatus, isDeleted, updatedAt
            FROM supplier_price_breaks
            WHERE supplierPartUuid = ? AND isDeleted = 0
            ORDER BY quantity ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, supplierPartUuid)
            while (stmt.step()) {
                results.add(
                    SupplierPriceBreakEntity(
                        uuid = stmt.getText(0),
                        supplierPartUuid = stmt.getText(1),
                        quantity = stmt.getDouble(2),
                        price = stmt.getDouble(3),
                        priceCurrency = stmt.getText(4),
                        packQuantity = stmt.getText(5),
                        syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(6)) }.getOrDefault(SyncStatus.PENDING),
                        isDeleted = stmt.getLong(7) != 0L,
                        updatedAt = stmt.getLong(8)
                    )
                )
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: SupplierPriceBreakEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO supplier_price_breaks (uuid, supplierPartUuid, quantity, price, priceCurrency, packQuantity, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.supplierPartUuid)
            stmt.bindDouble(3, entity.quantity)
            stmt.bindDouble(4, entity.price)
            stmt.bindText(5, entity.priceCurrency)
            stmt.bindText(6, entity.packQuantity)
            stmt.bindText(7, entity.syncStatus.name)
            stmt.bindLong(8, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(9, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE supplier_price_breaks SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }
}
