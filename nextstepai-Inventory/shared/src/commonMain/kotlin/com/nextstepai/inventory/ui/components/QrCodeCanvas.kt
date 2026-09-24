package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * مكون رسم رمز الاستجابة السريعة (QR Code) باستخدام Compose Canvas الموحد بدون أي مكتبات خارجية.
 */
@Composable
fun QrCodeCanvas(
    content: String,
    modifier: Modifier = Modifier,
    darkColor: Color = Color(0xFF0F172A),
    lightColor: Color = Color.White,
    quietZonePadding: Boolean = true
) {
    val matrix = remember(content) { QrMatrixEncoder.generateMatrix(content) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(lightColor)
            .then(if (quietZonePadding) Modifier.padding(8.dp) else Modifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = matrix.size
            if (size == 0) return@Canvas

            val cellSize = min(this.size.width, this.size.height) / size.toFloat()

            // رسم أرضية مصفوفة الـ QR (Light Background)
            drawRect(color = lightColor)

            // رسم مربعات البيانات والرموز المظلمة (Dark Modules)
            for (r in 0 until size) {
                for (c in 0 until size) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f) // overlapping micro pixel for gapless rendering
                        )
                    }
                }
            }
        }
    }
}

/**
 * محرك تشفير وبناء مصفوفة الـ QR Code القياسية (Standard ISO/IEC 18004 QR Matrix Generator).
 */
internal object QrMatrixEncoder {

    private val expTable = IntArray(512)
    private val logTable = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            expTable[i] = x
            logTable[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            expTable[i] = expTable[i - 255]
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return expTable[logTable[x] + logTable[y]]
    }

    private fun rsGenPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val nextPoly = IntArray(poly.size + 1)
            val factor = expTable[i]
            for (j in poly.indices) {
                nextPoly[j] = nextPoly[j] xor gfMul(poly[j], factor)
                nextPoly[j + 1] = nextPoly[j + 1] xor poly[j]
            }
            poly = nextPoly
        }
        return poly
    }

    private fun calculateErrorCorrection(data: IntArray, ecCount: Int): IntArray {
        val gen = rsGenPoly(ecCount)
        val res = IntArray(data.size + ecCount)
        data.copyInto(res)
        for (i in data.indices) {
            val coef = res[i]
            if (coef != 0) {
                for (j in gen.indices) {
                    res[i + j] = res[i + j] xor gfMul(gen[j], coef)
                }
            }
        }
        return res.copyOfRange(data.size, res.size)
    }

    data class QrVersionSpec(
        val version: Int,
        val size: Int,
        val totalDataBytes: Int,
        val ecBytes: Int,
        val alignmentPos: IntArray
    )

    private fun getVersionSpec(contentLength: Int): QrVersionSpec {
        return when {
            contentLength <= 14 -> QrVersionSpec(1, 21, 19, 7, intArrayOf())
            contentLength <= 26 -> QrVersionSpec(2, 25, 34, 10, intArrayOf(6, 18))
            contentLength <= 42 -> QrVersionSpec(3, 29, 55, 15, intArrayOf(6, 22))
            contentLength <= 62 -> QrVersionSpec(4, 33, 80, 20, intArrayOf(6, 26))
            else -> QrVersionSpec(5, 37, 108, 26, intArrayOf(6, 30))
        }
    }

    fun generateMatrix(text: String): Array<BooleanArray> {
        val spec = getVersionSpec(text.length)
        val size = spec.size
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // 1. رسم Finder Patterns (المربعات الثلاثة للزوايا)
        drawFinderPattern(matrix, reserved, 0, 0)
        drawFinderPattern(matrix, reserved, size - 7, 0)
        drawFinderPattern(matrix, reserved, 0, size - 7)

        // 2. رسم Timing Patterns (خط التناسق المتبادل)
        for (i in 0 until size) {
            if (!reserved[6][i]) {
                matrix[6][i] = (i % 2 == 0)
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = (i % 2 == 0)
                reserved[i][6] = true
            }
        }

        // 3. رسم Alignment Patterns
        if (spec.alignmentPos.isNotEmpty()) {
            for (r in spec.alignmentPos) {
                for (c in spec.alignmentPos) {
                    if (!reserved[r][c]) {
                        drawAlignmentPattern(matrix, reserved, r, c)
                    }
                }
            }
        }

        // 4. حجز وحدات معلومات التنسيق (Format Info Area)
        for (i in 0..8) {
            if (i < size) {
                reserved[8][i] = true
                reserved[i][8] = true
            }
        }
        for (i in (size - 8) until size) {
            reserved[8][i] = true
            reserved[i][8] = true
        }

        // Dark Module
        reserved[size - 8][8] = true
        matrix[size - 8][8] = true

        // 5. تجهيز حمولة البيانات والـ Bit Stream
        val bytes = text.encodeToByteArray()
        val dataBits = mutableListOf<Boolean>()

        // Mode Indicator (0100 for Byte Mode)
        addBits(dataBits, 0b0100, 4)
        // Character Count Indicator
        val countBits = if (spec.version <= 9) 8 else 16
        addBits(dataBits, bytes.size, countBits)

        // Data Bytes
        for (b in bytes) {
            addBits(dataBits, b.toInt() and 0xFF, 8)
        }

        // Terminator Bits (0000)
        val maxDataBits = spec.totalDataBytes * 8
        val termLen = min(4, maxDataBits - dataBits.size)
        if (termLen > 0) {
            addBits(dataBits, 0, termLen)
        }

        // Pad to byte boundary
        while (dataBits.size % 8 != 0) {
            dataBits.add(false)
        }

        // Pad Bytes (0xEC, 0x11)
        val padPattern = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (dataBits.size < maxDataBits) {
            addBits(dataBits, padPattern[padIdx], 8)
            padIdx = (padIdx + 1) % 2
        }

        // تحويل الـ Bits إلى Bytes للبيانات والتصحيح
        val dataInts = IntArray(spec.totalDataBytes)
        for (i in 0 until spec.totalDataBytes) {
            var v = 0
            for (bit in 0 until 8) {
                if (dataBits[i * 8 + bit]) {
                    v = v or (1 shl (7 - bit))
                }
            }
            dataInts[i] = v
        }

        val ecInts = calculateErrorCorrection(dataInts, spec.ecBytes)

        // دمج البيانات ورموز التصحيح
        val finalBits = mutableListOf<Boolean>()
        for (v in dataInts) addBits(finalBits, v, 8)
        for (v in ecInts) addBits(finalBits, v, 8)

        // 6. وضع البتات في المصفوفة بنمط Zig-zag
        var bitIndex = 0
        var right = size - 1
        while (right > 0) {
            if (right == 6) right-- // تجاوز خط الـ Timing Vertical

            for (vertical in 0 until size) {
                for (columnOffset in 0..1) {
                    val c = right - columnOffset
                    val upwards = ((right + 1) / 2) % 2 == 1
                    val r = if (upwards) size - 1 - vertical else vertical

                    if (!reserved[r][c]) {
                        val bit = if (bitIndex < finalBits.size) finalBits[bitIndex++] else false
                        // تطبييق القناع القياسي (Mask 0: (row + col) % 2 == 0)
                        val mask = (r + c) % 2 == 0
                        matrix[r][c] = bit xor mask
                    }
                }
            }
            right -= 2
        }

        // 7. تطبيق معلومات التنسيق (Format Info for Mask 0 / Level L: 0x77C4)
        applyFormatInfo(matrix, 0x77C4, size)

        return matrix
    }

    private fun addBits(bits: MutableList<Boolean>, value: Int, bitCount: Int) {
        for (i in (bitCount - 1) downTo 0) {
            bits.add(((value shr i) and 1) == 1)
        }
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, row: Int, col: Int) {
        for (r in -1..7) {
            for (c in -1..7) {
                val mr = row + r
                val mc = col + c
                if (mr in matrix.indices && mc in matrix.indices) {
                    reserved[mr][mc] = true
                    if (r in 0..6 && c in 0..6) {
                        matrix[mr][mc] = (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                    } else {
                        matrix[mr][mc] = false
                    }
                }
            }
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, row: Int, col: Int) {
        for (r in -2..2) {
            for (c in -2..2) {
                val mr = row + r
                val mc = col + c
                if (mr in matrix.indices && mc in matrix.indices) {
                    reserved[mr][mc] = true
                    matrix[mr][mc] = (r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0))
                }
            }
        }
    }

    private fun applyFormatInfo(matrix: Array<BooleanArray>, formatVal: Int, size: Int) {
        for (i in 0..14) {
            val bit = ((formatVal shr i) and 1) == 1

            // حول النمط العلوي الأيسر
            val (r1, c1) = when {
                i < 6 -> Pair(8, i)
                i == 6 -> Pair(8, 7)
                i == 7 -> Pair(8, 8)
                i == 8 -> Pair(7, 8)
                else -> Pair(14 - i, 8)
            }
            matrix[r1][c1] = bit

            // حول النمط السفلي والأيمن
            val (r2, c2) = when {
                i < 8 -> Pair(size - 1 - i, 8)
                else -> Pair(8, size - 15 + i)
            }
            matrix[r2][c2] = bit
        }
    }
}
