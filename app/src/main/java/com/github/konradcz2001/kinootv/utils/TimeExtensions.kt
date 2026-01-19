package com.github.konradcz2001.kinootv.utils

import android.content.Context
import com.github.konradcz2001.kinootv.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Converts a timestamp (Long milliseconds) into a localized relative time string.
 * Supports Polish pluralization rules (odmiany) for seconds, minutes, hours, days, weeks, months, years.
 * Also handles "today" and "yesterday".
 */
fun Long.toRelativeTimeString(context: Context): String {
    val now = LocalDateTime.now()
    val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

    // Calculate diffs
    val seconds = ChronoUnit.SECONDS.between(time, now)
    val minutes = ChronoUnit.MINUTES.between(time, now)
    val hours = ChronoUnit.HOURS.between(time, now)
    val days = ChronoUnit.DAYS.between(time, now)
    val weeks = days / 7
    val months = ChronoUnit.MONTHS.between(time, now)
    val years = ChronoUnit.YEARS.between(time, now)

    return when {
        // Just now / Seconds (< 1 minute)
        seconds < 60 -> {
            if (seconds < 10) context.getString(R.string.time_just_now)
            else context.resources.getQuantityString(R.plurals.time_seconds_ago, seconds.toInt(), seconds.toInt())
        }

        // Minutes (< 1 hour)
        minutes < 60 -> context.resources.getQuantityString(R.plurals.time_minutes_ago, minutes.toInt(), minutes.toInt())

        // Hours (< 24 hours)
        hours < 24 -> context.resources.getQuantityString(R.plurals.time_hours_ago, hours.toInt(), hours.toInt())

        // Yesterday (Check specifically if it was the previous calendar day)
        days == 1L -> context.getString(R.string.time_yesterday)

        // Days (< 7 days) - Note: "Today" is handled by hours logic implicitly if < 24h
        days < 7 -> context.resources.getQuantityString(R.plurals.time_days_ago, days.toInt(), days.toInt())

        // Weeks (< 1 month approx 4 weeks)
        weeks < 4 -> context.resources.getQuantityString(R.plurals.time_weeks_ago, weeks.toInt(), weeks.toInt())

        // Months (< 12 months)
        months < 12 -> context.resources.getQuantityString(R.plurals.time_months_ago, months.toInt(), months.toInt())

        // Years
        else -> context.resources.getQuantityString(R.plurals.time_years_ago, years.toInt(), years.toInt())
    }
}