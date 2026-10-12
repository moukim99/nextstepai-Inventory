package com.nextstepai.inventory.data

import kotlin.time.Clock

/**
 * تمثيل حساب المستخدم في نظام إدارة المخزون والتصنيع.
 *
 * @property uuid المعرف الفريد للمستخدم
 * @property name اسم المستخدم
 * @property role الدور الوظيفي للمستخدم
 * @property active حالة نشاط الحساب (مفعل/معطل)
 * @property isDeleted معلمة الحذف المنطقي
 * @property updatedAt تاريخ وتوقيت التحديث
 * @property ownerId معرف مالك الحساب أو المؤسسة
 * @property phoneNumber رقم هاتف الموظف
 * @property avatarUrl رابط الصورة الرمزية للموظف
 */
data class AppUser(
    val uuid: String,
    val name: String,
    val role: String = "",
    val active: Boolean = true,
    val isDeleted: Boolean = false,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val ownerId: String = "OWNER-DEFAULT",
    val phoneNumber: String = "",
    val avatarUrl: String = ""
)
