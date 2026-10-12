package com.nextstepai.inventory.util

actual object StaffLabelPdfGenerator {
    actual fun generatePdf(
        codeStr: String,
        labelTitle: String,
        assignedRole: String,
        issueDate: String
    ): ByteArray {
        val content = """
            ==============================================
            ملصق موظف / كود تفعيل - $issueDate
            ==============================================
            المسمى الوظيفي: ${labelTitle.ifBlank { "موظف" }}
            كود التفعيل / الموظف: $codeStr
            الدور والمهام: ${assignedRole.ifBlank { "OPERATOR" }}
            ==============================================
        """.trimIndent()
        return content.encodeToByteArray()
    }

    actual fun generatePngPreview(
        codeStr: String,
        labelTitle: String,
        assignedRole: String,
        issueDate: String
    ): ByteArray? {
        return null
    }
}
