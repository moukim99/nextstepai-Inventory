package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان موقع التخزين (StockLocationEntity) في قاعدة بيانات Room المحتوية على 15 حقل + حقول المزامنة بمرجع المحلي (parentUuid).
 */
@Entity(tableName = "stock_locations")
data class StockLocationEntity(
    @PrimaryKey
    override val uuid: String,
    val locationId: Long,
    val name: String,
    val description: String = "",
    val parentId: Long? = null,
    val parentUuid: String? = null,
    val structural: Boolean = false,
    val external: Boolean = false,
    val locationType: String = "SHELF",
    val ownerId: Long? = null,
    val icon: String = "warehouse",
    val customIcon: String = "",
    val level: Int = 0,
    val lft: Int = 0,
    val rght: Int = 0,
    val treeId: Int = 1,
    val metadata: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
