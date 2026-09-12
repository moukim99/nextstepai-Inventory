package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import com.nextstepai.inventory.util.DateTimeUtils
import kotlin.time.Clock

/**
 * تمثيل كيان مرفقات المخزون (StockItemAttachmentEntity) في قاعدة بيانات Room المحلية مع تاريخ الرفع اللحظي الديناميكي.
 */
@Entity(tableName = "stock_item_attachments")
data class StockItemAttachmentEntity(
    @PrimaryKey
    override val uuid: String,
    val attachmentId: Long,
    val stockItemId: Long,
    val stockItemUuid: String = "",
    val attachment: String? = null,
    val link: String? = null,
    val comment: String = "",
    val uploadDate: String = DateTimeUtils.getCurrentDate(),
    val userId: Long? = null,
    val metadata: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
