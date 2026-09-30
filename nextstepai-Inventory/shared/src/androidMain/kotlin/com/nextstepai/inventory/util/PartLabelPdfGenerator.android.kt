package com.nextstepai.inventory.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.ui.components.Code128Encoder
import java.io.ByteArrayOutputStream

actual object PartLabelPdfGenerator {
    actual fun generatePdf(
        part: Part,
        categoryName: String,
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

        // Nature Capsule
        val (natureText, natureBgColor, natureFgColor) = when {
            part.assembly -> Triple("تجميع BOM", "#F3E8FF", "#7E22CE")
            part.salable -> Triple("منتج تجاري", "#E0F2FE", "#0369A1")
            else -> Triple("مكوّن أولي", "#ECFDF5", "#047857")
        }

        val natureBgPaint = Paint().apply {
            color = Color.parseColor(natureBgColor)
            style = Paint.Style.FILL
        }
        val natureBox = RectF(12f, 10f, 82f, 26f)
        canvas.drawRoundRect(natureBox, 4f, 4f, natureBgPaint)

        val natureTextPaint = Paint().apply {
            color = Color.parseColor(natureFgColor)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(natureText, 16f, 21f, natureTextPaint)

        // IPN Capsule
        val ipnCode = part.ipn.ifBlank { "IPN-${part.id}" }
        val ipnBgPaint = Paint().apply {
            color = Color.parseColor("#EEF2FF")
            style = Paint.Style.FILL
        }
        val ipnBox = RectF(160f, 10f, 271f, 26f)
        canvas.drawRoundRect(ipnBox, 4f, 4f, ipnBgPaint)

        val ipnTextPaint = Paint().apply {
            color = Color.parseColor("#3730A3")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("IPN: $ipnCode".take(18), 164f, 21f, ipnTextPaint)

        // Title (Part Name)
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(part.name.take(24), 12f, 44f, titlePaint)

        // Subtitle (Category Name)
        if (categoryName.isNotBlank()) {
            val subPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                isAntiAlias = true
            }
            canvas.drawText("التصنيف: $categoryName".take(30), 12f, 57f, subPaint)
        }

        // Code 128 Barcode
        val barPattern = Code128Encoder.generateBarPattern(ipnCode)
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
        canvas.drawText(ipnCode, 100f, 132f, codeTextPaint)

        // Footer Date Only
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
        part: Part,
        categoryName: String,
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

            // Nature Capsule
            val (natureText, natureBgColor, natureFgColor) = when {
                part.assembly -> Triple("تجميع BOM", "#F3E8FF", "#7E22CE")
                part.salable -> Triple("منتج تجاري", "#E0F2FE", "#0369A1")
                else -> Triple("مكوّن أولي", "#ECFDF5", "#047857")
            }

            val natureBgPaint = Paint().apply {
                color = Color.parseColor(natureBgColor)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(30f, 25f, 220f, 70f, 10f, 10f, natureBgPaint)

            val natureTextPaint = Paint().apply {
                color = Color.parseColor(natureFgColor)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(natureText, 45f, 55f, natureTextPaint)

            // IPN Capsule
            val ipnCode = part.ipn.ifBlank { "IPN-${part.id}" }
            val ipnBgPaint = Paint().apply {
                color = Color.parseColor("#EEF2FF")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(480f, 25f, 770f, 70f, 10f, 10f, ipnBgPaint)

            val ipnTextPaint = Paint().apply {
                color = Color.parseColor("#3730A3")
                textSize = 24f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("IPN: $ipnCode".take(18), 500f, 57f, ipnTextPaint)

            // Title (Part Name)
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(part.name.take(24), 30f, 130f, titlePaint)

            // Category Name Subtitle
            if (categoryName.isNotBlank()) {
                val subPaint = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = 24f
                    isAntiAlias = true
                }
                canvas.drawText("التصنيف: $categoryName".take(30), 30f, 170f, subPaint)
            }

            // Code 128 Barcode
            val barPattern = Code128Encoder.generateBarPattern(ipnCode)
            if (barPattern.isNotEmpty()) {
                val quietZone = 10
                val totalModules = barPattern.size + quietZone * 2
                val barcodeLeft = 30f
                val barcodeTop = 195f
                val barcodeWidth = 740f
                val barcodeHeight = 155f
                val moduleWidth = barcodeWidth / totalModules.toFloat()

                val barPaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    style = Paint.Style.FILL
                }

                for (i in barPattern.indices) {
                    if (barPattern[i]) {
                        val left = barcodeLeft + (i + quietZone) * moduleWidth
                        canvas.drawRect(left, barcodeTop, left + moduleWidth + 0.4f, barcodeTop + barcodeHeight, barPaint)
                    }
                }
            }

            // Code Text Underneath Barcode
            val codeTextPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 28f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(ipnCode, 320f, 385f, codeTextPaint)

            // Footer
            val footerPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 20f
                isAntiAlias = true
            }
            val effectiveDate = issueDate.ifBlank { DateTimeUtils.getCurrentDate() }
            canvas.drawText("تاريخ التصدير: $effectiveDate", 30f, 440f, footerPaint)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }.getOrNull()
    }
}
