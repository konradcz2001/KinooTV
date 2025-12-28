package com.github.konradcz2001.kinootv.data

import java.io.Serializable

/**
 * Represents a single episode item within a season list.
 */
data class Episode(val title: String, val url: String) : Serializable