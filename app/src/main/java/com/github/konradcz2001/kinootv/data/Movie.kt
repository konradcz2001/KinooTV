package com.github.konradcz2001.kinootv.data

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName
import java.io.Serializable

/**
 * Data model representing a Movie or a Series item.
 * Designed to be compatible with Firebase Realtime Database.
 *
 * @property isSeries Flag indicating if the item is a TV Series. Mapped to JSON key "series" for Firebase compatibility.
 */
@Keep
data class Movie(
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val moviePageUrl: String = "",
    val year: String = "",
    val rating: String? = null,
    val views: String? = null,
    val qualityLabel: String? = null,

    // Ensures Firebase maps the JSON field "series" to the Kotlin property "isSeries"
    @get:PropertyName("series") @set:PropertyName("series")
    var isSeries: Boolean = false
) : Serializable