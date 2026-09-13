package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان سجل الإشعارات والتنبيهات (NotificationHistoryEntity) في قاعدة البيانات المحلية.
 */
@Entity(
    tableName = "notifications_history",
    indices = [
        Index("targetEntityUuid"),
        Index("companyUuid"),
        Index(value = ["isTriggered", "scheduledDate"]),
        Index(value = ["isRead", "isDeleted"])
    ]
)
data class NotificationHistoryEntity(
    @PrimaryKey
    override val uuid: String,
    val title: String,
    val message: String,
    val notificationType: String = "COMPANY_DOC_EXPIRY",
    val targetEntityUuid: String,
    val companyUuid: String? = null,
    val deepLink: String? = null,
    val scheduledDate: Long,
    val isRead: Boolean = false,
    val isTriggered: Boolean = false,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity
