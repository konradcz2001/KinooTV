package com.github.konradcz2001.kinootv.data

import java.io.Serializable

/**
 * Detailed information about a movie or series.
 * Contains extended metadata, list of player links, comments, and seasons (if applicable).
 */
data class MovieDetails(
    val title: String,
    val posterUrl: String,
    val backgroundUrl: String,
    val description: String,
    val rating: String,
    val year: String,
    val views: String,
    val genres: List<String>,
    val countries: List<String>,
    val playerLinks: List<PlayerLink>,
    val comments: List<Comment>,
    val seasons: List<Season> = emptyList(),
    val isSeries: Boolean = false
) : Serializable