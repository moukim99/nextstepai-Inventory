package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_parameter_templates")
data class PartParameterTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val units: String = "",
    val description: String = "",
    val choices: String = "",
    val checkbox: Boolean = false
)
