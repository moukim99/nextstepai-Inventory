package com.nextstepai.inventory.util

import com.nextstepai.inventory.data.Part

actual object PartLabelPdfGenerator {
    actual fun generatePdf(
        part: Part,
        categoryName: String,
        issueDate: String
    ): ByteArray {
        val ipnCode = part.ipn.ifBlank { "IPN-${part.id}" }
        val content = """
            ==============================================
            ملصق قطعة - $issueDate
            ==============================================
            اسم القطعة: ${part.name}
            كود IPN: $ipnCode
            التصنيف: ${categoryName.ifBlank { "عام" }}
            المعرف الفريد: ${part.effectiveUuid}
            ==============================================
        """.trimIndent()
        return content.encodeToByteArray()
    }

    actual fun generatePngPreview(
        part: Part,
        categoryName: String,
        issueDate: String
    ): ByteArray? {
        return null
    }
}
