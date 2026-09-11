package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity

/**
 * تمثيل كيان أمر التصنيع والإنتاج (BuildOrderEntity) في قاعدة بيانات Room المحلية.
 * يحتوي على الفهارس التلقائية `@Index` على `syncStatus`, `isDeleted`, `updatedAt` لسرعة الاستعلام والمزامنة.
 */
data class BuildOrderEntity(
    override val uuid: String,
    val reference: String,
    val title: String = "",
    val partId: Long,
    val partName: String = "",
    val quantity: Double = 1.0,
    val completedQuantity: Double = 0.0,
    val statusCode: Int = 10,
    val batch: String = "",
    val targetDate: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING_PUSH,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : SyncableEntity
