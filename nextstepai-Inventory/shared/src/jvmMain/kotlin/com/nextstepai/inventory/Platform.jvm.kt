package com.nextstepai.inventory

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun shareTextPayload(text: String, title: String) {
    runCatching {
        val selection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }
}

actual fun sharePdfPayload(
    pdfBytes: ByteArray,
    fileName: String,
    title: String,
    previewImageBytes: ByteArray?
) {
    runCatching {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "labels")
        if (!tempDir.exists()) tempDir.mkdirs()
        val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val pdfFile = File(tempDir, cleanName)
        pdfFile.writeBytes(pdfBytes)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(pdfFile)
        }
    }
}
