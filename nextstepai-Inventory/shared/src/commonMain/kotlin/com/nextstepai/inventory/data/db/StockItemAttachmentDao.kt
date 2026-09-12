package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus

/**
 * كائن الوصول لبيانات مرفقات عناصر المخزون (StockItemAttachmentDao) باستعلامات معلّمة صريحة (Parameterized Bind Queries).
 */
@Dao
class StockItemAttachmentDao {

    private val selectColumns = """
        uuid, attachmentId, stockItemId, stockItemUuid, attachment, link, comment, uploadDate,
        userId, metadata, syncStatus, isDeleted, updatedAt
    """.trimIndent()

    suspend fun getAttachmentsForStockItem(stockItemId: Long): List<StockItemAttachmentEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<StockItemAttachmentEntity>()

        val sql = """
            SELECT $selectColumns
            FROM stock_item_attachments
            WHERE isDeleted = 0 AND stockItemId = ?
            ORDER BY updatedAt DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, stockItemId)
            while (stmt.step()) {
                results.add(mapStockItemAttachmentEntity(stmt))
            }
        }
        return results
    }

    suspend fun insertOrUpdate(entity: StockItemAttachmentEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("""
            INSERT OR REPLACE INTO stock_item_attachments (
                uuid, attachmentId, stockItemId, stockItemUuid, attachment, link, comment, uploadDate,
                userId, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.attachmentId)
            stmt.bindLong(3, entity.stockItemId)
            stmt.bindText(4, entity.stockItemUuid)
            if (entity.attachment != null) stmt.bindText(5, entity.attachment) else stmt.bindNull(5)
            if (entity.link != null) stmt.bindText(6, entity.link) else stmt.bindNull(6)
            stmt.bindText(7, entity.comment)
            stmt.bindText(8, entity.uploadDate)
            if (entity.userId != null) stmt.bindLong(9, entity.userId) else stmt.bindNull(9)
            stmt.bindText(10, entity.metadata)
            stmt.bindText(11, entity.syncStatus.name)
            stmt.bindLong(12, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(13, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun deleteAttachment(uuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("UPDATE stock_item_attachments SET isDeleted = 1 WHERE uuid = ?").use { stmt ->
            stmt.bindText(1, uuid)
            stmt.step()
        }
    }

    private fun mapStockItemAttachmentEntity(stmt: SQLiteStatement): StockItemAttachmentEntity {
        return StockItemAttachmentEntity(
            uuid = runCatching { stmt.getText(0) }.getOrDefault(""),
            attachmentId = runCatching { stmt.getLong(1) }.getOrDefault(0L),
            stockItemId = runCatching { stmt.getLong(2) }.getOrDefault(0L),
            stockItemUuid = runCatching { stmt.getText(3) }.getOrDefault(""),
            attachment = runCatching { if (stmt.isNull(4)) null else stmt.getText(4) }.getOrNull(),
            link = runCatching { if (stmt.isNull(5)) null else stmt.getText(5) }.getOrNull(),
            comment = runCatching { stmt.getText(6) }.getOrDefault(""),
            uploadDate = runCatching { stmt.getText(7) }.getOrDefault(""),
            userId = runCatching { if (stmt.isNull(8)) null else stmt.getLong(8) }.getOrNull(),
            metadata = runCatching { stmt.getText(9) }.getOrDefault("{}"),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(10)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = runCatching { stmt.getLong(11) != 0L }.getOrDefault(false),
            updatedAt = runCatching { stmt.getLong(12) }.getOrDefault(0L)
        )
    }
}
