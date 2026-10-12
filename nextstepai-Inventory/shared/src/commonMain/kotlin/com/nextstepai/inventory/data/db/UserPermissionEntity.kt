package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

/**
 * كيان الصلاحيات التفصيلية للمستخدمين في النظام (UserPermissionEntity).
 */
@Entity(tableName = "user_permissions")
data class UserPermissionEntity(
    @PrimaryKey
    val uuid: String,
    val ownerId: String,
    val userUuid: String,
    val module: String, // WAREHOUSE, PRODUCTION, SALES_ORDERS, SETTINGS
    val canCreate: Boolean = false,
    val canRead: Boolean = true,
    val canUpdate: Boolean = false,
    val canDelete: Boolean = false,
    val version: Int = 1,
    val syncStatus: String = "PENDING_PUSH",
    val lastModifiedByDeviceUuid: String? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
