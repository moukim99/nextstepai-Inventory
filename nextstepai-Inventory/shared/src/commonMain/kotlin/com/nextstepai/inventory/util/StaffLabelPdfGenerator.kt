package com.nextstepai.inventory.util

expect object StaffLabelPdfGenerator {
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
