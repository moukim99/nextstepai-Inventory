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
        uuid, resultId, stockItemId, stockItemUuid, templateId, test, result, value, attachment,
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
                uuid, resultId, stockItemId, stockItemUuid, templateId, test, result, value, attachment,
                notes, date, userId, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.resultId)
            stmt.bindLong(3, entity.stockItemId)
            stmt.bindText(4, entity.stockItemUuid)
            if (entity.templateId != null) stmt.bindLong(5, entity.templateId) else stmt.bindNull(5)
            stmt.bindText(6, entity.test)
            stmt.bindLong(7, if (entity.result) 1L else 0L)
            stmt.bindText(8, entity.value)
            stmt.bindText(9, entity.attachment)
            stmt.bindText(10, entity.notes)
            stmt.bindText(11, entity.date)
            if (entity.userId != null) stmt.bindLong(12, entity.userId) else stmt.bindNull(12)
            stmt.bindText(13, entity.metadata)
            stmt.bindText(14, entity.syncStatus.name)
            stmt.bindLong(15, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(16, entity.updatedAt)
            stmt.step()
        }
    }

    private fun mapStockItemTestResultEntity(stmt: SQLiteStatement): StockItemTestResultEntity {
        return StockItemTestResultEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            resultId = runCatching { stmt.getLong(1) }.getOrDefault(0L),
            stockItemId = runCatching { stmt.getLong(2) }.getOrDefault(0L),
            stockItemUuid = runCatching { stmt.getText(3) }.getOrDefault(""),
            templateId = runCatching { if (stmt.isNull(4)) null else stmt.getLong(4) }.getOrNull(),
            test = runCatching { stmt.getText(5) }.getOrDefault(""),
            result = runCatching { stmt.getLong(6) != 0L }.getOrDefault(true),
            value = runCatching { stmt.getText(7) }.getOrDefault("Passed"),
            attachment = runCatching { stmt.getText(8) }.getOrDefault(""),
            notes = runCatching { stmt.getText(9) }.getOrDefault(""),
            date = runCatching { stmt.getText(10) }.getOrDefault(""),
            userId = runCatching { if (stmt.isNull(11)) null else stmt.getLong(11) }.getOrNull(),
            metadata = runCatching { stmt.getText(12) }.getOrDefault("{}"),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(13)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(14) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(15) }.getOrDefault(0L)
        )
    }
}
