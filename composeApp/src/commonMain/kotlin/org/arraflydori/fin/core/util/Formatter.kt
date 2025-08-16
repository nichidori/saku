package org.arraflydori.fin.core.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Instant

fun Long.toRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return format.format(this)
}

fun Instant.format(
    format: DateTimeFormat<LocalDateTime>,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val localDateTime = this.toLocalDateTime(timeZone)
    return localDateTime.format(format)
}