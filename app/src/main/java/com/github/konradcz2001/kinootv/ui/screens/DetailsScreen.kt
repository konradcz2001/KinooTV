package com.github.konradcz2001.kinootv.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.github.konradcz2001.kinootv.EpisodeActivity
import com.github.konradcz2001.kinootv.R
import com.github.konradcz2001.kinootv.data.Movie
import com.github.konradcz2001.kinootv.data.MovieDetails
import com.github.konradcz2001.kinootv.data.MovieScraper
import com.github.konradcz2001.kinootv.ui.components.CommentView
import com.github.konradcz2001.kinootv.ui.components.PlayerLinkButton
import com.github.konradcz2001.kinootv.ui.components.YouTubePlayerDialog
import com.github.konradcz2001.kinootv.utils.WatchlistManager
import com.github.konradcz2001.kinootv.utils.getLocalizedVersionName
import com.github.konradcz2001.kinootv.utils.sortLinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

/**
 * Comprehensive details view for a specific movie or TV series.
 * Handles fetching metadata, managing watchlist status via Firebase, displaying seasons/episodes
 * for series, and providing playback options.
 *
 * @param pageUrl The unique URL of the content to display.
 * @param scope CoroutineScope for managing async operations.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DetailsScreen(pageUrl: String, scope: CoroutineScope) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<MovieDetails?>(null) }
    val titleFocusRequester = remember { FocusRequester() }
    var isTrailerLoading by remember { mutableStateOf(false) }

    // State for the YouTube trailer dialog
    var trailerVideoId by remember { mutableStateOf<String?>(null) }

    var isPosterFocused by remember { mutableStateOf(false) }

    // --- FIREBASE: Observe the user's watchlist ---
    val watchlist by WatchlistManager.watchlistFlow.collectAsState()
    val isWatched = watchlist.any { it.moviePageUrl == pageUrl }

    val errorDataFetch = stringResource(R.string.error_data_fetch)
    val trailerNotFound = stringResource(R.string.trailer_not_found)
    val trailerFetchError = stringResource(R.string.trailer_fetch_error)

    LaunchedEffect(Unit) {
        try {
            scope.launch(Dispatchers.IO) {
                val scraper = MovieScraper(context)
                val fetchedDetails = scraper.fetchMovieDetails(pageUrl)
                withContext(Dispatchers.Main) {
                    details = fetchedDetails
                    if (fetchedDetails == null) {
                        Toast.makeText(context, errorDataFetch, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DetailsScreen", "Error", e)
        }
    }

    LaunchedEffect(details) {
        if (details != null) {
            delay(200)
            try { titleFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Handle Back press when trailer is open
    if (trailerVideoId != null) {
        BackHandler { trailerVideoId = null }
    }

    val currentDetails = details

    if (currentDetails == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Red)
        }
    } else {
        var selectedSeason by remember { mutableStateOf(currentDetails.seasons.firstOrNull()) }

        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = currentDetails.backgroundUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)))

            Row(modifier = Modifier.fillMaxSize().padding(48.dp)) {
                // LEFT COLUMN (Poster & Actions)
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .verticalScroll(rememberScrollState())
                        .graphicsLayer { clip = false }
                        .zIndex(if (isPosterFocused) 10f else 0f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 16.dp)
                    ) {
                        Card(
                            onClick = {
                                if (!isTrailerLoading) {
                                    isTrailerLoading = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            // Clean title for better YouTube search results
                                            val rawTitle = currentDetails.title
                                            val slashCount = rawTitle.count { it == '/' }
                                            val queryTitle = if (slashCount == 1) rawTitle.split("/")[1].trim() else rawTitle.trim()
                                            val query = "$queryTitle trailer ${currentDetails.year}"
                                            val scraper = MovieScraper(context)
                                            val videoId = scraper.getYouTubeTrailerId(query)

                                            withContext(Dispatchers.Main) {
                                                isTrailerLoading = false
                                                if (videoId != null && videoId.isNotEmpty()) {
                                                    trailerVideoId = videoId
                                                } else {
                                                    Toast.makeText(context, trailerNotFound, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (_: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isTrailerLoading = false
                                                Toast.makeText(context, trailerFetchError, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(260.dp)
                                .aspectRatio(2f / 3f)
                                .onFocusChanged { isPosterFocused = it.isFocused }
                                .scale(if (isPosterFocused) 0.95f else 1f)
                        ) {
                            AsyncImage(
                                model = currentDetails.posterUrl,
                                contentDescription = currentDetails.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                            }
                        }
                        if (isTrailerLoading) CircularProgressIndicator(color = Color.Red, modifier = Modifier.size(40.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isTrailerLoading) stringResource(R.string.trailer_searching) else stringResource(R.string.trailer_play_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "★ ${currentDetails.rating}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = Color(0xFFB0B0B0), modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(24.dp))

                    // WATCHLIST TOGGLE BUTTON
                    Button(
                        onClick = {
                            if (isWatched) WatchlistManager.removeFromWatchlist(pageUrl)
                            else WatchlistManager.addToWatchlist(Movie(currentDetails.title, currentDetails.description, currentDetails.posterUrl, pageUrl, currentDetails.year, currentDetails.rating, currentDetails.views, null, currentDetails.isSeries))
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = if (isWatched) Color(0xFFE50914) else Color(0xFF333333),
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.border(focusedBorder = Border(border = BorderStroke(2.dp, Color.White))),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = if (isWatched) stringResource(R.string.watchlist_added) else stringResource(R.string.watchlist_add), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(60.dp))
                }

                Spacer(modifier = Modifier.width(32.dp))

                // RIGHT COLUMN (Details & Episodes)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().zIndex(0f).verticalScroll(rememberScrollState())
                ) {
                    Box(modifier = Modifier.focusRequester(titleFocusRequester).focusable().padding(4.dp)) {
                        Text(text = currentDetails.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currentDetails.year, color = Color.LightGray)
                        if (currentDetails.countries.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = currentDetails.countries.joinToString(", "), color = Color.Gray)
                        }
                        if (currentDetails.views.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(R.string.views_format, currentDetails.views), color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    if (currentDetails.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.genres_format, currentDetails.genres.joinToString(", ")), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = currentDetails.description, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFAAAAAA))
                    Spacer(modifier = Modifier.height(24.dp))

                    if (currentDetails.isSeries) {
                        Text(text = stringResource(R.string.seasons_header), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        val seasonRowRequester = remember { FocusRequester() }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            modifier = Modifier.focusProperties {
                                @Suppress("DEPRECATION")
                                enter = { seasonRowRequester }
                            }
                        ) {
                            itemsIndexed(currentDetails.seasons) { index, season ->
                                // Try to parse season number from title (e.g. "Sezon 1" -> 1)
                                val seasonNumber = Regex("\\d+").find(season.title)?.value?.toIntOrNull()

                                val displayTitle = if (seasonNumber != null) {
                                    stringResource(R.string.season_number, seasonNumber)
                                } else {
                                    season.title // Fallback for "Specials", "OVA" etc.
                                }

                                Button(
                                    onClick = { selectedSeason = season },
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (selectedSeason == season) Color(0xFFE50914) else Color(0xFF333333),
                                        contentColor = Color.White
                                    ),
                                    modifier = if (index == 0) Modifier.focusRequester(seasonRowRequester) else Modifier
                                ) {
                                    Text(displayTitle)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedSeason != null) {
                            // Also localize the header "Episodes: Season X"
                            val seasonNumber = Regex("\\d+").find(selectedSeason!!.title)?.value?.toIntOrNull()
                            val displayTitle = if (seasonNumber != null) {
                                stringResource(R.string.season_number, seasonNumber)
                            } else {
                                selectedSeason!!.title
                            }

                            Text(text = stringResource(R.string.episodes_format, displayTitle), style = MaterialTheme.typography.titleMedium, color = Color(0xFFCCCCCC))
                            Spacer(modifier = Modifier.height(8.dp))
                            selectedSeason!!.episodes.forEach { episode ->
                                Button(
                                    onClick = {
                                        val intent = Intent(context, EpisodeActivity::class.java)
                                        intent.putExtra("EPISODE_URL", episode.url)
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.colors(containerColor = Color(0xFF222222), contentColor = Color.LightGray)
                                ) {
                                    Text(episode.title, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    } else {
                        val sortedLinks = sortLinks(currentDetails.playerLinks)
                        val groupedLinks = sortedLinks.groupBy { it.version }

                        if (groupedLinks.isNotEmpty()) {
                            Text(text = stringResource(R.string.players_header), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))

                            groupedLinks.forEach { (versionName, links) ->
                                // Use mapped version name here
                                val displayVersion = getLocalizedVersionName(versionName, context)
                                Text(
                                    text = displayVersion,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFBCAAA4),
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                                val rowRequester = remember { FocusRequester() }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                                    modifier = Modifier.focusProperties {
                                        @Suppress("DEPRECATION")
                                        enter = { rowRequester }
                                    }
                                ) {
                                    itemsIndexed(links) { index, link ->
                                        PlayerLinkButton(
                                            link = link,
                                            modifier = if (index == 0) Modifier.focusRequester(rowRequester) else Modifier
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(stringResource(R.string.no_players), color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = stringResource(R.string.comments_header), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentDetails.comments.isEmpty()) {
                        Text(stringResource(R.string.no_comments), color = Color.Gray)
                    } else {
                        currentDetails.comments.forEach { comment ->
                            CommentView(comment = comment)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        if (trailerVideoId != null) {
            YouTubePlayerDialog(videoId = trailerVideoId!!, onDismiss = { trailerVideoId = null })
        }
    }
}