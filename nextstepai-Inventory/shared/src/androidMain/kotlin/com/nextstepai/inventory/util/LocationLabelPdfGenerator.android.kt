package com.nextstepai.inventory.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.capacityUnit
import com.nextstepai.inventory.data.effectiveCapacity
import com.nextstepai.inventory.data.formatQuantity
import com.nextstepai.inventory.ui.components.QrMatrixEncoder
import java.io.ByteArrayOutputStream

actual object LocationLabelPdfGenerator {
    actual fun generatePdf(
        location: StockLocation,
        parentPath: String,
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

        // Title / Name
        val textPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(location.name.take(24), 12f, 24f, textPaint)

        // Subtitle / Parent Path
        val subPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9f
            isAntiAlias = true
        }
        val displayPath = parentPath.ifBlank { "موقع رئيسي (Root)" }
        canvas.drawText("📌 $displayPath".take(32), 12f, 38f, subPaint)

        // Capacity Text
        val capPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("السعة القصوى: ${location.effectiveCapacity.formatQuantity()} ${location.capacityUnit}", 12f, 54f, capPaint)

        // QR Code Matrix
        val payload = BarcodePayloadHelper.generateLocationPayload(location.effectiveUuid)
        val matrix = QrMatrixEncoder.generateMatrix(payload)
        val matrixSize = matrix.size
        val qrBoxSize = 90f
        val cellSize = qrBoxSize / matrixSize
        val qrLeft = 180f
        val qrTop = 64f

        val qrPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c]) {
                    val left = qrLeft + c * cellSize
                    val top = qrTop + r * cellSize
                    canvas.drawRect(left, top, left + cellSize, top + cellSize, qrPaint)
                }
            }
        }

        // Human Readable Code Box
        val codeBgPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val codeBox = RectF(12f, 68f, 170f, 108f)
        canvas.drawRoundRect(codeBox, 6f, 6f, codeBgPaint)

        val codeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        val shortCode = location.effectiveUuid.removePrefix("location-").let { if (it.all { ch -> ch.isDigit() }) "LOC-$it" else it.uppercase() }
        canvas.drawText(shortCode, 20f, 94f, codeTextPaint)

        // Footer Date & UUID
        val footerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
            isAntiAlias = true
        }
        val effectiveDate = issueDate.ifBlank { DateTimeUtils.getCurrentDate() }
        canvas.drawText("تاريخ الإصدار: $effectiveDate • ${location.effectiveUuid}", 12f, 156f, footerPaint)

        pdfDocument.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }

    actual fun generatePngPreview(
        location: StockLocation,
        parentPath: String,
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

            // Draw Title
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(location.name.take(24), 30f, 70f, titlePaint)

            // Subtitle
            val subPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 24f
                isAntiAlias = true
            }
            val displayPath = parentPath.ifBlank { "موقع رئيسي (Root)" }
            canvas.drawText("📌 $displayPath".take(32), 30f, 110f, subPaint)

            // QR Code Matrix
            val payload = BarcodePayloadHelper.generateLocationPayload(location.effectiveUuid)
            val matrix = QrMatrixEncoder.generateMatrix(payload)
            val matrixSize = matrix.size
            val qrBoxSize = 280f
            val cellSize = qrBoxSize / matrixSize
            val qrLeft = 480f
            val qrTop = 150f

            val qrPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (matrix[r][c]) {
                        val left = qrLeft + c * cellSize
                        val top = qrTop + r * cellSize
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, qrPaint)
                    }
                }
            }

            // Code Badge
            val codeBgPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            val codeBox = RectF(30f, 180f, 440f, 300f)
            canvas.drawRoundRect(codeBox, 16f, 16f, codeBgPaint)

            val codeTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 42f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
            val shortCode = location.effectiveUuid.removePrefix("location-").let { if (it.all { ch -> ch.isDigit() }) "LOC-$it" else it.uppercase() }
            canvas.drawText(shortCode, 50f, 255f, codeTextPaint)

            // Footer
            val footerPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 20f
                isAntiAlias = true
            }
            val effectiveDate = issueDate.ifBlank { DateTimeUtils.getCurrentDate() }
            canvas.drawText("تاريخ الإصدار: $effectiveDate • ${location.effectiveUuid}", 30f, 440f, footerPaint)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }.getOrNull()
    }
}
