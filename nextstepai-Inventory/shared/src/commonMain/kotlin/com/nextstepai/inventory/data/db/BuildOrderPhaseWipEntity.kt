package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "build_order_phase_wip")
data class BuildOrderPhaseWipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val buildOrderId: Long,
    val phaseId: Long,
    val quantity: Double,
    val status: String = "ACTIVE"
)
