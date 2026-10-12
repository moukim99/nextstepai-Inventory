package com.nextstepai.inventory.data.db

import androidx.room.Dao
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات إدارة الموظفين وأكواد التفعيل والصلاحيات المخصصة (StaffManagementDao).
 */
@Dao
class StaffManagementDao {

    /**
     * جلب الموظفين المرتبطين بالمالك.
     */
    fun getUsersByOwner(ownerId: String): List<AppUserEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<AppUserEntity>()
        val sql = if (ownerId.isBlank()) {
            """
            SELECT uuid, ownerId, name, phoneNumber, passwordHash, role, active, avatarUrl, version, syncStatus, isDeleted, updatedAt, createdAt
            FROM app_users
            WHERE isDeleted = 0
            ORDER BY name ASC
            """.trimIndent()
        } else {
            """
            SELECT uuid, ownerId, name, phoneNumber, passwordHash, role, active, avatarUrl, version, syncStatus, isDeleted, updatedAt, createdAt
            FROM app_users
            WHERE (ownerId = ? OR ownerId = '') AND isDeleted = 0
            ORDER BY name ASC
            """.trimIndent()
        }

        conn.prepare(sql).use { stmt ->
            if (ownerId.isNotBlank()) {
                stmt.bindText(1, ownerId)
            }
            while (stmt.step()) {
                results.add(
                    AppUserEntity(
                        uuid = stmt.getText(0),
                        ownerId = stmt.getText(1),
                        name = stmt.getText(2),
                        phoneNumber = stmt.getText(3),
                        passwordHash = stmt.getText(4),
                        role = stmt.getText(5),
                        active = stmt.getLong(6) != 0L,
                        avatarUrl = stmt.getText(7),
                        version = stmt.getLong(8).toInt(),
                        syncStatus = stmt.getText(9),
                        isDeleted = stmt.getLong(10) != 0L,
                        updatedAt = stmt.getLong(11),
                        createdAt = stmt.getLong(12)
                    )
                )
            }
        }
        return results
    }

    /**
     * جلب صلاحيات مستخدم محدد للوحدات الأربع.
     */
    fun getUserPermissions(userUuid: String): List<UserPermissionEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<UserPermissionEntity>()
        val sql = """
            SELECT uuid, ownerId, userUuid, module, canCreate, canRead, canUpdate, canDelete, version, syncStatus, lastModifiedByDeviceUuid, createdAt, updatedAt, isDeleted, deletedAt
            FROM user_permissions
            WHERE userUuid = ? AND isDeleted = 0
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, userUuid)
            while (stmt.step()) {
                results.add(
                    UserPermissionEntity(
                        uuid = stmt.getText(0),
                        ownerId = stmt.getText(1),
                        userUuid = stmt.getText(2),
                        module = stmt.getText(3),
                        canCreate = stmt.getLong(4) != 0L,
                        canRead = stmt.getLong(5) != 0L,
                        canUpdate = stmt.getLong(6) != 0L,
                        canDelete = stmt.getLong(7) != 0L,
                        version = stmt.getLong(8).toInt(),
                        syncStatus = stmt.getText(9),
                        lastModifiedByDeviceUuid = if (stmt.isNull(10)) null else stmt.getText(10),
                        createdAt = stmt.getLong(11),
                        updatedAt = stmt.getLong(12),
                        isDeleted = stmt.getLong(13) != 0L,
                        deletedAt = if (stmt.isNull(14)) null else stmt.getLong(14)
                    )
                )
            }
        }
        return results
    }

    /**
     * حفظ/تحديث مجموعة صلاحيات مستخدم.
     */
    fun saveUserPermissions(permissions: List<UserPermissionEntity>) {
        if (permissions.isEmpty()) return
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            INSERT OR REPLACE INTO user_permissions (uuid, ownerId, userUuid, module, canCreate, canRead, canUpdate, canDelete, version, syncStatus, lastModifiedByDeviceUuid, createdAt, updatedAt, isDeleted, deletedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        for (perm in permissions) {
            conn.prepare(sql).use { stmt ->
                stmt.bindText(1, perm.uuid)
                stmt.bindText(2, perm.ownerId)
                stmt.bindText(3, perm.userUuid)
                stmt.bindText(4, perm.module)
                stmt.bindLong(5, if (perm.canCreate) 1L else 0L)
                stmt.bindLong(6, if (perm.canRead) 1L else 0L)
                stmt.bindLong(7, if (perm.canUpdate) 1L else 0L)
                stmt.bindLong(8, if (perm.canDelete) 1L else 0L)
                stmt.bindLong(9, perm.version.toLong())
                stmt.bindText(10, perm.syncStatus)
                if (perm.lastModifiedByDeviceUuid != null) stmt.bindText(11, perm.lastModifiedByDeviceUuid) else stmt.bindNull(11)
                stmt.bindLong(12, perm.createdAt)
                stmt.bindLong(13, perm.updatedAt)
                stmt.bindLong(14, if (perm.isDeleted) 1L else 0L)
                if (perm.deletedAt != null) stmt.bindLong(15, perm.deletedAt) else stmt.bindNull(15)
                stmt.step()
            }
        }
    }

    /**
     * إدراج كود دعوة/تفعيل جديد للموظف.
     * يرجع true إذا تم الإدراج بنجاح، أو false إذا كان الكود مكرراً أو تعذر الإدراج.
     */
    fun insertActivationCode(code: ActivationCodeEntity): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            INSERT INTO activation_codes (code, ownerId, assignedRole, permissionsJson, label, isUsed, usedByUuid, expiresAt, version, syncStatus, lastModifiedByDeviceUuid, createdAt, updatedAt, isDeleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val inserted = try {
            conn.prepare(sql).use { stmt ->
                stmt.bindText(1, code.code)
                stmt.bindText(2, code.ownerId)
                stmt.bindText(3, code.assignedRole)
                stmt.bindText(4, code.permissionsJson)
                stmt.bindText(5, code.label)
                stmt.bindLong(6, if (code.isUsed) 1L else 0L)
                stmt.bindText(7, code.usedByUuid)
                stmt.bindLong(8, code.expiresAt)
                stmt.bindLong(9, code.version.toLong())
                stmt.bindText(10, code.syncStatus)
                if (code.lastModifiedByDeviceUuid != null) stmt.bindText(11, code.lastModifiedByDeviceUuid) else stmt.bindNull(11)
                stmt.bindLong(12, code.createdAt)
                stmt.bindLong(13, code.updatedAt)
                stmt.bindLong(14, if (code.isDeleted) 1L else 0L)
                stmt.step()
            }
            true
        } catch (_: Throwable) {
            false
        }

        if (!inserted) return false

        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed > 0
    }

    /**
     * جلب كافة أكواد التفعيل غير المستخدمة (Pending Invites).
     */
    fun getActiveInvitationCodes(ownerId: String): List<ActivationCodeEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<ActivationCodeEntity>()
        val sql = """
            SELECT code, ownerId, assignedRole, permissionsJson, label, isUsed, usedByUuid, expiresAt, version, syncStatus, lastModifiedByDeviceUuid, createdAt, updatedAt, isDeleted
            FROM activation_codes
            WHERE ownerId = ? AND isUsed = 0 AND isDeleted = 0
            ORDER BY createdAt DESC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, ownerId)
            while (stmt.step()) {
                results.add(
                    ActivationCodeEntity(
                        code = stmt.getText(0),
                        ownerId = stmt.getText(1),
                        assignedRole = stmt.getText(2),
                        permissionsJson = stmt.getText(3),
                        label = stmt.getText(4),
                        isUsed = stmt.getLong(5) != 0L,
                        usedByUuid = stmt.getText(6),
                        expiresAt = stmt.getLong(7),
                        version = stmt.getLong(8).toInt(),
                        syncStatus = stmt.getText(9),
                        lastModifiedByDeviceUuid = if (stmt.isNull(10)) null else stmt.getText(10),
                        createdAt = stmt.getLong(11),
                        updatedAt = stmt.getLong(12),
                        isDeleted = stmt.getLong(13) != 0L
                    )
                )
            }
        }
        return results
    }

    /**
     * حذف أو إلغاء كود تفعيل معلق.
     */
    fun deleteActivationCode(code: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE activation_codes SET isDeleted = 1, syncStatus = 'PENDING_PUSH', updatedAt = ? WHERE code = ?"
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, code)
            stmt.step()
        }
    }

    /**
     * تفعيل أو تعطيل حساب موظف.
     */
    fun setUserActive(userUuid: String, isActive: Boolean, now: Long = Clock.System.now().toEpochMilliseconds()) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE app_users SET active = ?, updatedAt = ?, syncStatus = 'PENDING_PUSH' WHERE uuid = ?"
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, if (isActive) 1L else 0L)
            stmt.bindLong(2, now)
            stmt.bindText(3, userUuid)
            stmt.step()
        }
    }

    /**
     * حذف موظف/مستخدم من النظام (Soft Delete).
     */
    fun deleteUser(userUuid: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE app_users SET isDeleted = 1, syncStatus = 'PENDING_PUSH', updatedAt = ? WHERE uuid = ?"
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare(sql).use { stmt ->
            stmt.bindLong(1, now)
            stmt.bindText(2, userUuid)
            stmt.step()
        }
    }

    /**
     * تحديث وصف/مسمى كود التفعيل.
     */
    fun updateActivationCodeLabel(code: String, newLabel: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE activation_codes SET label = ?, syncStatus = 'PENDING_PUSH', updatedAt = ? WHERE code = ?"
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, newLabel)
            stmt.bindLong(2, now)
            stmt.bindText(3, code)
            stmt.step()
        }
    }

    /**
     * تحديث كود التفعيل بالكامل (المسمى الوظيفي والدور والصلاحيات).
     */
    fun updateActivationCodeFull(code: String, label: String, role: String, permissionsJson: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE activation_codes SET label = ?, assignedRole = ?, permissionsJson = ?, syncStatus = 'PENDING_PUSH', updatedAt = ? WHERE code = ?"
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, label)
            stmt.bindText(2, role)
            stmt.bindText(3, permissionsJson)
            stmt.bindLong(4, now)
            stmt.bindText(5, code)
            stmt.step()
        }
    }

    /**
     * تحديث بيانات الموظف (المسمى الوظيفي والاسم).
     */
    fun updateAppUser(uuid: String, name: String, role: String) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = "UPDATE app_users SET name = ?, role = ?, syncStatus = 'PENDING_PUSH', updatedAt = ? WHERE uuid = ?"
        val now = Clock.System.now().toEpochMilliseconds()
        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, name)
            stmt.bindText(2, role)
            stmt.bindLong(3, now)
            stmt.bindText(4, uuid)
            stmt.step()
        }
    }
}
