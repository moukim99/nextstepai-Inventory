package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان أمر التصنيع والإنتاج (BuildOrderEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "build_orders")
data class BuildOrderEntity(
    @PrimaryKey
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
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
