package com.nextstepai.inventory.util

import com.nextstepai.inventory.data.StockLocation

expect object LocationLabelPdfGenerator {
    fun generatePdf(
        location: StockLocation,
        parentPath: String = "",
        issueDate: String = ""
    ): ByteArray

    fun generatePngPreview(
        location: StockLocation,
        parentPath: String = "",
        issueDate: String = ""
    ): ByteArray?
}
