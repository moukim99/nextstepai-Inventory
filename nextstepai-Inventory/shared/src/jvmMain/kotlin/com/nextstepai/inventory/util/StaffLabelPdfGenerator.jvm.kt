package com.nextstepai.inventory.util

actual object StaffLabelPdfGenerator {
    actual val isPdfExportSupported: Boolean = false

    actual fun generatePdf(
        codeStr: String,
        labelTitle: String,
        assignedRole: String,
        issueDate: String
    ): ByteArray {
        throw UnsupportedOperationException("JVM PDF generation for staff labels is not yet implemented.")
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
