package com.github.konradcz2001.kinootv.data

import java.io.Serializable

/**
 * Represents a season in a TV series, containing a list of episodes.
 */
data class Season(val title: String, val episodes: List<Episode>) : Serializable