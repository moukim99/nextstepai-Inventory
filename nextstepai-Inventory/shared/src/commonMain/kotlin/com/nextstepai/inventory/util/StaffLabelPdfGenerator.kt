package com.nextstepai.inventory.util

expect object StaffLabelPdfGenerator {
    val isPdfExportSupported: Boolean

    fun generatePdf(
        codeStr: String,
        labelTitle: String = "",
        assignedRole: String = "",
        issueDate: String = ""
    ): ByteArray

    fun generatePngPreview(
        codeStr: String,
        labelTitle: String = "",
        assignedRole: String = "",
        issueDate: String = ""
    ): ByteArray?
}
