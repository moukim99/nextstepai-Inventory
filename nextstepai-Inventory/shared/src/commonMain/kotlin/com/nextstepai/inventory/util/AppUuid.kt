package com.nextstepai.inventory.util

import com.github.f4b6a3.uuid.UuidCreator
import kotlin.random.Random
import kotlin.time.Clock

/**
 * المولد المركزي لمعرفات UUIDv7 المعيارية (RFC 9562) لجميع كيانات وقواعد بيانات التطبيق.
 * تعتمد UUIDv7 على الطابع الزمني الملي-ثاني لضمان الترتيب الزمني الفعال (Monotonic / Time-ordered)
 * مما يرفع أداء الفهارس والاستعلامات في SQLite و Room.
 */
object AppUuid {

    /**
     * توليد معرف فريد بصيغة UUIDv7 معتمد رسمياً.
     * يستخدم مكتبة `uuid-creator` المعيارية أو الخوارزمية المتوافقة مع RFC 9562.
     *
     * @return سلسلة نصية بصيغة `xxxxxxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx`
     */
    fun generate(): String {
        return try {
            UuidCreator.getTimeOrderedEpoch().toString()
        } catch (_: Throwable) {
            generatePureKotlinUuidv7()
        }
    }

    /**
     * توليد متوافق مع معيار RFC 9562 بأسلوب Pure Kotlin Multiplatform.
     */
    private fun generatePureKotlinUuidv7(): String {
        val now = Clock.System.now().toEpochMilliseconds()

        // MSB: 48 bits timestamp | 4 bits version (0x7) | 12 bits sequence/random
        val randA = Random.nextInt(0x1000).toLong()
        val msb = (now shl 16) or 0x7000L or randA

        // LSB: 2 bits variant (0b10) | 62 bits random
        val randB = Random.nextLong()
        val lsb = (0x2L shl 62) or (randB and 0x3FFFFFFFFFFFFFFFL)

        return formatUuidHex(msb, lsb)
    }

    private fun formatUuidHex(msb: Long, lsb: Long): String {
        fun Long.toHex(digits: Int): String {
            val hexDigits = "0123456789abcdef"
            val sb = StringBuilder(digits)
            for (i in (digits - 1) downTo 0) {
                val shift = i * 4
                val nibble = ((this ushr shift) and 0xF).toInt()
                sb.append(hexDigits[nibble])
            }
            return sb.toString()
        }

        val p1 = (msb ushr 32).toHex(8)
        val p2 = ((msb ushr 16) and 0xFFFF).toHex(4)
        val p3 = (msb and 0xFFFF).toHex(4)
        val p4 = (lsb ushr 48).toHex(4)
        val p5 = (lsb and 0xFFFFFFFFFFFFL).toHex(12)

        return "$p1-$p2-$p3-$p4-$p5"
    }
}
