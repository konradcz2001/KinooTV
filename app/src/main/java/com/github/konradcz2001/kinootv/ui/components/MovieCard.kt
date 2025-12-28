package com.github.konradcz2001.kinootv.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CompactCard
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.github.konradcz2001.kinootv.DetailsActivity
import com.github.konradcz2001.kinootv.data.Movie

/**
 * A compact card component representing a single movie or series.
 * Handles image loading via Coil, focus events, and navigation to the Details screen.
 *
 * @param movie The data object containing movie details (title, image URL, etc.).
 * @param onFocused Callback triggered when this card gains input focus.
 * @param width The fixed width of the card (default 120.dp).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    onFocused: () -> Unit,
    width: Dp = 120.dp,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    CompactCard(
        onClick = {
            val intent = Intent(context, DetailsActivity::class.java)
            intent.putExtra("MOVIE_PAGE_URL", movie.moviePageUrl)
            context.startActivity(intent)
        },
        image = {
            AsyncImage(
                model = movie.imageUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        },
        title = {
            Text(
                text = movie.title,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                    .fillMaxWidth()
                    .basicMarquee(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.LightGray
            )
        },
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .onFocusChanged { if (it.isFocused) onFocused() }
    )
}