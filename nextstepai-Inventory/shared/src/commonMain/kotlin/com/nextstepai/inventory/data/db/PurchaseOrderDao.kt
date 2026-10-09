package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أوامر الشراء (PurchaseOrderDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class PurchaseOrderDao {

    fun getOrdersPaged(
        supplierId: Long? = null,
        statusCode: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<PurchaseOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PurchaseOrderEntity>()

        val sql = """
            SELECT uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, sourceType, sourceReferenceUuid, destinationLocationUuid, syncStatus, isDeleted, updatedAt
            FROM purchase_orders
            WHERE isDeleted = 0
              AND (? IS NULL OR supplierId = ?)
              AND (? IS NULL OR statusCode = ?)
            ORDER BY updatedAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (supplierId != null) stmt.bindLong(1, supplierId) else stmt.bindNull(1)
            if (supplierId != null) stmt.bindLong(2, supplierId) else stmt.bindNull(2)
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

    fun getLinesForOrderPaged(
        orderUuid: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<PurchaseOrderLineEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PurchaseOrderLineEntity>()

        conn.prepare("""
            SELECT uuid, orderUuid, supplierPartId, quantity, receivedQuantity, purchasePrice, syncStatus, isDeleted, updatedAt
            FROM purchase_order_lines
            WHERE isDeleted = 0 AND orderUuid = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, orderUuid)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapLineEntity(stmt))
            }
        }
        return results
    }

    fun getPendingSyncOrders(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<PurchaseOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PurchaseOrderEntity>()

        conn.prepare("""
            SELECT uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, sourceType, sourceReferenceUuid, destinationLocationUuid, syncStatus, isDeleted, updatedAt
            FROM purchase_orders
            WHERE syncStatus = ?
            ORDER BY updatedAt ASC
            LIMIT ? OFFSET ?
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, status.name)
            stmt.bindLong(2, limit.toLong())
            stmt.bindLong(3, offset.toLong())
            while (stmt.step()) {
                results.add(mapOrderEntity(stmt))
            }
        }
        return results
    }

    fun insertOrUpdateOrder(entity: PurchaseOrderEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO purchase_orders (uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, sourceType, sourceReferenceUuid, destinationLocationUuid, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.reference)
            stmt.bindLong(3, entity.supplierId)
            stmt.bindText(4, entity.supplierName)
            stmt.bindLong(5, entity.statusCode.toLong())
            stmt.bindText(6, entity.description)
            stmt.bindText(7, entity.orderCurrency)
            stmt.bindText(8, entity.targetDate)
            stmt.bindDouble(9, entity.totalCost)
            stmt.bindText(10, entity.sourceType)
            if (entity.sourceReferenceUuid != null) stmt.bindText(11, entity.sourceReferenceUuid) else stmt.bindNull(11)
            if (entity.destinationLocationUuid != null) stmt.bindText(12, entity.destinationLocationUuid) else stmt.bindNull(12)
            stmt.bindText(13, entity.syncStatus.name)
            stmt.bindLong(14, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(15, entity.updatedAt)
            stmt.step()
        }
    }

    fun insertOrUpdateLine(entity: PurchaseOrderLineEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO purchase_order_lines (uuid, orderUuid, supplierPartId, quantity, receivedQuantity, purchasePrice, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.orderUuid)
            stmt.bindLong(3, entity.supplierPartId)
            stmt.bindDouble(4, entity.quantity)
            stmt.bindDouble(5, entity.receivedQuantity)
            stmt.bindDouble(6, entity.purchasePrice)
            stmt.bindText(7, entity.syncStatus.name)
            stmt.bindLong(8, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(9, entity.updatedAt)
            stmt.step()
        }
    }

    fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
        if (uuids.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        for (uuid in uuids) {
            conn.prepare("UPDATE purchase_orders SET syncStatus = ? WHERE uuid = ?").use { stmt ->
                stmt.bindText(1, newStatus.name)
                stmt.bindText(2, uuid)
                stmt.step()
            }
        }
    }

    fun getOrderByUuid(orderUuid: String): PurchaseOrderEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, sourceType, sourceReferenceUuid, destinationLocationUuid, syncStatus, isDeleted, updatedAt
            FROM purchase_orders
            WHERE isDeleted = 0 AND uuid = ?
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, orderUuid)
            if (stmt.step()) {
                return mapOrderEntity(stmt)
            }
        }
        return null
    }

    fun getOrderById(orderId: Long): PurchaseOrderEntity? {
        val direct = getOrderByUuid("po-$orderId")
        if (direct != null) return direct

        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, sourceType, sourceReferenceUuid, destinationLocationUuid, syncStatus, isDeleted, updatedAt
            FROM purchase_orders
            WHERE isDeleted = 0 AND (uuid = ? OR uuid LIKE ?)
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, orderId.toString())
            stmt.bindText(2, "%-$orderId")
            if (stmt.step()) {
                return mapOrderEntity(stmt)
            }
        }
        return null
    }

    fun getOrderByReference(reference: String): PurchaseOrderEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, sourceType, sourceReferenceUuid, destinationLocationUuid, syncStatus, isDeleted, updatedAt
            FROM purchase_orders
            WHERE isDeleted = 0 AND reference = ?
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, reference.trim())
            if (stmt.step()) {
                return mapOrderEntity(stmt)
            }
        }
        return null
    }

    fun getLinesForOrder(orderUuid: String): List<PurchaseOrderLineEntity> {
        return getLinesForOrderPaged(orderUuid, limit = 500, offset = 0)
    }

    fun getLineByUuid(lineUuid: String): PurchaseOrderLineEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT uuid, orderUuid, supplierPartId, quantity, receivedQuantity, purchasePrice, syncStatus, isDeleted, updatedAt
            FROM purchase_order_lines
            WHERE isDeleted = 0 AND uuid = ?
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, lineUuid)
            if (stmt.step()) {
                return mapLineEntity(stmt)
            }
        }
        return null
    }

    fun getLineById(lineId: Long): PurchaseOrderLineEntity? {
        val direct = getLineByUuid("po-line-$lineId")
        if (direct != null) return direct

        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT uuid, orderUuid, supplierPartId, quantity, receivedQuantity, purchasePrice, syncStatus, isDeleted, updatedAt
            FROM purchase_order_lines
            WHERE isDeleted = 0 AND (uuid = ? OR uuid LIKE ?)
            LIMIT 1
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, lineId.toString())
            stmt.bindText(2, "%-$lineId")
            if (stmt.step()) {
                return mapLineEntity(stmt)
            }
        }
        return null
    }

    fun updateLineReceivedQuantity(lineUuid: String, receivedQuantity: Double, updatedAt: Long) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            UPDATE purchase_order_lines
            SET receivedQuantity = ?, updatedAt = ?, syncStatus = 'PENDING'
            WHERE uuid = ?
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindDouble(1, receivedQuantity)
            stmt.bindLong(2, updatedAt)
            stmt.bindText(3, lineUuid)
            stmt.step()
        }
    }

    fun updateLineReceivedQuantityConditional(
        lineUuid: String,
        deltaQty: Double,
        updatedAt: Long
    ): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            UPDATE purchase_order_lines
            SET receivedQuantity = receivedQuantity + ?,
                updatedAt = ?,
                syncStatus = 'PENDING'
            WHERE uuid = ?
              AND (receivedQuantity + ?) <= (quantity + 0.0001)
              AND isDeleted = 0
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindDouble(1, deltaQty)
            stmt.bindLong(2, updatedAt)
            stmt.bindText(3, lineUuid)
            stmt.bindDouble(4, deltaQty)
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

    fun updateOrderStatus(orderUuid: String, newStatusCode: Int, updatedAt: Long) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            UPDATE purchase_orders
            SET statusCode = ?, updatedAt = ?, syncStatus = 'PENDING'
            WHERE uuid = ?
        """.trimIndent()
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, newStatusCode.toLong())
            stmt.bindLong(2, updatedAt)
            stmt.bindText(3, orderUuid)
            stmt.step()
        }
    }

    private fun mapOrderEntity(stmt: SQLiteStatement): PurchaseOrderEntity {
        return PurchaseOrderEntity(
            uuid = stmt.getText(0),
            reference = stmt.getText(1),
            supplierId = stmt.getLong(2),
            supplierName = stmt.getText(3),
            statusCode = stmt.getLong(4).toInt(),
            description = stmt.getText(5),
            orderCurrency = stmt.getText(6),
            targetDate = stmt.getText(7),
            totalCost = stmt.getDouble(8),
            sourceType = runCatching { stmt.getText(9) }.getOrDefault("MANUAL"),
            sourceReferenceUuid = if (stmt.isNull(10)) null else stmt.getText(10),
            destinationLocationUuid = if (stmt.isNull(11)) null else stmt.getText(11),
            syncStatus = SyncStatus.fromString(stmt.getText(12)),
            isDeleted = stmt.getLong(13) != 0L,
            updatedAt = stmt.getLong(14)
        )
    }

    private fun mapLineEntity(stmt: SQLiteStatement): PurchaseOrderLineEntity {
        return PurchaseOrderLineEntity(
            uuid = stmt.getText(0),
            orderUuid = stmt.getText(1),
            supplierPartId = stmt.getLong(2),
            quantity = stmt.getDouble(3),
            receivedQuantity = stmt.getDouble(4),
            purchasePrice = stmt.getDouble(5),
            syncStatus = SyncStatus.fromString(stmt.getText(6)),
            isDeleted = stmt.getLong(7) != 0L,
            updatedAt = stmt.getLong(8)
        )
    }
}
