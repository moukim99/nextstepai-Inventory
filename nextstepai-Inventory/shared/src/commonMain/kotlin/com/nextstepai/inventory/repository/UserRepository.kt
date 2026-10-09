package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.db.AppUserDao

/**
 * المستودع المسؤول عن إدارة وإمداد بيانات المستخدمين وجلب الحسابات النشطة من جدول app_users.
 */
class UserRepository(
    private val userDao: AppUserDao = AppUserDao()
) {
    private val defaultUsers = listOf(
        AppUser("usr-001", "مدير الإنتاج والتصنيع", "مدير الإنتاج والتصنيع"),
        AppUser("usr-002", "مشرف خط التجميع", "مشرف خط التجميع"),
        AppUser("usr-003", "مهندس الجودة والسلامة", "مهندس الجودة والسلامة"),
        AppUser("usr-004", "مدير المستودع والخدمات اللوجستية", "مدير المستودع"),
        AppUser("usr-005", "مدير النظام (Admin)", "مدير النظام"),
        AppUser("usr-006", "فريق التشغيل والتجميع", "فريق التشغيل")
    )

    /**
     * سحب قائمة الحسابات النشطة (WHERE active = 1 AND isDeleted = 0).
     */
    fun getActiveUsers(): List<AppUser> {
        return runCatching {
            val entities = userDao.getActiveUsers()
            if (entities.isNotEmpty()) {
                entities.map {
                    AppUser(
                        uuid = it.uuid,
                        name = it.name,
                        role = it.role,
                        active = it.active,
                        isDeleted = it.isDeleted,
                        updatedAt = it.updatedAt
                    )
                }
            } else {
                defaultUsers
            }
        }.getOrDefault(defaultUsers)
    }
}
