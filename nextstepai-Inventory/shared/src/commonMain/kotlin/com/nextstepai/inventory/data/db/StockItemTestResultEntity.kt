package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان نتائج فحوصات الجودة (StockItemTestResultEntity) في قاعدة بيانات Room المحلية يدعم المرجع المحلي stockItemUuid.
 */
@Entity(tableName = "stock_item_test_results")
data class StockItemTestResultEntity(
    @PrimaryKey
    override val uuid: String,
    val resultId: Long,
    val stockItemId: Long,
    val stockItemUuid: String = "",
    val templateId: Long? = null,
    val test: String,
    val result: Boolean = true,
    val value: String = "Passed",
    val attachment: String = "",
    val notes: String = "",
    val date: String = "2025-02-15",
    val userId: Long? = null,
    val metadata: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
