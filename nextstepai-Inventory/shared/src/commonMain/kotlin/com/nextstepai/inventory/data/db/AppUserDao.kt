package com.nextstepai.inventory.data.db

import androidx.room.Dao
import kotlin.time.Clock

/**
 * كائن الوصول لبيانات المستخدمين وأكواد التفعيل (AppUserDao) في قاعدة بيانات SQLite واستعلام الحسابات النشطة.
 */
@Dao
class AppUserDao {

    /**
     * جلب كافة الحسابات النشطة وغير المحذوفة من جدول app_users.
     */
    fun getActiveUsers(): List<AppUserEntity> {
        val conn = SqliteDatabaseManager.getConnection()
        val results = mutableListOf<AppUserEntity>()
        val sql = """
            SELECT uuid, name, role, active, isDeleted, updatedAt, ownerId, phoneNumber, passwordHash, avatarUrl, version, syncStatus, createdAt, lastModifiedByDeviceUuid
            FROM app_users
            WHERE active = 1 AND isDeleted = 0
            ORDER BY name ASC
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            while (stmt.step()) {
                results.add(
                    AppUserEntity(
                        uuid = stmt.getText(0),
                        name = stmt.getText(1),
                        role = stmt.getText(2),
                        active = stmt.getLong(3) != 0L,
                        isDeleted = stmt.getLong(4) != 0L,
                        updatedAt = stmt.getLong(5),
                        ownerId = stmt.getText(6),
                        phoneNumber = stmt.getText(7),
                        passwordHash = stmt.getText(8),
                        avatarUrl = stmt.getText(9),
                        version = stmt.getLong(10).toInt(),
                        syncStatus = stmt.getText(11),
                        createdAt = stmt.getLong(12),
                        lastModifiedByDeviceUuid = if (stmt.isNull(13)) null else stmt.getText(13)
                    )
                )
            }
        }
        return results
    }

    /**
     * البحث عن مستخدم بواسطة رقم الهاتف.
     */
    fun getUserByPhone(phoneNumber: String): AppUserEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT uuid, name, role, active, isDeleted, updatedAt, ownerId, phoneNumber, passwordHash, avatarUrl, version, syncStatus, createdAt, lastModifiedByDeviceUuid
            FROM app_users
            WHERE phoneNumber = ? AND isDeleted = 0
            LIMIT 1
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, phoneNumber.trim())
            if (stmt.step()) {
                return AppUserEntity(
                    uuid = stmt.getText(0),
                    name = stmt.getText(1),
                    role = stmt.getText(2),
                    active = stmt.getLong(3) != 0L,
                    isDeleted = stmt.getLong(4) != 0L,
                    updatedAt = stmt.getLong(5),
                    ownerId = stmt.getText(6),
                    phoneNumber = stmt.getText(7),
                    passwordHash = stmt.getText(8),
                    avatarUrl = stmt.getText(9),
                    version = stmt.getLong(10).toInt(),
                    syncStatus = stmt.getText(11),
                    createdAt = stmt.getLong(12),
                    lastModifiedByDeviceUuid = if (stmt.isNull(13)) null else stmt.getText(13)
                )
            }
        }
        return null
    }

    /**
     * إدراج أو تحديث حساب مستخدم في جدول app_users.
     */
    fun insertOrUpdate(entity: AppUserEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare(
            """
            INSERT OR REPLACE INTO app_users (uuid, name, role, active, isDeleted, updatedAt, ownerId, phoneNumber, passwordHash, avatarUrl, version, syncStatus, createdAt, lastModifiedByDeviceUuid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.name)
            stmt.bindText(3, entity.role)
            stmt.bindLong(4, if (entity.active) 1L else 0L)
            stmt.bindLong(5, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(6, entity.updatedAt)
            stmt.bindText(7, entity.ownerId)
            stmt.bindText(8, entity.phoneNumber)
            stmt.bindText(9, entity.passwordHash)
            stmt.bindText(10, entity.avatarUrl)
            stmt.bindLong(11, entity.version.toLong())
            stmt.bindText(12, entity.syncStatus)
            stmt.bindLong(13, entity.createdAt)
            if (entity.lastModifiedByDeviceUuid != null) {
                stmt.bindText(14, entity.lastModifiedByDeviceUuid)
            } else {
                stmt.bindNull(14)
            }
            stmt.step()
        }
    }

    /**
     * جلب معلومات كود التفعيل من جدول activation_codes.
     */
    fun getActivationCode(code: String): ActivationCodeEntity? {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            SELECT code, ownerId, assignedRole, permissionsJson, label, isUsed, usedByUuid, expiresAt, version, syncStatus, lastModifiedByDeviceUuid, createdAt, updatedAt, isDeleted
            FROM activation_codes
            WHERE code = ?
            LIMIT 1
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, code.trim().uppercase())
            if (stmt.step()) {
                return ActivationCodeEntity(
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
            }
        }
        return null
    }

    /**
     * تعليم كود التفعيل كمستخدم بربطه مع معرف المستخدم الجديد.
     */
    fun markActivationCodeUsed(code: String, userUuid: String): Boolean {
        val conn = SqliteDatabaseManager.getConnection()
        val now = Clock.System.now().toEpochMilliseconds()
        val sql = """
            UPDATE activation_codes
            SET isUsed = 1, usedByUuid = ?, updatedAt = ?, syncStatus = 'PENDING_PUSH'
            WHERE code = ? AND isUsed = 0 AND isDeleted = 0 AND (expiresAt = 0 OR expiresAt > ?)
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, userUuid)
            stmt.bindLong(2, now)
            stmt.bindText(3, code.trim().uppercase())
            stmt.bindLong(4, now)
            stmt.step()
        }

        var changed = 0
        conn.prepare("SELECT changes()").use { stmt ->
            if (stmt.step()) {
                changed = stmt.getLong(0).toInt()
            }
        }
        return changed > 0
    }
}
