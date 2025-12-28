package com.github.konradcz2001.kinootv.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.konradcz2001.kinootv.data.Movie

/**
 * Displays a horizontal, scrollable row of [MovieCard] items under a category title.
 * Used for sections like "New Movies", "Kids", etc.
 *
 * @param title The category title displayed above the row.
 * @param movies List of [Movie] objects to display.
 * @param onMovieFocused Callback invoked when a specific movie card gains focus.
 * @param focusRequester Optional FocusRequester to control focus programmatically (e.g. for the first item).
 */
@Composable
fun CategoryRow(
    title: String,
    movies: List<Movie>,
    onMovieFocused: (Movie) -> Unit,
    focusRequester: FocusRequester? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 24.dp).padding(start = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
        ) {
            items(movies.size) { index ->
                val movie = movies[index]
                MovieCard(
                    movie = movie,
                    onFocused = { onMovieFocused(movie) },
                    modifier = if (index == 0 && focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
                )
            }
        }
    }
}