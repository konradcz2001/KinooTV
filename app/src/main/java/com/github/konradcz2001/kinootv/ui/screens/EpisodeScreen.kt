package com.github.konradcz2001.kinootv.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.github.konradcz2001.kinootv.EpisodeActivity
import com.github.konradcz2001.kinootv.data.EpisodeDetails
import com.github.konradcz2001.kinootv.data.MovieScraper
import com.github.konradcz2001.kinootv.ui.components.CommentView
import com.github.konradcz2001.kinootv.ui.components.PlayerLinkButton
import com.github.konradcz2001.kinootv.utils.sortLinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen composable responsible for displaying the details of a specific TV series episode.
 * Fetches data asynchronously, displays navigation for previous/next episodes,
 * lists available player links, and shows user comments.
 *
 * @param url The specific URL of the episode to fetch details for.
 * @param scope CoroutineScope used for asynchronous data fetching operations.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EpisodeScreen(url: String, scope: CoroutineScope) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<EpisodeDetails?>(null) }

    LaunchedEffect(url) {
        details = null
        scope.launch(Dispatchers.IO) {
            try {
                val scraper = MovieScraper(context)
                val data = scraper.fetchEpisodeDetails(url)
                withContext(Dispatchers.Main) {
                    details = data
                }
            } catch (e: Exception) {
                Log.e("EpisodeScreen", "Error fetching episode details", e)
            }
        }
    }

    if (details == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Red)
        }
    } else {
        val d = details!!

        Box(modifier = Modifier.fillMaxSize()) {
            if (d.backgroundUrl.isNotEmpty()) {
                AsyncImage(
                    model = d.backgroundUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp)
            ) {
                item {
                    Text(
                        text = d.seriesTitle,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text(
                        text = d.episodeTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFFCCCCCC),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                }

                if (d.description.isNotEmpty()) {
                    item {
                        Text(
                            text = d.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFAAAAAA),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.padding(bottom = 32.dp)) {
                        if (d.prevEpisodeUrl != null) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, EpisodeActivity::class.java)
                                    intent.putExtra("EPISODE_URL", d.prevEpisodeUrl)
                                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFF333333),
                                    contentColor = Color.White,
                                    focusedContainerColor = Color(0xFFE50914)
                                )
                            ) {
                                Text("<< Poprzedni")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        if (d.nextEpisodeUrl != null) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, EpisodeActivity::class.java)
                                    intent.putExtra("EPISODE_URL", d.nextEpisodeUrl)
                                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFF333333),
                                    contentColor = Color.White,
                                    focusedContainerColor = Color(0xFFE50914)
                                )
                            ) {
                                Text("Następny >>")
                            }
                        }
                    }
                }

                val sortedLinks = sortLinks(d.playerLinks)
                val groupedLinks = sortedLinks.groupBy { it.version }

                if (groupedLinks.isNotEmpty()) {
                    item {
                        Text(text = "Odtwarzacze", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    groupedLinks.forEach { (versionName, links) ->
                        item {
                            Column {
                                Text(
                                    text = versionName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFBCAAA4),
                                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                                )
                                val rowRequester = remember { FocusRequester() }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                                    modifier = Modifier.focusProperties { enter = { rowRequester } }
                                ) {
                                    itemsIndexed(links) { index, link ->
                                        PlayerLinkButton(
                                            link = link,
                                            modifier = if (index == 0) Modifier.focusRequester(rowRequester) else Modifier
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item { Text("Brak dostępnych odtwarzaczy", color = Color.Gray) }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                item {
                    Text(text = "Komentarze", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (d.comments.isEmpty()) {
                    item { Text("Brak komentarzy", color = Color.Gray) }
                } else {
                    items(d.comments.size) { index ->
                        CommentView(comment = d.comments[index])
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}