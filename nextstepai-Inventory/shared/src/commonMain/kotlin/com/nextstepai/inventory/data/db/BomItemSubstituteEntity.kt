package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bom_item_substitutes")
data class BomItemSubstituteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val bomItemId: Long,
    val partId: Long
)
