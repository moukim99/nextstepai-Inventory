package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_parameters")
data class PartParameterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val templateId: Long,
    val data: String,
    val dataNumeric: Double? = null
)
