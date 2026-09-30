package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * تمثيل كيان موقع التخزين (StockLocationEntity) المعتمد بنسبة 100% على UUIDv7.
 */
@Entity(tableName = "stock_locations")
data class StockLocationEntity(
    @PrimaryKey
    override val uuid: String = AppUuid.generate(),
    val name: String,
    val description: String = "",
    val parentUuid: String? = null,
    val structural: Boolean = false,
    val external: Boolean = false,
    val locationTypeUuid: String? = null,
    val locationType: String = "SHELF",
    val customCapacity: Double? = null,
    val isBulkGenerated: Boolean = false,
    val address: String = "",
    val icon: String = "warehouse",
    val customIcon: String = "",
    val level: Int = 0,
    val lft: Int = 0,
    val rght: Int = 0,
    val treeId: Int = 1,
    val metadata: String = "{}",
    val version: Int = 1,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastModifiedByDeviceUuid: String? = null
) : SyncableEntity
