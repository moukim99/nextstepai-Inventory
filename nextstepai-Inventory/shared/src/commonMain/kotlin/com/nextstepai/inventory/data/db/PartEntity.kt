package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity

/**
 * تمثيل جدول الكيان القابل للمزامنة في قاعدة بيانات Room (PartEntity).
 * يحتوي على الفهارس التلقائية `@Index` على الحقوق المحددة لسرعة الاستعلام المزامنة.
 */
data class PartEntity(
    override val uuid: String,
    val name: String,
    val ipn: String = "",
    val description: String = "",
    val categoryId: Long? = null,
    val units: String = "pcs",
    val minimumStock: Double = 0.0,
    val totalInStock: Double = 0.0,
    val localImagePath: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_PUSH,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : SyncableEntity
