package com.nextstepai.inventory.repository

import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.SecureTokenStorage
import com.nextstepai.inventory.data.LoginRecord
import com.nextstepai.inventory.data.LoginTable

/**
 * المستودع (Repository) المسؤول عن إدارة عمليات التحقق والتفاعل مع جدول الدخول (Login Table)
 * وحفظ الرموز المميزة الآمنة (SecureTokenStorage).
 */
class LoginRepository(
    private val loginTable: LoginTable = LoginTable(),
    private val tokenStorage: SecureTokenStorage = SecureTokenStorage()
) {
    /**
     * التحقق البسيط وتسجيل دخول المستخدم بنقرة واحدة بدون بيانات مسجلة مسبقاً.
     * يقوم بإنشاء وإدراج سجل جديد في جدول الدخول وحفظ رموز المصادقة المشفرة.
     */
    suspend fun verifyAndLogin(username: String = "زائر المستودع"): LoginRecord {
        val record = loginTable.insertLoginRecord(
            username = username,
            authStatus = "AUTO_VERIFIED"
        )
        if (record.isLoggedIn) {
            tokenStorage.saveTokens(
                AuthTokens(
                    accessToken = "jwt-access-${record.id}-${record.loginTime}",
                    refreshToken = "jwt-refresh-${record.id}-${record.loginTime}"
                )
            )
        }
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
     * الحصول على رموز المصادقة المشفرة.
     */
    suspend fun getTokens(): AuthTokens? {
        return tokenStorage.getTokens()
    }

    /**
     * تسجيل الخروج من التطبيق ومسح الرموز المميزة الآمنة.
     */
    suspend fun logout() {
        loginTable.clearSession()
        tokenStorage.clearTokens()
    }
}
