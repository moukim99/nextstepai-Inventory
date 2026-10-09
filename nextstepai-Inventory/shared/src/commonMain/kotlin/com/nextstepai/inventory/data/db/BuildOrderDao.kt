package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أوامر الإنتاج والتصنيع (BuildOrderDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class BuildOrderDao {

    private val selectColumns = """
        uuid, reference, title, partId, partName, quantity, completedQuantity, statusCode, batch, targetDate,
        startDate, completionDate, creationDate, parentId, salesOrderId, takeFromLocationId, destinationLocationId,
        issuedBy, responsible, notes, link, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    fun getBuildOrdersPaged(
        partId: Long? = null,
        statusCode: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<BuildOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildOrderEntity>()

        val sql = """
            SELECT $selectColumns
            FROM build_orders
            WHERE isDeleted = 0
              AND (? IS NULL OR partId = ?)
              AND (? IS NULL OR statusCode = ?)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (partId != null) stmt.bindLong(1, partId) else stmt.bindNull(1)
            if (partId != null) stmt.bindLong(2, partId) else stmt.bindNull(2)
            if (statusCode != null) stmt.bindLong(3, statusCode.toLong()) else stmt.bindNull(3)
            if (statusCode != null) stmt.bindLong(4, statusCode.toLong()) else stmt.bindNull(4)
            stmt.bindLong(5, limit.toLong())
            stmt.bindLong(6, offset.toLong())
            while (stmt.step()) {
                results.add(mapBuildEntity(stmt))
            }
        }
        return results
    }

    fun getPendingSyncBuilds(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<BuildOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<BuildOrderEntity>()

        conn.prepare("""
            SELECT $selectColumns
            FROM build_orders
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapBuildEntity(stmt))
            }
        }
        return results
    }

    fun insertOrUpdate(entity: BuildOrderEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO build_orders (
                uuid, reference, title, partId, partName, quantity, completedQuantity, statusCode, batch, targetDate,
                startDate, completionDate, creationDate, parentId, salesOrderId, takeFromLocationId, destinationLocationId,
                issuedBy, responsible, notes, link, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.reference)
            stmt.bindText(3, entity.title)
            stmt.bindLong(4, entity.partId)
            stmt.bindText(5, entity.partName)
            stmt.bindDouble(6, entity.quantity)
            stmt.bindDouble(7, entity.completedQuantity)
            stmt.bindLong(8, entity.statusCode.toLong())
            stmt.bindText(9, entity.batch)
            stmt.bindText(10, entity.targetDate)
            stmt.bindText(11, entity.startDate)
            stmt.bindText(12, entity.completionDate)
            stmt.bindText(13, entity.creationDate)
            if (entity.parentId != null) stmt.bindLong(14, entity.parentId) else stmt.bindNull(14)
            if (entity.salesOrderId != null) stmt.bindLong(15, entity.salesOrderId) else stmt.bindNull(15)
            if (entity.takeFromLocationId != null) stmt.bindLong(16, entity.takeFromLocationId) else stmt.bindNull(16)
            if (entity.destinationLocationId != null) stmt.bindLong(17, entity.destinationLocationId) else stmt.bindNull(17)
            stmt.bindText(18, entity.issuedBy)
            stmt.bindText(19, entity.responsible)
            stmt.bindText(20, entity.notes)
            stmt.bindText(21, entity.link)
            stmt.bindText(22, entity.syncStatus.name)
            stmt.bindLong(23, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(24, entity.updatedAt)
            stmt.step()
        }
    }

    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE build_orders SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    fun findBuildOrder(identifier: String): BuildOrderEntity? {
        if (identifier.isBlank()) return null
        val conn = SqliteDatabaseManager.getConnection()
        val numericId = identifier.toLongOrNull()
        val buildIdStr = if (numericId != null) "build-$numericId" else identifier
        val paddedBo = if (numericId != null) "bo-" + numericId.toString().padStart(3, '0') else identifier
        val boStr = if (numericId != null) "bo-$numericId" else identifier

        val sql = """
            SELECT $selectColumns
            FROM build_orders
            WHERE isDeleted = 0
              AND (
                  uuid = ?
                  OR uuid = ?
                  OR uuid = ?
                  OR uuid = ?
                  OR reference = ?
              )
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, identifier)
            stmt.bindText(2, buildIdStr)
            stmt.bindText(3, paddedBo)
            stmt.bindText(4, boStr)
            stmt.bindText(5, identifier)
            if (stmt.step()) {
                return mapBuildEntity(stmt)
            }
        }
        return null
    }

    fun getBuildOrderByUuid(uuid: String): BuildOrderEntity? = findBuildOrder(uuid)

    fun getBuildOrderById(buildId: Long): BuildOrderEntity? = findBuildOrder(buildId.toString())

    fun updateBuildOrderOutput(
        uuid: String,
        newCompletedQty: Double,
        newStatusCode: Int,
        completionDate: String,
        updatedAt: Long
    ) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE build_orders
            SET completedQuantity = ?,
                statusCode = ?,
                completionDate = CASE WHEN ? != '' THEN ? ELSE completionDate END,
                syncStatus = 'PENDING',
                updatedAt = ?
            WHERE uuid = ?
        """.trimIndent()).use { stmt ->
            stmt.bindDouble(1, newCompletedQty)
            stmt.bindLong(2, newStatusCode.toLong())
            stmt.bindText(3, completionDate)
            stmt.bindText(4, completionDate)
            stmt.bindLong(5, updatedAt)
            stmt.bindText(6, uuid)
            stmt.step()
        }
    }

    fun updateBuildOrderOutputConditional(
        uuid: String,
        expectedStatusCode: Int,
        expectedCompletedQty: Double,
        newCompletedQty: Double,
        newStatusCode: Int,
        completionDate: String,
        updatedAt: Long
    ): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE build_orders
            SET completedQuantity = ?,
                statusCode = ?,
                completionDate = CASE WHEN ? != '' THEN ? ELSE completionDate END,
                syncStatus = 'PENDING',
                updatedAt = ?
            WHERE uuid = ?
              AND statusCode = ?
              AND ABS(completedQuantity - ?) < 0.0001
              AND isDeleted = 0
        """.trimIndent()).use { stmt ->
            stmt.bindDouble(1, newCompletedQty)
            stmt.bindLong(2, newStatusCode.toLong())
            stmt.bindText(3, completionDate)
            stmt.bindText(4, completionDate)
            stmt.bindLong(5, updatedAt)
            stmt.bindText(6, uuid)
            stmt.bindLong(7, expectedStatusCode.toLong())
            stmt.bindDouble(8, expectedCompletedQty)
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

    fun updateBuildStatus(
        uuid: String,
        newStatusCode: Int,
        startDate: String = "",
        updatedAt: Long
    ) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            UPDATE build_orders
            SET statusCode = ?,
                startDate = CASE WHEN ? != '' THEN ? ELSE startDate END,
                syncStatus = 'PENDING',
                updatedAt = ?
            WHERE uuid = ?
        """.trimIndent()).use { stmt ->
            stmt.bindLong(1, newStatusCode.toLong())
            stmt.bindText(2, startDate)
            stmt.bindText(3, startDate)
            stmt.bindLong(4, updatedAt)
            stmt.bindText(5, uuid)
            stmt.step()
        }
    }

    private fun mapBuildEntity(stmt: SQLiteStatement): BuildOrderEntity {
        return BuildOrderEntity(
            uuid = stmt.getText(0),
            reference = stmt.getText(1),
            title = stmt.getText(2),
            partId = stmt.getLong(3),
            partName = stmt.getText(4),
            quantity = stmt.getDouble(5),
            completedQuantity = stmt.getDouble(6),
            statusCode = stmt.getLong(7).toInt(),
            batch = stmt.getText(8),
            targetDate = stmt.getText(9),
            startDate = stmt.getText(10),
            completionDate = stmt.getText(11),
            creationDate = stmt.getText(12),
            parentId = if (stmt.isNull(13)) null else stmt.getLong(13),
            salesOrderId = if (stmt.isNull(14)) null else stmt.getLong(14),
            takeFromLocationId = if (stmt.isNull(15)) null else stmt.getLong(15),
            destinationLocationId = if (stmt.isNull(16)) null else stmt.getLong(16),
            issuedBy = stmt.getText(17),
            responsible = stmt.getText(18),
            notes = stmt.getText(19),
            link = stmt.getText(20),
            syncStatus = SyncStatus.fromString(stmt.getText(21)),
            isDeleted = stmt.getLong(22) != 0L,
            updatedAt = stmt.getLong(23)
        )
    }
}

