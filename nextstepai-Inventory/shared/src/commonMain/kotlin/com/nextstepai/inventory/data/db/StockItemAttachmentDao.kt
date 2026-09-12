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
        uuid, attachmentId, stockItemId, attachment, link, comment, uploadDate,
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
                uuid, attachmentId, stockItemId, attachment, link, comment, uploadDate,
                userId, metadata, syncStatus, isDeleted, updatedAt
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindLong(2, entity.attachmentId)
            stmt.bindLong(3, entity.stockItemId)
            if (entity.attachment != null) stmt.bindText(4, entity.attachment) else stmt.bindNull(4)
            if (entity.link != null) stmt.bindText(5, entity.link) else stmt.bindNull(5)
            stmt.bindText(6, entity.comment)
            stmt.bindText(7, entity.uploadDate)
            if (entity.userId != null) stmt.bindLong(8, entity.userId) else stmt.bindNull(8)
            stmt.bindText(9, entity.metadata)
            stmt.bindText(10, entity.syncStatus.name)
            stmt.bindLong(11, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(12, entity.updatedAt)
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
            uuid = stmt.getText(0),
            attachmentId = stmt.getLong(1),
            stockItemId = stmt.getLong(2),
            attachment = if (stmt.isNull(3)) null else stmt.getText(3),
            link = if (stmt.isNull(4)) null else stmt.getText(4),
            comment = stmt.getText(5),
            uploadDate = stmt.getText(6),
            userId = if (stmt.isNull(7)) null else stmt.getLong(7),
            metadata = stmt.getText(8),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(9)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(10) != 0L,
            updatedAt = stmt.getLong(11)
        )
    }
}
