package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أوامر البيع وبنودها (SalesOrderDao) باستعلامات صريحة ومؤمنة في SQLite.
 */
@Dao
class SalesOrderDao {

    private val selectOrderColumns = """
        uuid, reference, customerId, customerUuid, customerName, statusCode, description, orderCurrency, targetDate, totalPrice, sourceType, sourceReferenceUuid, notes, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    private val selectLineColumns = """
        uuid, orderUuid, orderId, partId, partUuid, partName, quantity, unitPrice, allocatedQuantity, shippedQuantity, notes, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getOrdersPaged(
        customerId: Long? = null,
        statusCode: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<SalesOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<SalesOrderEntity>()

        val sql = """
            SELECT $selectOrderColumns
            FROM sales_orders
            WHERE isDeleted = 0
              AND (? IS NULL OR customerId = ?)
              AND (? IS NULL OR statusCode = ?)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (customerId != null) stmt.bindLong(1, customerId) else stmt.bindNull(1)
            if (customerId != null) stmt.bindLong(2, customerId) else stmt.bindNull(2)
            if (statusCode != null) stmt.bindLong(3, statusCode.toLong()) else stmt.bindNull(3)
            if (statusCode != null) stmt.bindLong(4, statusCode.toLong()) else stmt.bindNull(4)
            stmt.bindLong(5, limit.toLong())
            stmt.bindLong(6, offset.toLong())
            while (stmt.step()) {
                results.add(mapOrderEntity(stmt))
            }
        }
        return results
    }

    suspend fun getLinesForOrderUuid(orderUuid: String): List<SalesOrderLineEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<SalesOrderLineEntity>()

        conn.prepare("""
            SELECT $selectLineColumns
            FROM sales_order_lines
            WHERE isDeleted = 0 AND orderUuid = ?
            ORDER BY updatedAt ASC
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, orderUuid)
            while (stmt.step()) {
                results.add(mapLineEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdateOrder(entity: SalesOrderEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO sales_orders (
                uuid, reference, customerId, customerUuid, customerName, statusCode, description, orderCurrency, targetDate, totalPrice, sourceType, sourceReferenceUuid, notes, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.reference)
            stmt.bindLong(3, entity.customerId)
            stmt.bindText(4, entity.customerUuid)
            stmt.bindText(5, entity.customerName)
            stmt.bindLong(6, entity.statusCode.toLong())
            stmt.bindText(7, entity.description)
            stmt.bindText(8, entity.orderCurrency)
            stmt.bindText(9, entity.targetDate)
            stmt.bindDouble(10, entity.totalPrice)
            stmt.bindText(11, entity.sourceType)
            if (entity.sourceReferenceUuid != null) stmt.bindText(12, entity.sourceReferenceUuid) else stmt.bindNull(12)
            stmt.bindText(13, entity.notes)
            stmt.bindText(14, entity.syncStatus.name)
            stmt.bindLong(15, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(16, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun insertOrUpdateLine(entity: SalesOrderLineEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO sales_order_lines (
                uuid, orderUuid, orderId, partId, partUuid, partName, quantity, unitPrice, allocatedQuantity, shippedQuantity, notes, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.orderUuid)
            stmt.bindLong(3, entity.orderId)
            stmt.bindLong(4, entity.partId)
            stmt.bindText(5, entity.partUuid)
            stmt.bindText(6, entity.partName)
            stmt.bindDouble(7, entity.quantity)
            stmt.bindDouble(8, entity.unitPrice)
            stmt.bindDouble(9, entity.allocatedQuantity)
            stmt.bindDouble(10, entity.shippedQuantity)
            stmt.bindText(11, entity.notes)
            stmt.bindText(12, entity.syncStatus.name)
            stmt.bindLong(13, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(14, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapOrderEntity(stmt: SQLiteStatement): SalesOrderEntity {
        return SalesOrderEntity(
            uuid = stmt.getText(0),
            reference = stmt.getText(1),
            customerId = stmt.getLong(2),
            customerUuid = stmt.getText(3),
            customerName = stmt.getText(4),
            statusCode = stmt.getLong(5).toInt(),
            description = stmt.getText(6),
            orderCurrency = stmt.getText(7),
            targetDate = stmt.getText(8),
            totalPrice = stmt.getDouble(9),
            sourceType = runCatching { stmt.getText(10) }.getOrDefault("MANUAL"),
            sourceReferenceUuid = if (stmt.isNull(11)) null else stmt.getText(11),
            notes = stmt.getText(12),
            syncStatus = SyncStatus.fromString(stmt.getText(13)),
            isDeleted = stmt.getLong(14) != 0L,
            updatedAt = stmt.getLong(15)
        )
    }

    private fun mapLineEntity(stmt: SQLiteStatement): SalesOrderLineEntity {
        return SalesOrderLineEntity(
            uuid = stmt.getText(0),
            orderUuid = stmt.getText(1),
            orderId = stmt.getLong(2),
            partId = stmt.getLong(3),
            partUuid = stmt.getText(4),
            partName = stmt.getText(5),
            quantity = stmt.getDouble(6),
            unitPrice = stmt.getDouble(7),
            allocatedQuantity = stmt.getDouble(8),
            shippedQuantity = stmt.getDouble(9),
            notes = stmt.getText(10),
            syncStatus = SyncStatus.fromString(stmt.getText(11)),
            isDeleted = stmt.getLong(12) != 0L,
            updatedAt = stmt.getLong(13)
        )
    }
}
