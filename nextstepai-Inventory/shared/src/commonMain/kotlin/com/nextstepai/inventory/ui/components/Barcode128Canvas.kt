package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * مكون رسم الباركود الخطي 1D (Code 128 Barcode Canvas) باستخدام Compose Canvas الموحد بدون أي مكتبات خارجية.
 * مخصص حصرياً لتمثيل كود الـ IPN على ملصقات القطع مع طباعة كود مقروء بالعين والماسحات الضوئية أسفله.
 */
@Composable
fun Barcode128Canvas(
    content: String,
    modifier: Modifier = Modifier,
    darkColor: Color = Color(0xFF0F172A),
    lightColor: Color = Color.White,
    showText: Boolean = true,
    quietZoneModules: Int = 10
) {
    val barPattern = remember(content) { Code128Encoder.generateBarPattern(content) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(lightColor)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (barPattern.isEmpty()) return@Canvas
                val totalModules = barPattern.size + (quietZoneModules * 2)
                val moduleWidth = size.width / totalModules.toFloat()
                val barHeight = size.height

                // رسم أرضية ملصق الباركود
                drawRect(color = lightColor)

                // رسم الخطوط والأعمدة السوداء (Black Bars)
                for (i in barPattern.indices) {
                    if (barPattern[i]) {
                        val x = (i + quietZoneModules) * moduleWidth
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(x, 0f),
                            size = Size(moduleWidth + 0.3f, barHeight)
                        )
                    }
                }
            }

            if (showText && content.isNotBlank()) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    ),
                    color = darkColor,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * محرك تشفير وبناء نمط الخطوط والأعمدة للباركود الخطي Standard Code 128 (Code B).
 */
object Code128Encoder {

    // مصفوفة أطوال الأشرطة والمواضع (107 رموز قياسية للباركود الخطي Code 128)
    private val PATTERNS = arrayOf(
        intArrayOf(2, 1, 2, 2, 2, 2), // 0
        intArrayOf(2, 2, 2, 1, 2, 2), // 1
        intArrayOf(2, 2, 2, 2, 2, 1), // 2
        intArrayOf(1, 2, 1, 2, 2, 3), // 3
        intArrayOf(1, 2, 1, 3, 2, 2), // 4
        intArrayOf(1, 3, 1, 2, 2, 2), // 5
        intArrayOf(1, 2, 2, 2, 1, 3), // 6
        intArrayOf(1, 2, 2, 3, 1, 2), // 7
        intArrayOf(1, 3, 2, 2, 1, 2), // 8
        intArrayOf(2, 2, 1, 2, 1, 3), // 9
        intArrayOf(2, 2, 1, 3, 1, 2), // 10
        intArrayOf(2, 3, 1, 2, 1, 2), // 11
        intArrayOf(1, 1, 2, 2, 3, 2), // 12
        intArrayOf(1, 2, 2, 1, 3, 2), // 13
        intArrayOf(1, 2, 2, 2, 3, 1), // 14
        intArrayOf(1, 1, 3, 2, 2, 2), // 15
        intArrayOf(1, 2, 3, 1, 2, 2), // 16
        intArrayOf(1, 2, 3, 2, 2, 1), // 17
        intArrayOf(2, 2, 3, 2, 1, 1), // 18
        intArrayOf(2, 2, 1, 1, 3, 2), // 19
        intArrayOf(2, 2, 1, 2, 3, 1), // 20
        intArrayOf(2, 1, 3, 2, 1, 2), // 21
        intArrayOf(2, 2, 3, 1, 1, 2), // 22
        intArrayOf(3, 1, 2, 1, 3, 1), // 23
        intArrayOf(3, 1, 1, 2, 2, 2), // 24
        intArrayOf(3, 2, 1, 1, 2, 2), // 25
        intArrayOf(3, 2, 1, 2, 2, 1), // 26
        intArrayOf(3, 1, 2, 2, 1, 2), // 27
        intArrayOf(3, 2, 2, 1, 1, 2), // 28
        intArrayOf(3, 2, 2, 2, 1, 1), // 29
        intArrayOf(2, 1, 2, 1, 2, 3), // 30
        intArrayOf(2, 1, 2, 3, 2, 1), // 31
        intArrayOf(2, 3, 2, 1, 2, 1), // 32
        intArrayOf(1, 1, 1, 3, 2, 3), // 33
        intArrayOf(1, 3, 1, 1, 2, 3), // 34
        intArrayOf(1, 3, 1, 3, 2, 1), // 35
        intArrayOf(1, 1, 2, 3, 1, 3), // 36
        intArrayOf(1, 3, 2, 1, 1, 3), // 37
        intArrayOf(1, 3, 2, 3, 1, 1), // 38
        intArrayOf(2, 1, 1, 3, 1, 3), // 39
        intArrayOf(2, 3, 1, 1, 1, 3), // 40
        intArrayOf(2, 3, 1, 3, 1, 1), // 41
        intArrayOf(1, 1, 2, 1, 3, 3), // 42
        intArrayOf(1, 1, 2, 3, 3, 1), // 43
        intArrayOf(1, 3, 2, 1, 3, 1), // 44
        intArrayOf(1, 1, 3, 1, 2, 3), // 45
        intArrayOf(1, 1, 3, 3, 2, 1), // 46
        intArrayOf(1, 3, 3, 1, 2, 1), // 47
        intArrayOf(3, 1, 3, 1, 2, 1), // 48
        intArrayOf(2, 1, 1, 3, 3, 1), // 49
        intArrayOf(2, 3, 1, 1, 3, 1), // 50
        intArrayOf(2, 1, 3, 1, 1, 3), // 51
        intArrayOf(2, 1, 3, 3, 1, 1), // 52
        intArrayOf(2, 1, 3, 1, 3, 1), // 53
        intArrayOf(3, 1, 1, 1, 2, 3), // 54
        intArrayOf(3, 1, 1, 3, 2, 1), // 55
        intArrayOf(3, 3, 1, 1, 2, 1), // 56
        intArrayOf(3, 1, 2, 1, 1, 3), // 57
        intArrayOf(3, 1, 2, 3, 1, 1), // 58
        intArrayOf(3, 3, 2, 1, 1, 1), // 59
        intArrayOf(3, 1, 4, 1, 1, 1), // 60
        intArrayOf(2, 2, 1, 4, 1, 1), // 61
        intArrayOf(4, 3, 1, 1, 1, 1), // 62
        intArrayOf(1, 1, 1, 2, 2, 4), // 63
        intArrayOf(1, 1, 1, 4, 2, 2), // 64
        intArrayOf(1, 2, 1, 1, 2, 4), // 65
        intArrayOf(1, 2, 1, 4, 2, 1), // 66
        intArrayOf(1, 4, 1, 1, 2, 2), // 67
        intArrayOf(1, 4, 1, 2, 2, 1), // 68
        intArrayOf(1, 1, 2, 2, 1, 4), // 69
        intArrayOf(1, 1, 2, 4, 1, 2), // 70
        intArrayOf(1, 2, 2, 1, 1, 4), // 71
        intArrayOf(1, 2, 2, 4, 1, 1), // 72
        intArrayOf(1, 4, 2, 1, 1, 2), // 73
        intArrayOf(1, 4, 2, 2, 1, 1), // 74
        intArrayOf(2, 4, 1, 2, 1, 1), // 75
        intArrayOf(2, 2, 1, 1, 1, 4), // 76
        intArrayOf(4, 1, 3, 1, 1, 1), // 77
        intArrayOf(2, 4, 1, 1, 1, 2), // 78
        intArrayOf(1, 3, 4, 1, 1, 1), // 79
        intArrayOf(1, 1, 1, 2, 4, 2), // 80
        intArrayOf(1, 2, 1, 1, 4, 2), // 81
        intArrayOf(1, 2, 1, 2, 4, 1), // 82
        intArrayOf(1, 1, 4, 2, 1, 2), // 83
        intArrayOf(1, 2, 4, 1, 1, 2), // 84
        intArrayOf(1, 2, 4, 2, 1, 1), // 85
        intArrayOf(4, 1, 1, 2, 1, 2), // 86
        intArrayOf(4, 2, 1, 1, 1, 2), // 87
        intArrayOf(4, 2, 1, 2, 1, 1), // 88
        intArrayOf(2, 1, 2, 1, 4, 1), // 89
        intArrayOf(2, 1, 4, 1, 2, 1), // 90
        intArrayOf(9, 1, 2, 1, 2, 1), // 91
        intArrayOf(1, 1, 1, 1, 4, 3), // 92
        intArrayOf(1, 1, 1, 3, 4, 1), // 93
        intArrayOf(1, 3, 1, 1, 4, 1), // 94
        intArrayOf(1, 1, 4, 1, 1, 3), // 95
        intArrayOf(1, 1, 4, 3, 1, 1), // 96
        intArrayOf(4, 1, 1, 1, 1, 3), // 97
        intArrayOf(4, 1, 1, 3, 1, 1), // 98
        intArrayOf(1, 1, 3, 1, 4, 1), // 99
        intArrayOf(1, 1, 4, 1, 3, 1), // 100
        intArrayOf(3, 1, 1, 1, 4, 1), // 101
        intArrayOf(4, 1, 1, 1, 3, 1), // 102
        intArrayOf(2, 1, 1, 4, 1, 2), // 103 (Start A)
        intArrayOf(2, 1, 1, 2, 1, 4), // 104 (Start B)
        intArrayOf(2, 1, 1, 2, 3, 2), // 105 (Start C)
        intArrayOf(2, 3, 3, 1, 1, 1, 2) // 106 (Stop)
    )

    fun generateBarPattern(text: String): BooleanArray {
        val cleanText = text.ifBlank { "GEN-0001" }
        val symbols = mutableListOf<Int>()

        // 1. رمز البداية Start B (104)
        symbols.add(104)

        // 2. تحويل الرموز النصية إلى أكواد الرموز
        var checksumSum = 104L
        for (i in cleanText.indices) {
            val charCode = cleanText[i].code
            val symbolVal = if (charCode in 32..127) charCode - 32 else 0
            symbols.add(symbolVal)
            checksumSum += (i + 1).toLong() * symbolVal.toLong()
        }

        // 3. رمز التدقيق (Checksum Value)
        val checksumSymbol = (checksumSum % 103).toInt()
        symbols.add(checksumSymbol)

        // 4. رمز النهاية Stop Code (106)
        symbols.add(106)

        // 5. تحويل الأكواد إلى أشرطة خطية وحساب العرض المطلوب
        val resultBits = mutableListOf<Boolean>()
        for (sym in symbols) {
            val pattern = PATTERNS[sym]
            var isBar = true
            for (width in pattern) {
                repeat(width) {
                    resultBits.add(isBar)
                }
                isBar = !isBar
            }
        }

        return resultBits.toBooleanArray()
    }
}
