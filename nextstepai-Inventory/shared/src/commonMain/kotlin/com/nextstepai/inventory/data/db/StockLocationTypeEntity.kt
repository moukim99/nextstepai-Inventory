package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان نوع موقع التخزين (StockLocationTypeEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "stock_location_types")
data class StockLocationTypeEntity(
    @PrimaryKey
    override val uuid: String,
    val typeId: Long,
    val name: String,
    val description: String = "",
    val icon: String = "warehouse",
    val customIcon: String = "",
    val metadata: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
