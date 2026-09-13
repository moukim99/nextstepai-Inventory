package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات سجل الإشعارات (NotificationHistoryDao) باستعلامات صريحة.
 */
@Dao
class NotificationHistoryDao {

    suspend fun getAllNotifications(limit: Int = 50, offset: Int = 0): List<NotificationHistoryEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<NotificationHistoryEntity>()
        val sql = """
            SELECT uuid, title, message, notificationType, targetEntityUuid, companyUuid, deepLink, scheduledDate, isRead, isTriggered, createdAt, syncStatus, isDeleted, updatedAt
            FROM notifications_history
            WHERE isDeleted = 0
            ORDER BY scheduledDate DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, limit.toLong())
            stmt.bindLong(2, offset.toLong())
            while (stmt.step()) {
                results.add(mapNotificationHistoryEntity(stmt))
            }
        }
        return results
    }

    suspend fun getUnreadNotifications(): List<NotificationHistoryEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<NotificationHistoryEntity>()
        val sql = """
            SELECT uuid, title, message, notificationType, targetEntityUuid, companyUuid, deepLink, scheduledDate, isRead, isTriggered, createdAt, syncStatus, isDeleted, updatedAt
            FROM notifications_history
            WHERE isRead = 0 AND isDeleted = 0
            ORDER BY scheduledDate DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            while (stmt.step()) {
                results.add(mapNotificationHistoryEntity(stmt))
            }
        }
        return results
    }

    suspend fun getUnreadCount(): Int {
        val conn = SqliteDatabaseManager.getConnection()
        var count = 0
        val sql = "SELECT COUNT(*) FROM notifications_history WHERE isRead = 0 AND isDeleted = 0"
        conn.prepare(sql).use { stmt ->
            if (stmt.step()) {
                count = stmt.getLong(0).toInt()
            }
        }
        return count
    }

    suspend fun getPendingTriggerNotifications(currentTimeMs: Long): List<NotificationHistoryEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<NotificationHistoryEntity>()
        val sql = """
            SELECT uuid, title, message, notificationType, targetEntityUuid, companyUuid, deepLink, scheduledDate, isRead, isTriggered, createdAt, syncStatus, isDeleted, updatedAt
            FROM notifications_history
            WHERE isTriggered = 0 AND scheduledDate <= ? AND isDeleted = 0
            ORDER BY scheduledDate ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, currentTimeMs)
            while (stmt.step()) {
                results.add(mapNotificationHistoryEntity(stmt))
            }
        }
        return results
    }

    suspend fun getByUuid(uuid: String): NotificationHistoryEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        var entity: NotificationHistoryEntity? = null
        val sql = """
            SELECT uuid, title, message, notificationType, targetEntityUuid, companyUuid, deepLink, scheduledDate, isRead, isTriggered, createdAt, syncStatus, isDeleted, updatedAt
            FROM notifications_history
            WHERE uuid = ? AND isDeleted = 0
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, uuid)
            if (stmt.step()) {
                entity = mapNotificationHistoryEntity(stmt)
            }
        }
        return entity
    }

    suspend fun insertOrUpdate(entity: NotificationHistoryEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            INSERT OR REPLACE INTO notifications_history (
                uuid, title, message, notificationType, targetEntityUuid, companyUuid, deepLink, scheduledDate, isRead, isTriggered, createdAt, syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.title)
            stmt.bindText(3, entity.message)
            stmt.bindText(4, entity.notificationType)
            stmt.bindText(5, entity.targetEntityUuid)
            if (entity.companyUuid != null) stmt.bindText(6, entity.companyUuid) else stmt.bindNull(6)
            if (entity.deepLink != null) stmt.bindText(7, entity.deepLink) else stmt.bindNull(7)
            stmt.bindLong(8, entity.scheduledDate)
            stmt.bindLong(9, if (entity.isRead) 1L else 0L)
            stmt.bindLong(10, if (entity.isTriggered) 1L else 0L)
            stmt.bindLong(11, entity.createdAt)
            stmt.bindText(12, entity.syncStatus.name)
            stmt.bindLong(13, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(14, entity.updatedAt)
            stmt.step()
        }
    }

    suspend fun markAsRead(uuid: String, updatedAtMs: Long = Clock.System.now().toEpochMilliseconds()) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE notifications_history SET isRead = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?"
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, updatedAtMs)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }

    suspend fun markAsTriggered(uuid: String, updatedAtMs: Long = Clock.System.now().toEpochMilliseconds()) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE notifications_history SET isTriggered = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?"
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, updatedAtMs)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }

    suspend fun delete(uuid: String, updatedAtMs: Long = Clock.System.now().toEpochMilliseconds()) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE notifications_history SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = ? WHERE uuid = ?"
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, updatedAtMs)
            stmt.bindText(2, uuid)
            stmt.step()
        }
    }

    private fun mapNotificationHistoryEntity(stmt: SQLiteStatement): NotificationHistoryEntity {
        return NotificationHistoryEntity(
            uuid = stmt.getText(0),
            title = stmt.getText(1),
            message = stmt.getText(2),
            notificationType = stmt.getText(3),
            targetEntityUuid = stmt.getText(4),
            companyUuid = if (stmt.isNull(5)) null else stmt.getText(5),
            deepLink = if (stmt.isNull(6)) null else stmt.getText(6),
            scheduledDate = stmt.getLong(7),
            isRead = stmt.getLong(8) != 0L,
            isTriggered = stmt.getLong(9) != 0L,
            createdAt = stmt.getLong(10),
            syncStatus = runCatching { SyncStatus.valueOf(stmt.getText(11)) }.getOrDefault(SyncStatus.PENDING),
            isDeleted = stmt.getLong(12) != 0L,
            updatedAt = stmt.getLong(13)
        )
    }
}
