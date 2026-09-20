package ru.unicorecms.unicoreconnect.common.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formats {
    private val money = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.forLanguageTag("ru")))

    fun money(amount: Double): String = money.format(amount)

    fun real(amount: Double): String = "${money.format(amount)} руб."

    fun duration(minutes: Long): String {
        val totalMinutes = if (minutes < 0) 0 else minutes
        val days = TimeUnit.MINUTES.toDays(totalMinutes)
        val hours = TimeUnit.MINUTES.toHours(totalMinutes) % 24
        val rest = totalMinutes % 60
        val parts = mutableListOf<String>()

        if (days > 0) parts.add("${days}д")
        if (hours > 0) parts.add("${hours}ч")
        if (rest > 0 || parts.isEmpty()) parts.add("${rest}м")

        return parts.joinToString(" ")
    }
}
