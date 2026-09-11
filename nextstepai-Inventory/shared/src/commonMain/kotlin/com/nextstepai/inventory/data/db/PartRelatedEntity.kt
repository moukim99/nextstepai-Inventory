package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_related")
data class PartRelatedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val part1Id: Long,
    val part2Id: Long
)
