package com.github.konradcz2001.kinootv.utils

import android.content.Context
import com.github.konradcz2001.kinootv.R
import com.github.konradcz2001.kinootv.data.PlayerLink
import java.util.regex.Pattern

/**
 * Calculates a priority score for the video version based on audio and subtitle type.
 * Lower score indicates higher priority.
 *
 * @param version The string description of the video version (e.g., "PL", "Napisy").
 * @return Integer score representing priority (0 is best).
 */
fun getVersionScore(version: String): Int {
    val v = version.trim()

    return when {
        v.equals("PL", ignoreCase = true) -> 0
        v.contains("Dubbing", ignoreCase = true) && !v.contains("Kino", ignoreCase = true) -> 1
        v.equals("Lektor", ignoreCase = true) -> 2
        v.contains("Dubbing_Kino", ignoreCase = true) -> 3
        v.contains("Lektor_AI", ignoreCase = true) -> 4
        v.contains("Lektor_IVO", ignoreCase = true) -> 5
        v.equals("Napisy", ignoreCase = true) -> 6
        v.contains("Napisy_Transl", ignoreCase = true) -> 7
        v.contains("ENG", ignoreCase = true) -> 8
        else -> 100
    }
}

/**
 * Sorts player links based on version priority (PL/Dubbing > Lektor > Napisy)
 * and quality. Prioritizes specific hosts (like "voe").
 *
 * @param links The list of [PlayerLink] objects to sort.
 * @return A sorted list of [PlayerLink].
 */
fun sortLinks(links: List<PlayerLink>): List<PlayerLink> {
    return links.sortedWith(
        compareBy<PlayerLink> { getVersionScore(it.version) }
            .thenByDescending { getQualityScore(it.quality) }
            .thenByDescending { it.hostName.contains("voe", ignoreCase = true) }
    )
}

/**
 * Maps raw version strings from the scraper to localized resources.
 */
fun getLocalizedVersionName(rawVersion: String, context: Context): String {
    val v = rawVersion.trim()
    return when {
        v.equals("Napisy", ignoreCase = true) -> context.getString(R.string.ver_subtitles)
        v.equals("Lektor", ignoreCase = true) -> context.getString(R.string.ver_lector)
        v.equals("Dubbing", ignoreCase = true) -> context.getString(R.string.ver_dubbing)
        v.equals("PL", ignoreCase = true) -> context.getString(R.string.ver_pl)
        v.equals("ENG", ignoreCase = true) -> context.getString(R.string.ver_eng)
        v.equals("Inne", ignoreCase = true) -> context.getString(R.string.ver_other)
        else -> v // Fallback to raw string if unknown
    }
}

/**
 * Parses relative time strings (e.g., "10 godzin temu", "rok temu")
 * and applies proper localized grammar using Plurals resources.
 * If no number is found but a unit is present, it defaults to 1.
 */
fun parseAndLocalizeDate(rawDate: String, context: Context): String {
    val normalized = rawDate.trim().lowercase()

    // 1. Handle specific keywords
    if (normalized.contains("wczoraj")) return context.getString(R.string.time_yesterday)
    if (normalized.contains("dzisiaj")) return context.getString(R.string.time_today)
    if (normalized.contains("chwil")) return context.getString(R.string.time_just_now)

    // 2. Regex with optional number group
    // Pattern: (Optional Number) + (Unit) + "temu"
    // (?:(\d+)\s+)? -> Non-capturing group for the number, which is optional (?)
    // ([a-zA-Ząęćłńóśźż]+) -> The unit (e.g., "godzin", "rok")
    val pattern = Pattern.compile("(?:(\\d+)\\s+)?([a-zA-Ząęćłńóśźż]+)\\s+temu", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(normalized)

    if (matcher.find()) {
        val countString = matcher.group(1)
        val unit = matcher.group(2)?.lowercase() ?: ""

        // Logic: If a number is found, use it. If not (e.g., "rok temu"), default to 1.
        val count = countString?.toIntOrNull() ?: 1

        val pluralId = when {
            // Seconds
            unit.startsWith("sekund") -> R.plurals.time_seconds_ago

            // Minutes (matches "minut", "minuty", "minutę")
            unit.startsWith("minut") -> R.plurals.time_minutes_ago

            // Hours (matches "godzin", "godziny", "godzinę")
            unit.startsWith("godzin") -> R.plurals.time_hours_ago

            // Days (matches "dzień", "dni")
            unit.startsWith("dni") || unit.startsWith("dzie") -> R.plurals.time_days_ago

            // Weeks (matches "tydzień", "tygodnie", "tygodni")
            unit.startsWith("tygodn") || unit.startsWith("tydzie") -> R.plurals.time_weeks_ago

            // Months (matches "miesiąc", "miesiące", "miesięcy")
            unit.startsWith("miesi") -> R.plurals.time_months_ago

            // Years (matches "rok", "lata", "lat")
            unit.startsWith("lat") || unit.startsWith("rok") -> R.plurals.time_years_ago

            else -> return rawDate // Return original if unit is not recognized
        }

        // Android will automatically pick "one" for count=1, "few"/"many" for others
        return context.resources.getQuantityString(pluralId, count, count)
    }

    // Fallback: if regex doesn't match, return original string
    return rawDate
}

internal fun getQualityScore(quality: String): Int {
    return when {
        quality.contains("4K", ignoreCase = true) -> 4
        quality.contains("1080", ignoreCase = true) -> 3
        quality.contains("720", ignoreCase = true) -> 2
        quality.contains("480", ignoreCase = true) -> 1
        else -> 0
    }
}