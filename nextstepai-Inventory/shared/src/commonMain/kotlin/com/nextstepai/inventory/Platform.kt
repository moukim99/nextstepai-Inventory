package com.nextstepai.inventory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun shareTextPayload(text: String, title: String = "مشاركة بطاقة الملصق")

expect fun sharePdfPayload(
    pdfBytes: ByteArray,
    fileName: String,
    title: String = "مشاركة الملصق",
    previewImageBytes: ByteArray? = null
)
