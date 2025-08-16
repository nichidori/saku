package org.arraflydori.fin.core.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.time.Instant

fun Long.toRupiah(): String {
    val symbols = DecimalFormatSymbols(Locale("in", "ID"))
    val formatter = DecimalFormat("Rp #,###", symbols)
    return formatter.format(this)
}

fun Instant.format(
    format: DateTimeFormat<LocalDateTime>,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val localDateTime = this.toLocalDateTime(timeZone)
    return localDateTime.format(format)
}