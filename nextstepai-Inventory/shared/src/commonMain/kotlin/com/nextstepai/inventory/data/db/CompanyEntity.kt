package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity

/**
 * تمثيل كيان الشركة (CompanyEntity) في قاعدة بيانات Room المحلية.
 * يحتوي على الفهارس التلقائية `@Index` على `syncStatus`, `isDeleted`, `updatedAt` لسرعة المزامنة والاستعلام.
 */
data class CompanyEntity(
    override val uuid: String,
    val name: String,
    val description: String = "",
    val phone: String = "",
    val email: String = "",
    val isSupplier: Boolean = true,
    val isManufacturer: Boolean = false,
    val isCustomer: Boolean = false,
    val currency: String = "USD",
    val logoPath: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING_PUSH,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : SyncableEntity
