package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_categories")
data class PartCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val parentId: Long? = null,
    val description: String = "",
    val structural: Boolean = false,
    val defaultLocationId: Long? = null
)
