package com.github.konradcz2001.kinootv.data

import java.io.Serializable

/**
 * Wrapper class for search results, segregated into Movies and Serials.
 */
data class SearchResult(
    val movies: List<Movie>,
    val serials: List<Movie>
) : Serializable