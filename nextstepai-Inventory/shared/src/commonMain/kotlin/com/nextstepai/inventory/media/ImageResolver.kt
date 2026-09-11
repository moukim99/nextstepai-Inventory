package com.nextstepai.inventory.media

import com.nextstepai.inventory.data.db.getDatabasePath
import java.io.File

/**
 * الكائن المسؤول عن تحويل المسارات النسبية المحفوظة في قاعدة البيانات
 * إلى مسارات مطلقة حقيقية على جهاز المستخدم عبر دالة جذر التخزين getDatabasePath().
 */
object ImageResolver {
    fun resolveAbsolutePath(relativePath: String?): String? {
        if (relativePath.isNullOrBlank()) return null
        val dbFile = File(getDatabasePath())
        val rootDir = dbFile.parentFile ?: File(".")
        val resolved = File(rootDir, relativePath)
        return resolved.absolutePath
    }
}
