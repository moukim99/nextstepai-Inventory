package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "part_attachments")
data class PartAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val attachment: String? = null,
    val link: String? = null,
    val comment: String = "",
    val uploadDate: String = "",
    val userId: Long? = 1L
)
