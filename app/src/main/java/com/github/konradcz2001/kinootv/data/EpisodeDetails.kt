package com.github.konradcz2001.kinootv.data

import java.io.Serializable

/**
 * Detailed information about a specific TV episode.
 * Includes navigation links to previous/next episodes and episode-specific metadata.
 */
data class EpisodeDetails(
    val seriesTitle: String,
    val episodeTitle: String,
    val description: String,
    val backgroundUrl: String,
    val playerLinks: List<PlayerLink>,
    val comments: List<Comment>,
    val prevEpisodeUrl: String?,
    val nextEpisodeUrl: String?
) : Serializable