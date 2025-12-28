package com.github.konradcz2001.kinootv.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.github.konradcz2001.kinootv.data.PlayerLink

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
 * Calculates a score based on video quality resolution.
 * Higher score indicates better quality.
 *
 * @param quality The string description of quality (e.g., "1080p", "720p").
 * @return Integer score representing quality level.
 */
fun getQualityScore(quality: String): Int {
    return when {
        quality.contains("2160") || quality.contains("4k") -> 4
        quality.contains("1080") -> 3
        quality.contains("720") -> 2
        quality.contains("480") -> 1
        else -> 0
    }
}

/**
 * Sorts a list of player links based on defined heuristics:
 * 1. Version preference (Language/Dubbing).
 * 2. Quality (Resolution).
 * 3. Host preference (downgrading specific hosts like "voe").
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
 * Attempts to open a specific video ID in the external YouTube application.
 * Tries the TV version first, falls back to the standard app, and handles errors gracefully.
 *
 * @param context The context used to start the activity.
 * @param videoId The YouTube video identifier.
 */
fun openYouTubeApp(context: Context, videoId: String) {
    val uri = "https://www.youtube.com/watch?v=$videoId".toUri()
    val tvIntent = Intent(Intent.ACTION_VIEW, uri)
    tvIntent.setPackage("com.google.android.youtube.tv")
    tvIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(tvIntent)
    } catch (_: ActivityNotFoundException) {
        // Fallback to generic viewer if TV app is missing
        val genericIntent = Intent(Intent.ACTION_VIEW, uri)
        genericIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(genericIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open video.", Toast.LENGTH_SHORT).show()
        }
    }
}