package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات أوامر الشراء (PurchaseOrderDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class PurchaseOrderDao {

    suspend fun getOrdersPaged(
        supplierId: Long? = null,
        statusCode: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<PurchaseOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PurchaseOrderEntity>()

        val sql = """
            SELECT uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, syncStatus, isDeleted, updatedAt
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

    suspend fun getLinesForOrderPaged(
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

    suspend fun getPendingSyncOrders(status: SyncStatus = SyncStatus.PENDING, limit: Int = 50, offset: Int = 0): List<PurchaseOrderEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<PurchaseOrderEntity>()

        conn.prepare("""
            SELECT uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, syncStatus, isDeleted, updatedAt
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

    suspend fun insertOrUpdateOrder(entity: PurchaseOrderEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO purchase_orders (uuid, reference, supplierId, supplierName, statusCode, description, orderCurrency, targetDate, totalCost, syncStatus, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.bindText(10, entity.syncStatus.name)
            stmt.bindLong(11, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(12, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun insertOrUpdateLine(entity: PurchaseOrderLineEntity) {
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

    suspend fun updateSyncStatusForUuids(uuids: List<String>, newStatus: SyncStatus) {
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
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(9)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(10) != 0L,
            updatedAt = stmt.getLong(11)
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
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(6)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(7) != 0L,
            updatedAt = stmt.getLong(8)
        )
    }
}
