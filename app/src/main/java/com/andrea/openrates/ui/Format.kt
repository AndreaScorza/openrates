package com.andrea.openrates.ui

import com.andrea.openrates.data.Conversion
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Number and time formatting shared by the screen. */
object Format {

    /** Converted amounts: grouped, with enough decimals to stay meaningful. */
    fun amount(value: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            val decimals = Conversion.decimalsFor(value)
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)

    /** Exchange rates carry more precision than money: 1 EUR = 185.65 JPY, 0.000094 BTC-ish values. */
    fun rate(value: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = if (value >= 100) 2 else if (value >= 1) 4 else 6
            minimumFractionDigits = 2
        }.format(value)

    /** "just now", "12 min ago", "3 h ago", "2 days ago". */
    fun relativeTime(epochMs: Long, now: Long = System.currentTimeMillis()): String {
        val delta = (now - epochMs).coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "yesterday"
            else -> "$days days ago"
        }
    }
}
