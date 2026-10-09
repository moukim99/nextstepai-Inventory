package com.nextstepai.inventory.auth

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.SecureRandom

/**
 * محرك التشفير وتجزئة كلمات المرور باستخدام خوارزمية PBKDF2WithHmacSHA256 (Offline-First Crypto Engine).
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTE_SIZE = 16
    private const val FORMAT_PREFIX = "pbkdf2_sha256"

    private val secureRandom = SecureRandom()

    /**
     * تجزئة كلمة المرور الصريحة وإرجاع النص المشفر المنسق.
     * التنسيق النهائي: "pbkdf2_sha256$120000$<saltBase64>$<hashBase64>"
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTE_SIZE).also { secureRandom.nextBytes(it) }
        val hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)

        val saltBase64 = encodeBase64(salt)
        val hashBase64 = encodeBase64(hash)

        return "$FORMAT_PREFIX$$ITERATIONS$$saltBase64$$hashBase64"
    }

    /**
     * المطابقة الآمنة لكلمة المرور المدخلة مع النص المشفر المحفوظ، مع الحماية من هجمات التوقيت (Constant-time comparison).
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || !storedHash.startsWith("$FORMAT_PREFIX$")) {
            return false
        }

        val parts = storedHash.split("$")
        if (parts.size != 4) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = decodeBase64(parts[2])
        val expectedHash = decodeBase64(parts[3])

        val computedHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.size * 8)

        return constantTimeEquals(computedHash, expectedHash)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLengthBits)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /**
     * مقارنة زمنية ثابتة لمنع هجمات التوقيت (Timing Attack Protection).
     */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    // --- Base64 Helper (Pure Kotlin for 100% Cross-Platform Safety) ---
    private const val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun encodeBase64(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

            val c0 = b0 shr 2
            var c1 = (b0 and 0x03) shl 4
            if (b1 != -1) c1 = c1 or (b1 shr 4)

            var c2 = 64
            if (b1 != -1) c2 = (b1 and 0x0F) shl 2
            if (b2 != -1) c2 = c2 or (b2 shr 6)

            var c3 = 64
            if (b2 != -1) c3 = b2 and 0x3F

            sb.append(BASE64_CHARS[c0])
            sb.append(BASE64_CHARS[c1])
            sb.append(if (c2 < 64) BASE64_CHARS[c2] else '=')
            sb.append(if (c3 < 64) BASE64_CHARS[c3] else '=')
        }
        return sb.toString()
    }

    private fun decodeBase64(input: String): ByteArray {
        val cleanInput = input.trimEnd('=')
        val byteLen = (cleanInput.length * 6) / 8
        val out = ByteArray(byteLen)
        var outIdx = 0

        var i = 0
        while (i < input.length) {
            val c0 = BASE64_CHARS.indexOf(input[i++]).takeIf { it >= 0 } ?: 0
            val c1 = if (i < input.length) BASE64_CHARS.indexOf(input[i++]).takeIf { it >= 0 } ?: 0 else 0
            val c2 = if (i < input.length && input[i] != '=') BASE64_CHARS.indexOf(input[i++]).takeIf { it >= 0 } ?: 0 else run { if (i < input.length && input[i] == '=') i++; 64 }
            val c3 = if (i < input.length && input[i] != '=') BASE64_CHARS.indexOf(input[i++]).takeIf { it >= 0 } ?: 0 else run { if (i < input.length && input[i] == '=') i++; 64 }

            val b0 = (c0 shl 2) or (c1 shr 4)
            if (outIdx < out.size) out[outIdx++] = b0.toByte()

            if (c2 < 64 && outIdx < out.size) {
                val b1 = ((c1 and 0x0F) shl 4) or (c2 shr 2)
                out[outIdx++] = b1.toByte()
            }

            if (c3 < 64 && outIdx < out.size) {
                val b2 = ((c2 and 0x03) shl 6) or c3
                out[outIdx++] = b2.toByte()
            }
        }
        return out
    }
}
