package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PartCategoryParameterTemplate
import com.nextstepai.inventory.data.PartNotes
import com.nextstepai.inventory.data.PartParameter
import com.nextstepai.inventory.data.PartParameterTemplate
import com.nextstepai.inventory.data.PartRelated
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات مرفقات القطع (PartAttachmentDao) في SQLite.
 */
@Dao
class PartAttachmentDao {

    fun getForPart(partUuid: String): List<PartAttachment> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartAttachment>()
        conn.prepare("""
            SELECT uuid, partUuid, attachment, link, comment, uploadDate, userUuid
            FROM part_attachments
            WHERE partUuid = ? AND isDeleted = 0
            ORDER BY uploadDate DESC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val pUuid = stmt.getText(1)
                val attachment = if (stmt.isNull(2)) null else stmt.getText(2)
                val link = if (stmt.isNull(3)) null else stmt.getText(3)
                val comment = stmt.getText(4)
                val uploadDate = stmt.getText(5)
                val userUuid = if (stmt.isNull(6)) null else stmt.getText(6)
                val id = uuid.removePrefix("part-att-").toLongOrNull() ?: 0L
                val partId = pUuid.removePrefix("part-").toLongOrNull() ?: 0L
                val userId = userUuid?.removePrefix("usr-")?.toLongOrNull() ?: 1L
                results.add(
                    PartAttachment(
                        id = id,
                        partId = partId,
                        attachment = attachment,
                        link = link,
                        comment = comment,
                        uploadDate = uploadDate,
                        userId = userId
                    )
                )
            }
        }
        return results
    }

    fun insertOrUpdate(attachment: PartAttachment, partUuid: String = "part-${attachment.partId}"): PartAttachment {
        val allocatedId = if (attachment.id > 0L) attachment.id else SqliteNumericIdAllocator.nextId("part_attachments", "part-att-")
        val attachmentUuid = "part-att-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO part_attachments (
                uuid, partUuid, attachment, link, comment, uploadDate, userUuid, version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, attachmentUuid)
            stmt.bindText(2, partUuid)
            if (attachment.attachment != null) stmt.bindText(3, attachment.attachment) else stmt.bindNull(3)
            if (attachment.link != null) stmt.bindText(4, attachment.link) else stmt.bindNull(4)
            stmt.bindText(5, attachment.comment)
            stmt.bindText(6, attachment.uploadDate)
            stmt.bindText(7, "usr-${attachment.userId ?: 1L}")
            stmt.bindLong(8, now)
            stmt.step()
        }
        return attachment.copy(id = allocatedId)
    }

    fun delete(uuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_attachments SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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

    fun deleteForPart(partUuid: String): Int {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_attachments SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed
    }
}

/**
 * كائن الوصول لبيانات ملاحظات القطع (PartNotesDao) في SQLite.
 */
@Dao
class PartNotesDao {

    fun getForPart(partUuid: String): PartNotes? {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            SELECT uuid, partUuid, notes, updatedAt, userUuid
            FROM part_notes
            WHERE partUuid = ? AND isDeleted = 0
            LIMIT 1
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            if (stmt.step()) {
                val uuid = stmt.getText(0)
                val pUuid = stmt.getText(1)
                val notes = stmt.getText(2)
                val updatedAt = stmt.getLong(3).toString()
                val userUuid = if (stmt.isNull(4)) null else stmt.getText(4)
                val id = uuid.removePrefix("part-note-").toLongOrNull() ?: 0L
                val partId = pUuid.removePrefix("part-").toLongOrNull() ?: 0L
                val userId = userUuid?.removePrefix("usr-")?.toLongOrNull() ?: 1L
                return PartNotes(
                    id = id,
                    partId = partId,
                    notes = notes,
                    updatedAt = updatedAt,
                    userId = userId
                )
            }
        }
        return null
    }

    fun saveOrUpdate(partUuid: String, notes: String, userId: Long? = 1L, currentTimestamp: String = Clock.System.now().toString()): PartNotes {
        val conn = SqliteDatabaseManager.getConnection()
        val existing = getForPart(partUuid)
        val allocatedId = existing?.id?.takeIf { it > 0L } ?: SqliteNumericIdAllocator.nextId("part_notes", "part-note-")
        val noteUuid = "part-note-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        val userStr = "usr-${userId ?: 1L}"

        conn.prepare("""
            INSERT INTO part_notes (uuid, partUuid, notes, userUuid, version, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, 1, 'PENDING', 0, ?)
            ON CONFLICT(partUuid) DO UPDATE SET
                notes = excluded.notes,
                userUuid = excluded.userUuid,
                version = part_notes.version + 1,
                syncStatus = 'PENDING',
                isDeleted = 0,
                updatedAt = excluded.updatedAt
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, noteUuid)
            stmt.bindText(2, partUuid)
            stmt.bindText(3, notes)
            stmt.bindText(4, userStr)
            stmt.bindLong(5, now)
            stmt.step()
        }

        val partId = partUuid.removePrefix("part-").toLongOrNull() ?: 0L
        return PartNotes(
            id = allocatedId,
            partId = partId,
            notes = notes,
            updatedAt = currentTimestamp,
            userId = userId
        )
    }

    fun deleteForPart(partUuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_notes SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
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
}

/**
 * كائن الوصول لبيانات الأسعار الداخلية للقطع (PartInternalPriceDao) في SQLite.
 */
@Dao
class PartInternalPriceDao {

    fun getForPart(partUuid: String): List<PartInternalPriceEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartInternalPriceEntity>()
        conn.prepare("""
            SELECT uuid, partUuid, quantity, price, currency
            FROM part_internal_prices
            WHERE partUuid = ? AND isDeleted = 0
            ORDER BY quantity ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val pUuid = stmt.getText(1)
                val qty = stmt.getDouble(2)
                val price = stmt.getDouble(3)
                val currency = stmt.getText(4)
                val id = uuid.removePrefix("part-iprice-").toLongOrNull() ?: 0L
                val partId = pUuid.removePrefix("part-").toLongOrNull() ?: 0L
                results.add(
                    PartInternalPriceEntity(
                        id = id,
                        partId = partId,
                        quantity = qty,
                        price = price,
                        currency = currency
                    )
                )
            }
        }
        return results
    }

    fun insert(partUuid: String, quantity: Double, price: Double, currency: String = "USD"): PartInternalPriceEntity {
        val allocatedId = SqliteNumericIdAllocator.nextId("part_internal_prices", "part-iprice-")
        val uuid = "part-iprice-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO part_internal_prices (
                uuid, partUuid, quantity, price, currency, version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindText(2, partUuid)
            stmt.bindDouble(3, quantity)
            stmt.bindDouble(4, price)
            stmt.bindText(5, currency)
            stmt.bindLong(6, now)
            stmt.step()
        }
        val partId = partUuid.removePrefix("part-").toLongOrNull() ?: 0L
        return PartInternalPriceEntity(
            id = allocatedId,
            partId = partId,
            quantity = quantity,
            price = price,
            currency = currency
        )
    }

    fun delete(uuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_internal_prices SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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

    fun deleteForPart(partUuid: String): Int {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_internal_prices SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed
    }
}

/**
 * كائن الوصول لبيانات أسعار البيع للقطع (PartSalePriceDao) في SQLite.
 */
@Dao
class PartSalePriceDao {

    fun getForPart(partUuid: String): List<PartSalePriceEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartSalePriceEntity>()
        conn.prepare("""
            SELECT uuid, partUuid, quantity, price, currency
            FROM part_sale_prices
            WHERE partUuid = ? AND isDeleted = 0
            ORDER BY quantity ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val pUuid = stmt.getText(1)
                val qty = stmt.getDouble(2)
                val price = stmt.getDouble(3)
                val currency = stmt.getText(4)
                val id = uuid.removePrefix("part-sprice-").toLongOrNull() ?: 0L
                val partId = pUuid.removePrefix("part-").toLongOrNull() ?: 0L
                results.add(
                    PartSalePriceEntity(
                        id = id,
                        partId = partId,
                        quantity = qty,
                        price = price,
                        currency = currency
                    )
                )
            }
        }
        return results
    }

    fun insert(partUuid: String, quantity: Double, price: Double, currency: String = "USD"): PartSalePriceEntity {
        val allocatedId = SqliteNumericIdAllocator.nextId("part_sale_prices", "part-sprice-")
        val uuid = "part-sprice-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO part_sale_prices (
                uuid, partUuid, quantity, price, currency, version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindText(2, partUuid)
            stmt.bindDouble(3, quantity)
            stmt.bindDouble(4, price)
            stmt.bindText(5, currency)
            stmt.bindLong(6, now)
            stmt.step()
        }
        val partId = partUuid.removePrefix("part-").toLongOrNull() ?: 0L
        return PartSalePriceEntity(
            id = allocatedId,
            partId = partId,
            quantity = quantity,
            price = price,
            currency = currency
        )
    }

    fun delete(uuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_sale_prices SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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

    fun deleteForPart(partUuid: String): Int {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_sale_prices SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed
    }
}

/**
 * كائن الوصول لبيانات تمييز القطع بنجمة (PartStarDao) في SQLite.
 */
@Dao
class PartStarDao {

    private val partDao: PartDao by lazy { PartDao() }

    fun isStarred(partUuid: String, userUuid: String = "usr-001", partId: Long = 0L): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val numericUserId = userUuid.removePrefix("usr-").toLongOrNull() ?: 1L
        val canonicalUserUuid = "usr-${numericUserId.toString().padStart(3, '0')}"
        val legacyUserUuid = "usr-$numericUserId"

        conn.prepare("""
            SELECT 1 FROM part_stars 
            WHERE (partUuid = ? OR (partId > 0 AND partId = ?)) 
              AND (userUuid = ? OR userUuid = ? OR userId = ?) 
              AND isDeleted = 0 
            LIMIT 1
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            stmt.bindLong(2, partId)
            stmt.bindText(3, canonicalUserUuid)
            stmt.bindText(4, legacyUserUuid)
            stmt.bindLong(5, numericUserId)
            return stmt.step()
        }
    }

    fun toggleStar(partUuid: String, userUuid: String = "usr-001", partId: Long = 0L): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        val numericUserId = userUuid.removePrefix("usr-").toLongOrNull() ?: 1L
        val canonicalUserUuid = "usr-${numericUserId.toString().padStart(3, '0')}"
        val legacyUserUuid = "usr-$numericUserId"

        var foundUuid: String? = null
        var isCurrentlyDeleted = false

        conn.prepare("""
            SELECT uuid, isDeleted FROM part_stars 
            WHERE (partUuid = ? OR (partId > 0 AND partId = ?)) 
              AND (userUuid = ? OR userUuid = ? OR userId = ?) 
            LIMIT 1
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            stmt.bindLong(2, partId)
            stmt.bindText(3, canonicalUserUuid)
            stmt.bindText(4, legacyUserUuid)
            stmt.bindLong(5, numericUserId)
            if (stmt.step()) {
                foundUuid = stmt.getText(0)
                isCurrentlyDeleted = stmt.getLong(1) != 0L
            }
        }

        if (foundUuid != null) {
            val newDeleted = if (isCurrentlyDeleted) 0L else 1L
            conn.prepare("""
                UPDATE part_stars 
                SET isDeleted = ?, syncStatus = 'PENDING', version = version + 1, updatedAt = ? 
                WHERE uuid = ?
            """.trimIndent()).use { stmt ->
                stmt.bindLong(1, newDeleted)
                stmt.bindLong(2, now)
                stmt.bindText(3, foundUuid)
                stmt.step()
            }
            return newDeleted == 0L
        } else {
            val allocatedId = SqliteNumericIdAllocator.nextId("part_stars", "part-star-")
            val recordUuid = "part-star-$allocatedId"
            val targetPartId = if (partId > 0) partId else (partDao.getPartByUuid(partUuid)?.id ?: partUuid.removePrefix("part-uuid-").removePrefix("part-").toLongOrNull() ?: 0L)

            conn.prepare("""
                INSERT INTO part_stars (uuid, id, partId, partUuid, userId, userUuid, version, syncStatus, isDeleted, updatedAt) 
                VALUES (?, ?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, recordUuid)
                stmt.bindLong(2, allocatedId)
                stmt.bindLong(3, targetPartId)
                stmt.bindText(4, partUuid)
                stmt.bindLong(5, numericUserId)
                stmt.bindText(6, canonicalUserUuid)
                stmt.bindLong(7, now)
                stmt.step()
            }
            return true
        }
    }

    fun getStarredPartUuids(userUuid: String = "usr-001"): List<String> {
        val conn = SqliteDatabaseManager.getConnection()
        val numericUserId = userUuid.removePrefix("usr-").toLongOrNull() ?: 1L
        val canonicalUserUuid = "usr-${numericUserId.toString().padStart(3, '0')}"
        val legacyUserUuid = "usr-$numericUserId"
        val results = mutableListOf<String>()

        conn.prepare("""
            SELECT partUuid FROM part_stars 
            WHERE (userUuid = ? OR userUuid = ? OR userId = ?) AND isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, canonicalUserUuid)
            stmt.bindText(2, legacyUserUuid)
            stmt.bindLong(3, numericUserId)
            while (stmt.step()) {
                results.add(stmt.getText(0))
            }
        }
        return results
    }

    fun deleteForPart(partUuid: String): Int {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_stars SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed
    }
}

/**
 * كائن الوصول لبيانات تسعير وتكاليف القطع (PartPricingDao) في SQLite.
 */
@Dao
class PartPricingDao {

    fun getForPart(partUuid: String): PartPricingEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            SELECT uuid, partUuid, currency, overallMin, overallMax,
                   purchaseCostMin, purchaseCostMax, bomCostMin, bomCostMax,
                   variantCostMin, variantCostMax, internalCostMin, internalCostMax, updatedAt
            FROM part_pricing
            WHERE partUuid = ? AND isDeleted = 0
            LIMIT 1
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            if (stmt.step()) {
                val uuid = stmt.getText(0)
                val pUuid = stmt.getText(1)
                val currency = stmt.getText(2)
                val overallMin = if (stmt.isNull(3)) null else stmt.getDouble(3)
                val overallMax = if (stmt.isNull(4)) null else stmt.getDouble(4)
                val purchaseMin = if (stmt.isNull(5)) null else stmt.getDouble(5)
                val purchaseMax = if (stmt.isNull(6)) null else stmt.getDouble(6)
                val bomMin = if (stmt.isNull(7)) null else stmt.getDouble(7)
                val bomMax = if (stmt.isNull(8)) null else stmt.getDouble(8)
                val variantMin = if (stmt.isNull(9)) null else stmt.getDouble(9)
                val variantMax = if (stmt.isNull(10)) null else stmt.getDouble(10)
                val internalMin = if (stmt.isNull(11)) null else stmt.getDouble(11)
                val internalMax = if (stmt.isNull(12)) null else stmt.getDouble(12)
                val updatedAt = stmt.getLong(13).toString()
                val id = uuid.removePrefix("part-pricing-").toLongOrNull() ?: 0L
                val partId = pUuid.removePrefix("part-").toLongOrNull() ?: 0L
                return PartPricingEntity(
                    id = id,
                    partId = partId,
                    currency = currency,
                    overallMin = overallMin,
                    overallMax = overallMax,
                    purchaseCostMin = purchaseMin,
                    purchaseCostMax = purchaseMax,
                    bomCostMin = bomMin,
                    bomCostMax = bomMax,
                    variantCostMin = variantMin,
                    variantCostMax = variantMax,
                    internalCostMin = internalMin,
                    internalCostMax = internalMax,
                    updatedAt = updatedAt
                )
            }
        }
        return null
    }

    fun saveOrUpdate(entity: PartPricingEntity, partUuid: String = "part-${entity.partId}"): PartPricingEntity {
        val conn = SqliteDatabaseManager.getConnection()
        val existing = getForPart(partUuid)
        val allocatedId = existing?.id?.takeIf { it > 0L } ?: SqliteNumericIdAllocator.nextId("part_pricing", "part-pricing-")
        val pricingUuid = "part-pricing-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()

        conn.prepare("""
            INSERT INTO part_pricing (
                uuid, partUuid, currency, overallMin, overallMax,
                purchaseCostMin, purchaseCostMax, bomCostMin, bomCostMax,
                variantCostMin, variantCostMax, internalCostMin, internalCostMax,
                version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
            ON CONFLICT(partUuid) DO UPDATE SET
                currency = excluded.currency,
                overallMin = excluded.overallMin,
                overallMax = excluded.overallMax,
                purchaseCostMin = excluded.purchaseCostMin,
                purchaseCostMax = excluded.purchaseCostMax,
                bomCostMin = excluded.bomCostMin,
                bomCostMax = excluded.bomCostMax,
                variantCostMin = excluded.variantCostMin,
                variantCostMax = excluded.variantCostMax,
                internalCostMin = excluded.internalCostMin,
                internalCostMax = excluded.internalCostMax,
                version = part_pricing.version + 1,
                syncStatus = 'PENDING',
                isDeleted = 0,
                updatedAt = excluded.updatedAt
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, pricingUuid)
            stmt.bindText(2, partUuid)
            stmt.bindText(3, entity.currency)
            if (entity.overallMin != null) stmt.bindDouble(4, entity.overallMin) else stmt.bindNull(4)
            if (entity.overallMax != null) stmt.bindDouble(5, entity.overallMax) else stmt.bindNull(5)
            if (entity.purchaseCostMin != null) stmt.bindDouble(6, entity.purchaseCostMin) else stmt.bindNull(6)
            if (entity.purchaseCostMax != null) stmt.bindDouble(7, entity.purchaseCostMax) else stmt.bindNull(7)
            if (entity.bomCostMin != null) stmt.bindDouble(8, entity.bomCostMin) else stmt.bindNull(8)
            if (entity.bomCostMax != null) stmt.bindDouble(9, entity.bomCostMax) else stmt.bindNull(9)
            if (entity.variantCostMin != null) stmt.bindDouble(10, entity.variantCostMin) else stmt.bindNull(10)
            if (entity.variantCostMax != null) stmt.bindDouble(11, entity.variantCostMax) else stmt.bindNull(11)
            if (entity.internalCostMin != null) stmt.bindDouble(12, entity.internalCostMin) else stmt.bindNull(12)
            if (entity.internalCostMax != null) stmt.bindDouble(13, entity.internalCostMax) else stmt.bindNull(13)
            stmt.bindLong(14, now)
            stmt.step()
        }
        return entity.copy(id = allocatedId)
    }

    fun deleteForPart(partUuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_pricing SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
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
}

/**
 * كائن الوصول لبيانات قوالب اختبارات القطع (PartTestTemplateDao) في SQLite.
 */
@Dao
class PartTestTemplateDao {

    fun getForPart(partUuid: String): List<PartTestTemplate> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartTestTemplate>()
        conn.prepare("""
            SELECT uuid, partUuid, testName, description, required, requiresValue, requiresAttachment
            FROM part_test_templates
            WHERE partUuid = ? AND isDeleted = 0
            ORDER BY testName ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val pUuid = stmt.getText(1)
                val testName = stmt.getText(2)
                val description = stmt.getText(3)
                val required = stmt.getLong(4) != 0L
                val reqValue = stmt.getLong(5) != 0L
                val reqAttachment = stmt.getLong(6) != 0L
                val id = uuid.removePrefix("part-test-").toLongOrNull() ?: 0L
                val partId = pUuid.removePrefix("part-").toLongOrNull() ?: 0L
                results.add(
                    PartTestTemplate(
                        id = id,
                        partId = partId,
                        testName = testName,
                        description = description,
                        required = required,
                        requiresValue = reqValue,
                        requiresAttachment = reqAttachment
                    )
                )
            }
        }
        return results
    }

    fun insert(template: PartTestTemplate, partUuid: String = "part-${template.partId}"): PartTestTemplate {
        val allocatedId = if (template.id > 0L) template.id else SqliteNumericIdAllocator.nextId("part_test_templates", "part-test-")
        val uuid = "part-test-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO part_test_templates (
                uuid, partUuid, testName, description, required, requiresValue, requiresAttachment,
                version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindText(2, partUuid)
            stmt.bindText(3, template.testName)
            stmt.bindText(4, template.description)
            stmt.bindLong(5, if (template.required) 1L else 0L)
            stmt.bindLong(6, if (template.requiresValue) 1L else 0L)
            stmt.bindLong(7, if (template.requiresAttachment) 1L else 0L)
            stmt.bindLong(8, now)
            stmt.step()
        }
        return template.copy(id = allocatedId)
    }

    fun delete(uuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_test_templates SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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

    fun deleteForPart(partUuid: String): Int {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_test_templates SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed
    }
}

/**
 * كائن الوصول لبيانات معاملات وخصائص القطع (PartParameterDao) في SQLite.
 */
@Dao
class PartParameterDao {

    fun getForPart(partUuid: String): List<PartParameter> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartParameter>()
        conn.prepare("""
            SELECT uuid, partUuid, templateUuid, data, dataNumeric
            FROM part_parameters
            WHERE partUuid = ? AND isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val pUuid = stmt.getText(1)
                val templateUuid = stmt.getText(2)
                val data = stmt.getText(3)
                val dataNumeric = if (stmt.isNull(4)) null else stmt.getDouble(4)
                val id = uuid.removePrefix("part-param-").toLongOrNull() ?: 0L
                val partId = pUuid.removePrefix("part-").toLongOrNull() ?: 0L
                val templateId = templateUuid.removePrefix("param-tpl-").toLongOrNull() ?: 0L
                results.add(
                    PartParameter(
                        id = id,
                        partId = partId,
                        templateId = templateId,
                        data = data,
                        dataNumeric = dataNumeric
                    )
                )
            }
        }
        return results
    }

    fun insertOrUpdate(partUuid: String, templateUuid: String, data: String, dataNumeric: Double? = null, id: Long = 0L): PartParameter {
        val allocatedId = if (id > 0L) id else SqliteNumericIdAllocator.nextId("part_parameters", "part-param-")
        val uuid = "part-param-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO part_parameters (
                uuid, partUuid, templateUuid, data, dataNumeric, version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindText(2, partUuid)
            stmt.bindText(3, templateUuid)
            stmt.bindText(4, data)
            if (dataNumeric != null) stmt.bindDouble(5, dataNumeric) else stmt.bindNull(5)
            stmt.bindLong(6, now)
            stmt.step()
        }
        val partId = partUuid.removePrefix("part-").toLongOrNull() ?: 0L
        val templateId = templateUuid.removePrefix("param-tpl-").toLongOrNull() ?: 0L
        return PartParameter(
            id = allocatedId,
            partId = partId,
            templateId = templateId,
            data = data,
            dataNumeric = dataNumeric
        )
    }

    fun delete(uuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_parameters SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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

    fun deleteForPart(partUuid: String): Int {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_parameters SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE partUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed
    }
}

/**
 * كائن الوصول لبيانات القطع ذات الصلة (PartRelatedDao) في SQLite.
 */
@Dao
class PartRelatedDao {

    fun getRelatedForPart(partUuid: String): List<PartRelated> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartRelated>()
        conn.prepare("""
            SELECT rel.uuid, rel.part1Uuid, rel.part2Uuid, p1.id, p2.id
            FROM part_related rel
            LEFT JOIN parts p1 ON p1.uuid = rel.part1Uuid
            LEFT JOIN parts p2 ON p2.uuid = rel.part2Uuid
            WHERE (rel.part1Uuid = ? OR rel.part2Uuid = ?) AND rel.isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, partUuid)
            stmt.bindText(2, partUuid)
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val p1 = stmt.getText(1)
                val p2 = stmt.getText(2)
                val id = uuid.removePrefix("part-rel-").toLongOrNull() ?: 0L
                val part1Id = stmt.getLong(3).takeIf { it > 0L }
                    ?: p1.removePrefix("part-").toLongOrNull() ?: 0L
                val part2Id = stmt.getLong(4).takeIf { it > 0L }
                    ?: p2.removePrefix("part-").toLongOrNull() ?: 0L
                results.add(PartRelated(id = id, part1Id = part1Id, part2Id = part2Id))
            }
        }
        return results
    }

    fun insert(part1Uuid: String, part2Uuid: String): PartRelated {
        require(part1Uuid != part2Uuid) {
            "لا يمكن ربط القطعة بنفسها كقطعة ذات صلة! (Self-Relation Prevention)"
        }
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            SELECT 1 FROM part_related
            WHERE ((part1Uuid = ? AND part2Uuid = ?) OR (part1Uuid = ? AND part2Uuid = ?))
              AND isDeleted = 0
            LIMIT 1
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, part1Uuid)
            stmt.bindText(2, part2Uuid)
            stmt.bindText(3, part2Uuid)
            stmt.bindText(4, part1Uuid)
            if (stmt.step()) {
                throw IllegalArgumentException("علاقة الربط بين هاتين القطعتين موجودة بالفعل! (Bidirectional Uniqueness)")
            }
        }

        val allocatedId = SqliteNumericIdAllocator.nextId("part_related", "part-rel-")
        val uuid = "part-rel-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("""
            INSERT INTO part_related (
                uuid, part1Uuid, part2Uuid, version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindText(2, part1Uuid)
            stmt.bindText(3, part2Uuid)
            stmt.bindLong(4, now)
            stmt.step()
        }
        val p1 = part1Uuid.removePrefix("part-").toLongOrNull() ?: 0L
        val p2 = part2Uuid.removePrefix("part-").toLongOrNull() ?: 0L
        return PartRelated(id = allocatedId, part1Id = p1, part2Id = p2)
    }

    fun delete(id: Long): Boolean {
        val uuid = "part-rel-$id"
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_related SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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

    fun deleteForPart(partUuid: String): Int {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("""
            UPDATE part_related
            SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ?
            WHERE (part1Uuid = ? OR part2Uuid = ?) AND isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, partUuid)
            stmt.bindText(3, partUuid)
            stmt.step()
        }
        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed
    }
}

/**
 * كائن الوصول لبيانات قوالب المعاملات الفنية (PartParameterTemplateDao) في SQLite.
 */
@Dao
class PartParameterTemplateDao {

    fun getAll(): List<PartParameterTemplate> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartParameterTemplate>()
        conn.prepare("""
            SELECT uuid, name, units, description, choices, checkbox
            FROM part_parameter_templates
            WHERE isDeleted = 0
            ORDER BY name ASC
        """.trimIndent()).use { stmt ->
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val name = stmt.getText(1)
                val units = stmt.getText(2)
                val description = stmt.getText(3)
                val choicesStr = stmt.getText(4)
                val checkbox = stmt.getLong(5) != 0L
                val id = uuid.removePrefix("param-tpl-").toLongOrNull() ?: 0L
                val choices = if (choicesStr.isBlank()) emptyList() else choicesStr.split(",").map { it.trim() }
                results.add(
                    PartParameterTemplate(
                        id = id,
                        name = name,
                        units = units,
                        description = description,
                        choices = choices,
                        checkbox = checkbox
                    )
                )
            }
        }
        return results
    }

    fun getById(id: Long): PartParameterTemplate? {
        val uuid = "param-tpl-$id"
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            SELECT uuid, name, units, description, choices, checkbox
            FROM part_parameter_templates
            WHERE uuid = ? AND isDeleted = 0
            LIMIT 1
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            if (stmt.step()) {
                val name = stmt.getText(1)
                val units = stmt.getText(2)
                val description = stmt.getText(3)
                val choicesStr = stmt.getText(4)
                val checkbox = stmt.getLong(5) != 0L
                val choices = if (choicesStr.isBlank()) emptyList() else choicesStr.split(",").map { it.trim() }
                return PartParameterTemplate(
                    id = id,
                    name = name,
                    units = units,
                    description = description,
                    choices = choices,
                    checkbox = checkbox
                )
            }
        }
        return null
    }

    fun insert(template: PartParameterTemplate): PartParameterTemplate {
        require(template.name.isNotBlank()) {
            "اسم قالب المعامل الفني إلزامي ولا يمكن أن يكون فارغاً."
        }
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("SELECT 1 FROM part_parameter_templates WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND isDeleted = 0 LIMIT 1").use { stmt ->
            stmt.bindText(1, template.name)
            if (stmt.step()) {
                throw IllegalArgumentException("اسم قالب المعامل '${template.name.trim()}' موجود بالفعل بالنظام. (قيد الفرادة Unique Name Constraint)")
            }
        }

        val sanitizedTemplate = if (template.checkbox) {
            template.copy(units = "", choices = emptyList())
        } else {
            template
        }

        val allocatedId = if (sanitizedTemplate.id > 0L) sanitizedTemplate.id else SqliteNumericIdAllocator.nextId("part_parameter_templates", "param-tpl-")
        val uuid = "param-tpl-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        val choicesStr = sanitizedTemplate.choices.joinToString(",")

        conn.prepare("""
            INSERT INTO part_parameter_templates (
                uuid, name, units, description, choices, checkbox, version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindText(2, sanitizedTemplate.name.trim())
            stmt.bindText(3, sanitizedTemplate.units)
            stmt.bindText(4, sanitizedTemplate.description)
            stmt.bindText(5, choicesStr)
            stmt.bindLong(6, if (sanitizedTemplate.checkbox) 1L else 0L)
            stmt.bindLong(7, now)
            stmt.step()
        }
        return sanitizedTemplate.copy(id = allocatedId)
    }

    fun delete(id: Long): Boolean {
        val uuid = "param-tpl-$id"
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_parameter_templates SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
        conn.prepare("UPDATE part_category_parameter_templates SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE parameterTemplateUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, uuid)
            stmt.step()
        }
        conn.prepare("UPDATE part_parameters SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE templateUuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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
}

/**
 * كائن الوصول لبيانات ربط قوالب المعاملات بالتصنيفات (PartCategoryParameterTemplateDao) في SQLite.
 */
@Dao
class PartCategoryParameterTemplateDao {

    fun getForCategory(categoryUuid: String): List<PartCategoryParameterTemplate> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PartCategoryParameterTemplate>()
        conn.prepare("""
            SELECT uuid, categoryUuid, parameterTemplateUuid, defaultValue
            FROM part_category_parameter_templates
            WHERE categoryUuid = ? AND isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, categoryUuid)
            while (stmt.step()) {
                val uuid = stmt.getText(0)
                val catUuid = stmt.getText(1)
                val tplUuid = stmt.getText(2)
                val defaultValue = if (stmt.isNull(3)) null else stmt.getText(3)
                val id = uuid.removePrefix("cat-param-tpl-").toLongOrNull() ?: 0L
                val catId = catUuid.removePrefix("cat-").toLongOrNull() ?: 0L
                val tplId = tplUuid.removePrefix("param-tpl-").toLongOrNull() ?: 0L
                results.add(
                    PartCategoryParameterTemplate(
                        id = id,
                        categoryId = catId,
                        parameterTemplateId = tplId,
                        defaultValue = defaultValue
                    )
                )
            }
        }
        return results
    }

    fun insert(categoryUuid: String, parameterTemplateUuid: String, defaultValue: String? = null): PartCategoryParameterTemplate {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            SELECT 1 FROM part_category_parameter_templates
            WHERE categoryUuid = ? AND parameterTemplateUuid = ? AND isDeleted = 0
            LIMIT 1
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, categoryUuid)
            stmt.bindText(2, parameterTemplateUuid)
            if (stmt.step()) {
                throw IllegalArgumentException("قالب المعامل الفني مرتبط بالفعل بهذا التصنيف! (قيد التفرد unique_together مُفعّل)")
            }
        }

        val allocatedId = SqliteNumericIdAllocator.nextId("part_category_parameter_templates", "cat-param-tpl-")
        val uuid = "cat-param-tpl-$allocatedId"
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("""
            INSERT INTO part_category_parameter_templates (
                uuid, categoryUuid, parameterTemplateUuid, defaultValue, version, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, 1, 'PENDING', 0, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, uuid)
            stmt.bindText(2, categoryUuid)
            stmt.bindText(3, parameterTemplateUuid)
            if (defaultValue != null) stmt.bindText(4, defaultValue) else stmt.bindNull(4)
            stmt.bindLong(5, now)
            stmt.step()
        }
        val catId = categoryUuid.removePrefix("cat-").toLongOrNull() ?: 0L
        val tplId = parameterTemplateUuid.removePrefix("param-tpl-").toLongOrNull() ?: 0L
        return PartCategoryParameterTemplate(
            id = allocatedId,
            categoryId = catId,
            parameterTemplateId = tplId,
            defaultValue = defaultValue
        )
    }

    fun delete(id: Long): Boolean {
        val uuid = "cat-param-tpl-$id"
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare("UPDATE part_category_parameter_templates SET isDeleted = 1, syncStatus = 'PENDING', version = version + 1, updatedAt = ? WHERE uuid = ? AND isDeleted = 0").use { stmt ->
            stmt.bindLong(1, now)
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
}

