package com.nextstepai.inventory.data.db

import androidx.room.Dao

/**
 * كائن الوصول لبيانات المستخدمين (AppUserDao) في قاعدة بيانات SQLite واستعلام الحسابات النشطة.
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
            SELECT uuid, name, role, active, isDeleted, updatedAt
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
                    )
                )
            }
        }
        return results
    }

    /**
     * إدراج أو تحديث حساب مستخدم في جدول app_users.
     */
    fun insertOrUpdate(entity: AppUserEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare(
            """
            INSERT OR REPLACE INTO app_users (uuid, name, role, active, isDeleted, updatedAt)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, entity.uuid)
            stmt.bindText(2, entity.name)
            stmt.bindText(3, entity.role)
            stmt.bindLong(4, if (entity.active) 1L else 0L)
            stmt.bindLong(5, if (entity.isDeleted) 1L else 0L)
            stmt.bindLong(6, entity.updatedAt)
            stmt.step()
        }
    }
}
