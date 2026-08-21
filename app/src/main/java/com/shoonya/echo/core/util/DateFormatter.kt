package com.shoonya.echo.core.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun String.toInstant(): Instant = Instant.parse(this)

fun Instant.toRelativeTime(now: Instant = Instant.now()): String {
    val duration = Duration.between(this, now)
    return when {
        duration.isNegative -> "just now"
        duration.seconds < 60L -> "just now"
        duration.toMinutes() < 60L -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24L -> "${duration.toHours()}h ago"
        duration.toDays() < 7L -> "${duration.toDays()}d ago"
        else -> {
            val local = atZone(ZoneId.systemDefault())
            "${local.monthValue}/${local.dayOfMonth}/${local.year}"
        }
    }
}

/**
 * Returns a WhatsApp-style day separator label for the given [instant] relative to [now].
 * Result examples: "Today", "Yesterday", "August 11, 2026"
 */
fun Instant.formatDaySeparator(now: Instant = Instant.now()): String {
    val zone = ZoneId.systemDefault()
    val msgDate = atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    val yesterday = today.minusDays(1)

    return when (msgDate) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> {
            val local = atZone(zone)
            val month = local.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
            "${month} ${local.dayOfMonth}, ${local.year}"
        }
    }
}
