package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

/**
 * تمثيل كيان كود التفعيل (ActivationCodeEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "activation_codes")
data class ActivationCodeEntity(
    @PrimaryKey
    val code: String,
    val ownerId: String = "OWNER-DEFAULT",
    val assignedRole: String = "STAFF",
    val permissionsJson: String = "{}",
    val label: String = "",
    val isUsed: Boolean = false,
    val usedByUuid: String = "",
    val expiresAt: Long = 0L,
    val version: Int = 1,
    val syncStatus: String = "PENDING_PUSH",
    val lastModifiedByDeviceUuid: String? = null,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isDeleted: Boolean = false
)
