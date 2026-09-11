package com.nextstepai.inventory.data

import kotlin.time.Clock

/**
 * تمثيل سجل الدخول في جدول تسجيل الدخول (Login Table).
 *
 * @property id المعرف الفريد للسجل
 * @property username اسم المستخدم المسجل
 * @property isLoggedIn حالة تسجيل الدخول الحالية
 * @property loginTime وقت تسجيل الدخول
 * @property authStatus حالة التحقق (مثال: "VERIFIED_GUEST")
 */
data class LoginRecord(
    val id: Long,
    val username: String,
    val isLoggedIn: Boolean,
    val loginTime: Long,
    val authStatus: String
)

/**
 * محاكاة وإدارة جدول الدخول (Login Table) لحفظ وجلب وإدارة حالات وسجلات الدخول.
 */
class LoginTable {
    private val records = mutableListOf<LoginRecord>()
    private var activeRecord: LoginRecord? = null

    /**
     * إدراج سجل دخول جديد في جدول الدخول وإتاحة الجلسة.
     */
    fun insertLoginRecord(username: String = "مستخدم جديد", authStatus: String = "VERIFIED_GUEST"): LoginRecord {
        val newRecord = LoginRecord(
            id = (records.size + 1).toLong(),
            username = username,
            isLoggedIn = true,
            loginTime = Clock.System.now().toEpochMilliseconds(),
            authStatus = authStatus
        )
        records.add(newRecord)
        activeRecord = newRecord
        return newRecord
    }

    /**
     * جلب سجل الدخول النشط حالياً.
     */
    fun getActiveSession(): LoginRecord? {
        return activeRecord
    }

    /**
     * التحقق من وجود جلسة دخول نشطة في الجدول.
     */
    fun verifyActiveSession(): Boolean {
        return activeRecord?.isLoggedIn == true
    }

    /**
     * إنهاء الجلسة وتحديث السجل.
     */
    fun clearSession() {
        activeRecord = activeRecord?.copy(isLoggedIn = false)
    }

    /**
     * الحصول على جميع سجلات الدخول المحفوظة.
     */
    fun getAllRecords(): List<LoginRecord> = records.toList()
}
