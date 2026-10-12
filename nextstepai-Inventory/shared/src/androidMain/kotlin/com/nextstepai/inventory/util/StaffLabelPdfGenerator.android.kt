package com.nextstepai.inventory.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.nextstepai.inventory.ui.components.Code128Encoder
import java.io.ByteArrayOutputStream

actual object StaffLabelPdfGenerator {
    actual fun generatePdf(
        codeStr: String,
        labelTitle: String,
        assignedRole: String,
        issueDate: String
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(283, 170, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 283f, 170f, bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val borderRect = RectF(4f, 4f, 279f, 166f)
        canvas.drawRoundRect(borderRect, 8f, 8f, borderPaint)

        // Code Capsule
        val codeBgPaint = Paint().apply {
            color = Color.parseColor("#FEF3C7")
            style = Paint.Style.FILL
        }
        val codeBox = RectF(12f, 10f, 130f, 26f)
        canvas.drawRoundRect(codeBox, 4f, 4f, codeBgPaint)

        val codeTextCapsulePaint = Paint().apply {
            color = Color.parseColor("#92400E")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("كود التفعيل: $codeStr".take(20), 16f, 21f, codeTextCapsulePaint)

        // Title (Job Title / Label)
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val displayTitle = labelTitle.ifBlank { "الاسم الوظيفي" }
        canvas.drawText(displayTitle.take(24), 12f, 48f, titlePaint)

        // Code 128 Barcode
        val barPattern = Code128Encoder.generateBarPattern(codeStr)
        if (barPattern.isNotEmpty()) {
            val quietZone = 10
            val totalModules = barPattern.size + quietZone * 2
            val barcodeLeft = 12f
            val barcodeTop = 66f
            val barcodeWidth = 259f
            val barcodeHeight = 52f
            val moduleWidth = barcodeWidth / totalModules.toFloat()

            val barPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }

            for (i in barPattern.indices) {
                if (barPattern[i]) {
                    val left = barcodeLeft + (i + quietZone) * moduleWidth
                    canvas.drawRect(left, barcodeTop, left + moduleWidth + 0.2f, barcodeTop + barcodeHeight, barPaint)
                }
            }
        }

        // Monospace Code Text Underneath Barcode
        val codeTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(codeStr, 100f, 132f, codeTextPaint)

        // Footer Date
        val footerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
            isAntiAlias = true
        }
        val effectiveDate = issueDate.ifBlank { DateTimeUtils.getCurrentDate() }
        canvas.drawText("تاريخ التصدير: $effectiveDate", 12f, 156f, footerPaint)

        pdfDocument.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }

    actual fun generatePngPreview(
        codeStr: String,
        labelTitle: String,
        assignedRole: String,
        issueDate: String
    ): ByteArray? {
        return runCatching {
            val bitmap = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fill Background
            val bgPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 800f, 480f, bgPaint)

            // Draw Border
            val borderPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRoundRect(10f, 10f, 790f, 470f, 20f, 20f, borderPaint)

            // Title
            val displayTitle = labelTitle.ifBlank { "كود موظف" }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(displayTitle, 40f, 100f, titlePaint)

            // Code 128 Barcode
            val barPattern = Code128Encoder.generateBarPattern(codeStr)
            if (barPattern.isNotEmpty()) {
                val quietZone = 10
                val totalModules = barPattern.size + quietZone * 2
                val barcodeLeft = 40f
                val barcodeTop = 160f
                val barcodeWidth = 720f
                val barcodeHeight = 180f
                val moduleWidth = barcodeWidth / totalModules.toFloat()

                val barPaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    style = Paint.Style.FILL
                }

                for (i in barPattern.indices) {
                    if (barPattern[i]) {
                        val left = barcodeLeft + (i + quietZone) * moduleWidth
                        canvas.drawRect(left, barcodeTop, left + moduleWidth + 0.5f, barcodeTop + barcodeHeight, barPaint)
                    }
                }
            }

            val codeTextPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 28f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(codeStr, 320f, 380f, codeTextPaint)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }.getOrNull()
    }
}
