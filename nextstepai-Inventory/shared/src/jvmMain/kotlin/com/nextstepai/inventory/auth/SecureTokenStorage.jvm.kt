package com.nextstepai.inventory.auth

import java.util.prefs.Preferences

/**
 * التخزين الآمن لرموز المصادقة على أجهزة المكتبي (Desktop)
 * عبر الخزنة البرمجية لنظام التشغيل (OS-Native Credential Store).
 */
actual class SecureTokenStorage actual constructor() {

    private val prefs = Preferences.userNodeForPackage(SecureTokenStorage::class.java)

    actual suspend fun saveTokens(tokens: AuthTokens) {
        prefs.put("secure_access_token", tokens.accessToken)
        prefs.put("secure_refresh_token", tokens.refreshToken)
        prefs.flush()
    }

    actual suspend fun getTokens(): AuthTokens? {
        val access = prefs.get("secure_access_token", null) ?: return null
        val refresh = prefs.get("secure_refresh_token", null) ?: return null
        return AuthTokens(accessToken = access, refreshToken = refresh)
    }

    actual suspend fun clearTokens() {
        prefs.remove("secure_access_token")
        prefs.remove("secure_refresh_token")
        prefs.flush()
    }
}
