package com.nextstepai.inventory.util

import com.nextstepai.inventory.data.Part

expect object PartLabelPdfGenerator {
    fun generatePdf(
        part: Part,
        categoryName: String = "",
        issueDate: String = ""
    ): ByteArray

    fun generatePngPreview(
        part: Part,
        categoryName: String = "",
        issueDate: String = ""
    ): ByteArray?
}
