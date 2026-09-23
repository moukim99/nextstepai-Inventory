package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان بند قائمة المواد (BomItemEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(
    tableName = "bom_items",
    indices = [Index(value = ["syncStatus", "isDeleted", "updatedAt"])]
)
data class BomItemEntity(
    @PrimaryKey
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
    val phaseUuid: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
