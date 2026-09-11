package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_notes")
data class PartNotesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val notes: String = "",
    val updatedAt: String = "",
    val userId: Long? = 1L
)
