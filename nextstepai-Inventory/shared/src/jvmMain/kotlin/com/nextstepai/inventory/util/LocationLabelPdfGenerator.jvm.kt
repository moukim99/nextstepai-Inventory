package com.nextstepai.inventory.util

import com.nextstepai.inventory.data.StockLocation

actual object LocationLabelPdfGenerator {
    actual fun generatePdf(
        location: StockLocation,
        parentPath: String,
        issueDate: String
    ): ByteArray {
        val content = """
            ==============================================
            ملصق موقع تخزيني - $issueDate
            ==============================================
            اسم الموقع: ${location.name}
            المسار الهرمي: ${parentPath.ifBlank { "موقع رئيسي (Root)" }}
            المعرف الفريد: ${location.effectiveUuid}
            نوع الموقع: ${location.locationType}
            السعة التخزينية: ${location.customCapacity ?: "غير محددة"}
            ==============================================
        """.trimIndent()
        return content.encodeToByteArray()
    }

    actual fun generatePngPreview(
        location: StockLocation,
        parentPath: String,
        issueDate: String
    ): ByteArray? {
        return null
    }
}
