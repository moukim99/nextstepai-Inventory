package com.nextstepai.inventory.auth

/**
 * محرك التشفير وتجزئة كلمات المرور باستخدام خوارزمية PBKDF2 (Offline-First Crypto Engine).
 */
expect object PasswordHasher {

    /**
     * تجزئة كلمة المرور الصريحة وإرجاع النص المشفر المنسق.
     * التنسيق النهائي: "pbkdf2_sha256$120000$<saltBase64>$<hashBase64>"
     */
    fun hashPassword(password: String): String

    /**
     * المطابقة الآمنة لكلمة المرور المدخلة مع النص المشفر المحفوظ، مع الحماية من هجمات التوقيت (Constant-time comparison).
     */
    fun verifyPassword(password: String, storedHash: String): Boolean
}
