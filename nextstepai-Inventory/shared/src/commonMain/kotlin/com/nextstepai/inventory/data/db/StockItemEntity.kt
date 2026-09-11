package com.nextstepai.inventory.data.db

import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity

/**
 * تمثيل كيان الوحدة المخزنية (StockItemEntity) المادية في قاعدة بيانات Room المحنّطة.
 * يحتوي على الفهارس المخصصة `@Index` على `syncStatus`, `isDeleted`, `updatedAt` لضمان سرعة الاستعلام والمزامنة.
 */
data class StockItemEntity(
    override val uuid: String,
    val partId: Long,
    val locationId: Long? = null,
    val quantity: Double = 1.0,
    val serial: String = "",
    val batch: String = "",
    val statusCode: Int = 10,
    val packaging: String = "Box",
    val expiryDate: String = "",
    val notes: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING_PUSH,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = System.currentTimeMillis()
) : SyncableEntity
