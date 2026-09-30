package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * تمثيل كيان تصنيف القطع (PartCategoryEntity) في قاعدة بيانات Room والـ SQLite المعتمد بنسبة 100% على UUIDv7.
 */
@Entity(tableName = "part_categories")
data class PartCategoryEntity(
    @PrimaryKey
    override val uuid: String = AppUuid.generate(),
    val name: String,
    val parentUuid: String? = null,
    val description: String = "",
    val structural: Boolean = false,
    val defaultLocationUuid: String? = null,
    val version: Int = 1,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastModifiedByDeviceUuid: String? = null
) : SyncableEntity
