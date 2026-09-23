package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

@Entity(tableName = "manufacturing_phases")
data class ManufacturingPhaseEntity(
    @PrimaryKey
    val uuid: String,
    val id: Long = 0L,
    val partUuid: String? = null,
    val name: String,
    val sequenceOrder: Int,
    val description: String = "",
    val isSystemDefault: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
