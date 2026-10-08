package com.nextstepai.inventory.auth

/**
 * استثناء فشل المصادقة عند غياب أو انتهاء صلاحية الرموز المميزة.
 */
class AuthenticationException(message: String) : IllegalStateException(message)

/**
 * نموذج رموز المصادقة (JWT Tokens).
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)

/**
 * التخزين الآمن المشفر لرموز المصادقة معزول عن باقي بيانات قاعدة البيانات.
 */
expect class SecureTokenStorage() {
    suspend fun saveTokens(tokens: AuthTokens)
    suspend fun getTokens(): AuthTokens?
    suspend fun clearTokens()
}
