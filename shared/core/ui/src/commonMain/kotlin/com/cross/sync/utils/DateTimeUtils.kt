@file:OptIn(FormatStringsInDatetimeFormats::class, ExperimentalTime::class)

package com.cross.sync.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val dateTimeFormat = LocalDateTime.Format {
    byUnicodePattern("HH:mm dd.MM.yy")
}

val dateTimeFormatText = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    chars(" ")
    day()
    chars( ", ")
    hour()
    char(':')
    minute()
}

val timeFormat = LocalDateTime.Format {
    byUnicodePattern("HH:mm")
}

fun getNowDate(): LocalDate {
    val now: Instant = Clock.System.now()
    return now.toLocalDateTime(TimeZone.currentSystemDefault()).date
}


fun LocalDateTime.humanize(): String {
    return dateTimeFormat.format(this)
}

fun LocalDateTime.dayFormat(): String {
    return if (date == getNowDate()) {
        "today, ${timeFormat.format(this)}"
        dateTimeFormatText.format(this)
    } else {
        dateTimeFormatText.format(this)
    }
}