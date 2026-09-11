package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity

/**
 * تمثيل كيان بند قائمة المواد (BomItemEntity) في قاعدة بيانات Room المحلية.
 * يحتوي على الفهارس المخصصة `@Index` على `syncStatus`, `isDeleted`, `updatedAt` لسرعة المزامنة والاستعلام.
 */
data class BomItemEntity(
    override val uuid: String,
    val partId: Long,
    val subPartId: Long,
    val quantity: Double,
    val reference: String = "",
    val optional: Boolean = false,
    val consumable: Boolean = false,
    val allowVariants: Boolean = false,
    val inherited: Boolean = false,
    val note: String = "",
    val checksum: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING_PUSH,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : SyncableEntity
