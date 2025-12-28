package com.github.konradcz2001.kinootv.data

import java.io.Serializable

/**
 * Represents a streaming link source.
 *
 * @property dataIframe Base64 encoded JSON or URL string containing the player source.
 * @property version Language version (e.g., "PL", "Napisy").
 * @property quality Video resolution quality (e.g., "1080p").
 */
data class PlayerLink(
    val hostName: String,
    val dataIframe: String,
    val quality: String,
    val version: String,
    val addedDate: String = ""
) : Serializable