package com.github.konradcz2001.kinootv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.konradcz2001.kinootv.data.Movie
import com.github.konradcz2001.kinootv.ui.components.CategoryRow
import com.github.konradcz2001.kinootv.ui.components.MovieDescriptionBanner
import com.github.konradcz2001.kinootv.utils.WatchlistManager
import kotlinx.coroutines.delay

/**
 * Displays the user's personal watchlist.
 * Observes the [WatchlistManager] flow to reactively update the UI when items are added or removed.
 * Segregates content into "Movies" and "Series" sections.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun WatchlistScreen() {
    val watchlist by WatchlistManager.watchlistFlow.collectAsState()
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    val firstItemRequester = remember { FocusRequester() }

    val density = LocalDensity.current
    val topScrollOffset = remember(density) { with(density) { 50.dp.toPx() } }

    val bringIntoViewSpec = remember(topScrollOffset) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                return offset - topScrollOffset
            }
        }
    }

    LaunchedEffect(watchlist) {
        if (watchlist.isNotEmpty() && focusedMovie == null) {
            focusedMovie = watchlist.first()
        }
        if (watchlist.isNotEmpty()) {
            delay(200)
            try { firstItemRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val movies = watchlist.filter { !it.isSeries }
    val serials = watchlist.filter { it.isSeries }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.height(260.dp).fillMaxWidth()) {
            if (focusedMovie != null) {
                MovieDescriptionBanner(movie = focusedMovie!!)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }

        if (watchlist.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Brak obserwowanych", color = Color.Gray, style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
                val rowRequester = remember { FocusRequester() }
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 220.dp),
                    modifier = Modifier.fillMaxSize().focusProperties { enter = { rowRequester } }
                ) {
                    if (movies.isNotEmpty()) {
                        item {
                            CategoryRow(
                                title = "FILMY",
                                movies = movies,
                                onMovieFocused = { focusedMovie = it },
                                focusRequester = firstItemRequester
                            )
                        }
                    }
                    if (serials.isNotEmpty()) {
                        item {
                            CategoryRow(
                                title = "SERIALE",
                                movies = serials,
                                onMovieFocused = { focusedMovie = it },
                                focusRequester = if (movies.isEmpty()) firstItemRequester else null
                            )
                        }
                    }
                }
            }
        }
    }
}