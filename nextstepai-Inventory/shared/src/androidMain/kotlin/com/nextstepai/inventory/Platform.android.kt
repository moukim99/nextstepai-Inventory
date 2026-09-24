package com.nextstepai.inventory

import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.nextstepai.inventory.data.db.AppContextHolder
import java.io.File

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun shareTextPayload(text: String, title: String) {
    val ctx = AppContextHolder.appContext ?: return
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val shareIntent = Intent.createChooser(sendIntent, title).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    ctx.startActivity(shareIntent)
}

actual fun sharePdfPayload(
    pdfBytes: ByteArray,
    fileName: String,
    title: String,
    previewImageBytes: ByteArray?
) {
    val ctx = AppContextHolder.appContext ?: return
    runCatching {
        val cacheDir = File(ctx.cacheDir, "labels")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        // تنظيف اسم الملف وحفظه بصيغة PDF عربية واضحة
        val cleanPdfName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val pdfFile = File(cacheDir, cleanPdfName)
        pdfFile.writeBytes(pdfBytes)

        val authority = "${ctx.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(ctx, authority, pdfFile)

        // إطلاق Intent مشاركة PDF المباشر للطباعة ومنع أيقونة القلم لتعديل الصور
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, "ملصق موقع تخزيني جاهز للطباعة والمشاركة $title")
            clipData = ClipData.newRawUri(title, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(chooserIntent)
    }.onFailure { it.printStackTrace() }
}
