package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_test_templates")
data class PartTestTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val testName: String,
    val description: String = "",
    val required: Boolean = true,
    val requiresValue: Boolean = false,
    val requiresAttachment: Boolean = false
)
