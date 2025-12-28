package com.github.konradcz2001.kinootv.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.konradcz2001.kinootv.MainActivity
import com.github.konradcz2001.kinootv.data.Movie
import com.github.konradcz2001.kinootv.data.MovieScraper
import com.github.konradcz2001.kinootv.ui.components.CategoryRow
import com.github.konradcz2001.kinootv.ui.components.MovieDescriptionBanner
import com.github.konradcz2001.kinootv.utils.SessionExpiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The main dashboard screen of the application.
 * Fetches and displays horizontal rows of content (e.g., Latest Movies, Popular Series) for easy browsing.
 * Redirects to the Login screen if the session is detected as expired.
 */
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var rows by remember { mutableStateOf<LinkedHashMap<String, List<Movie>>?>(null) }
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }

    val firstItemRequester = remember { FocusRequester() }
    var isLoaded by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val topScrollOffset = remember(density) { with(density) { 50.dp.toPx() } }

    val bringIntoViewSpec = remember(topScrollOffset) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                return offset - topScrollOffset
            }
        }
    }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val scraper = MovieScraper(context)
            try {
                val data = scraper.fetchMainPageRows()
                withContext(Dispatchers.Main) {
                    if (data.isNotEmpty()) {
                        rows = data
                        if (focusedMovie == null) focusedMovie = data.values.firstOrNull()?.firstOrNull()
                        isLoaded = true
                    }
                }
            } catch (_: SessionExpiredException) {
                withContext(Dispatchers.Main) {
                    Log.e("HomeScreen", "Session expired! Redirecting to login.")
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            } catch (e: Exception) { Log.e("HomeScreen", "Error", e) }
        }
    }

    LaunchedEffect(isLoaded) {
        if(isLoaded) {
            delay(200)
            try { firstItemRequester.requestFocus() } catch(_: Exception) {}
        }
    }

    if (rows == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Red)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.height(260.dp).fillMaxWidth()) {
                if (focusedMovie != null) MovieDescriptionBanner(movie = focusedMovie!!)
            }

            CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 220.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rows!!.entries.toList().size) { index ->
                        val entry = rows!!.entries.toList()[index]
                        val isFirstCategory = (index == 0)
                        CategoryRow(
                            title = entry.key,
                            movies = entry.value,
                            onMovieFocused = { focusedMovie = it },
                            focusRequester = if(isFirstCategory) firstItemRequester else null
                        )
                    }
                }
            }
        }
    }
}