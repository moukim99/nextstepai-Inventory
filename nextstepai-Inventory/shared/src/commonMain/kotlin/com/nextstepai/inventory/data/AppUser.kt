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
 */
data class AppUser(
    val uuid: String,
    val name: String,
    val role: String = "",
    val active: Boolean = true,
    val isDeleted: Boolean = false,
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
