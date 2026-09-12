package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات نتائج فحوص الجودة (StockItemTestResultDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockItemTestResultDao {

    private val selectColumns = """
        uuid, resultId, stockItemId, templateId, test, result, value, attachment,
        notes, date, userId, metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getTestResultsForStockItem(stockItemId: Long): List<StockItemTestResultEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemTestResultEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_item_test_results
            WHERE isDeleted = 0 AND stockItemId = ?
            ORDER BY updatedAt DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, stockItemId)
            while (stmt.step()) {
                results.add(mapStockItemTestResultEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: StockItemTestResultEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_item_test_results (
                uuid, resultId, stockItemId, templateId, test, result, value, attachment,
                notes, date, userId, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.resultId)
            stmt.bindLong(3, entity.stockItemId)
            if (entity.templateId != null) stmt.bindLong(4, entity.templateId) else stmt.bindNull(4)
            stmt.bindText(5, entity.test)
            stmt.bindLong(6, if (entity.result) 1L else 0L)
            stmt.bindText(7, entity.value)
            stmt.bindText(8, entity.attachment)
            stmt.bindText(9, entity.notes)
            stmt.bindText(10, entity.date)
            if (entity.userId != null) stmt.bindLong(11, entity.userId) else stmt.bindNull(11)
            stmt.bindText(12, entity.metadata)
            stmt.bindText(13, entity.syncStatus.name)
            stmt.bindLong(14, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(15, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapStockItemTestResultEntity(stmt: SQLiteStatement): StockItemTestResultEntity {
        return StockItemTestResultEntity(
            uuid = stmt.getText(0),
            resultId = stmt.getLong(1),
            stockItemId = stmt.getLong(2),
            templateId = if (stmt.isNull(3)) null else stmt.getLong(3),
            test = stmt.getText(4),
            result = stmt.getLong(5) != 0L,
            value = stmt.getText(6),
            attachment = stmt.getText(7),
            notes = stmt.getText(8),
            date = stmt.getText(9),
            userId = if (stmt.isNull(10)) null else stmt.getLong(10),
            metadata = stmt.getText(11),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(12)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(13) != 0L,
            updatedAt = stmt.getLong(14)
        )
    }
}
