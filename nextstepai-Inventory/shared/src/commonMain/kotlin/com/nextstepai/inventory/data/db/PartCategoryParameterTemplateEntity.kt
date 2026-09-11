package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_category_parameter_templates")
data class PartCategoryParameterTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val categoryId: Long,
    val parameterTemplateId: Long,
    val defaultValue: String? = null
)
