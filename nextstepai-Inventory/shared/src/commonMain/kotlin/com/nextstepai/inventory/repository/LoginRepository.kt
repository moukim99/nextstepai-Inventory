package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.LoginRecord
import com.nextstepai.inventory.data.LoginTable

/**
 * المستودع (Repository) المسؤول عن إدارة عمليات التحقق والتفاعل مع جدول الدخول (Login Table).
 */
class LoginRepository(
    private val loginTable: LoginTable = LoginTable()
) {
    /**
     * التحقق البسيط وتسجيل دخول المستخدم بنقرة واحدة بدون بيانات مسجلة مسبقاً.
     * يقوم بإنشاء وإدراج سجل جديد في جدول الدخول والتحقق من صحة العملية.
     */
    fun verifyAndLogin(): LoginRecord {
        val record = loginTable.insertLoginRecord(
            username = "زائر المستودع",
            authStatus = "AUTO_VERIFIED"
        )
        return record
    }

    /**
     * التحقق من وجود جلسة دخول نشطة.
     */
    fun isLoggedIn(): Boolean {
        return loginTable.verifyActiveSession()
    }

    /**
     * الحصول على سجل الدخول الحالي.
     */
    fun getCurrentSession(): LoginRecord? {
        return loginTable.getActiveSession()
    }

    /**
     * تسجيل الخروج من التطبيق.
     */
    fun logout() {
        loginTable.clearSession()
    }
}
