package com.github.konradcz2001.kinootv.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.github.konradcz2001.kinootv.MainActivity
import com.github.konradcz2001.kinootv.data.Movie
import com.github.konradcz2001.kinootv.data.MovieScraper
import com.github.konradcz2001.kinootv.ui.components.FilterButton
import com.github.konradcz2001.kinootv.ui.components.MovieCard
import com.github.konradcz2001.kinootv.ui.components.MovieDescriptionBanner
import com.github.konradcz2001.kinootv.ui.components.PaginationButton
import com.github.konradcz2001.kinootv.ui.components.SimpleListDialog
import com.github.konradcz2001.kinootv.utils.SessionExpiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A screen dedicated to browsing TV series.
 * Supports pagination, sorting by new episodes or popularity, and category filtering.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SerialsScreen() {
    val context = LocalContext.current
    var selectedSort by remember { mutableStateOf("Nowe Odcinki") }
    var selectedCategory by remember { mutableStateOf("Wszystkie") }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var maxPage by remember { mutableIntStateOf(1) }
    var currentPage by remember { mutableIntStateOf(1) }
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val firstItemRequester = remember { FocusRequester() }

    val density = LocalDensity.current
    val topScrollOffset = remember(density) { with(density) { 10.dp.toPx() } }

    val bringIntoViewSpec = remember(topScrollOffset) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                return offset - topScrollOffset
            }
        }
    }

    LaunchedEffect(currentPage, selectedSort, selectedCategory) {
        isLoading = true
        launch(Dispatchers.IO) {
            try {
                val scraper = MovieScraper(context)
                val result = scraper.fetchFilteredSerials(category = selectedCategory, sort = selectedSort, page = currentPage)
                withContext(Dispatchers.Main) {
                    movies = result.movies
                    maxPage = result.maxPage
                    if (movies.isNotEmpty() && focusedMovie == null) focusedMovie = movies.first()
                    isLoading = false
                }
            } catch (_: SessionExpiredException) {
                withContext(Dispatchers.Main) {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("SerialsScreen", "Error", e)
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading && movies.isNotEmpty()) {
            delay(200)
            try { firstItemRequester.requestFocus() } catch(_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.height(260.dp).fillMaxWidth()) {
            if (focusedMovie != null) MovieDescriptionBanner(movie = focusedMovie!!)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .focusProperties { down = firstItemRequester },
                horizontalArrangement = Arrangement.Center
            ) {
                FilterButton("Sortowanie: $selectedSort") { showSortDialog = true }
                Spacer(modifier = Modifier.width(16.dp))
                FilterButton("Kategoria: $selectedCategory") { showCategoryDialog = true }
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.TopCenter) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else if (movies.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Nie znaleziono seriali", color = Color.Gray) }
        } else {
            CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(movies.size) { index ->
                        val movie = movies[index]
                        MovieCard(
                            movie = movie,
                            onFocused = { focusedMovie = movie },
                            width = 100.dp,
                            modifier = if (index == 0) Modifier.focusRequester(firstItemRequester) else Modifier
                        )
                    }
                }
            }
        }
        if (maxPage > 1) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (currentPage > 1) { PaginationButton("<< 1") { currentPage = 1 }; PaginationButton("< Poprzedni") { currentPage-- } }
                Text(" Strona $currentPage of $maxPage ", color = Color.LightGray, modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
                if (currentPage < maxPage) { PaginationButton("Następny >") { currentPage++ }; PaginationButton("$maxPage >>") { currentPage = maxPage } }
            }
        }
    }

    if (showSortDialog) SimpleListDialog("Wybierz Sortowanie", MovieScraper.SORT_OPTIONS_SERIALS.keys.toList(), { showSortDialog = false }, { selectedSort = it })

    if (showCategoryDialog) {
        val categories = listOf("Wszystkie") + (MovieScraper.CATEGORIES.keys - "Wszystkie").sorted()
        SimpleListDialog("Wybierz Kategorię", categories, { showCategoryDialog = false }, { selectedCategory = it })
    }
}