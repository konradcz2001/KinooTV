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
 * Parses relative time strings (e.g. "10 godzin temu") and returns localized format.
 */
fun parseAndLocalizeDate(rawDate: String, context: Context): String {
    // Regex for: Number + Space + Unit + Space + "temu"
    // e.g. "10 godzin temu"
    val pattern = Pattern.compile("(\\d+)\\s+(sekund|minut|godzin|dni|tygodni|miesięcy|lat)\\s+temu", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(rawDate)

    if (matcher.find()) {
        val number = matcher.group(1) ?: ""
        val unit = matcher.group(2)?.lowercase() ?: ""

        val formatResId = when {
            unit.startsWith("sekund") -> R.string.time_format_seconds
            unit.startsWith("minut") -> R.string.time_format_minutes
            unit.startsWith("godzin") -> R.string.time_format_hours
            unit.startsWith("dni") -> R.string.time_format_days
            else -> return rawDate // Complex units (weeks/months) left as is for now
        }
        return context.getString(formatResId, number)
    }

    if (rawDate.contains("wczoraj", ignoreCase = true)) return context.getString(R.string.time_yesterday)
    if (rawDate.contains("dzisiaj", ignoreCase = true)) return context.getString(R.string.time_today)

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