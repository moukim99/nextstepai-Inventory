package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

/**
 * تمثيل كيان المستخدم (AppUserEntity) في جدول app_users بقاعدة البيانات المحلية.
 */
@Entity(tableName = "app_users")
data class AppUserEntity(
    @PrimaryKey
    val uuid: String,
    val name: String,
    val role: String = "",
    val active: Boolean = true,
    val isDeleted: Boolean = false,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
