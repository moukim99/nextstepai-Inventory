package com.nextstepai.inventory.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * أداة مساعدة لتوليد التواريخ والأوقات اللحظية الديناميكية بتنسيق معيار ISO دون أي تواريخ صلبة.
 */
object DateTimeUtils {
    /**
     * جلب التاريخ الحالي بتنسيق YYYY-MM-DD.
     */
    fun getCurrentDate(): String {
        val instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val year = local.year
        val month = local.monthNumber.toString().padStart(2, '0')
        val day = local.dayOfMonth.toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    /**
     * جلب الختم الزمني والتاريخ والوقت بتنسيق YYYY-MM-DD HH:MM:SS.
     */
    fun getCurrentDateTime(): String {
        val instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val year = local.year
        val month = local.monthNumber.toString().padStart(2, '0')
        val day = local.dayOfMonth.toString().padStart(2, '0')
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        val second = local.second.toString().padStart(2, '0')
        return "$year-$month-$day $hour:$minute:$second"
    }
}
