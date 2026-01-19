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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.github.konradcz2001.kinootv.MainActivity
import com.github.konradcz2001.kinootv.R
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

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SerialsScreen() {
    val context = LocalContext.current

    // Internal keys (used for scraper logic)
    var selectedSortKey by remember { mutableStateOf("Nowe Odcinki") }
    var selectedCategoryKey by remember { mutableStateOf("Wszystkie") }

    // --- MAPPING LOGIC ---
    val sortMap = mapOf(
        "Nowe Odcinki" to stringResource(R.string.sort_new_episodes),
        "Nowe Seriale" to stringResource(R.string.sort_new_serials),
        "Liczba Głosów" to stringResource(R.string.sort_votes),
        "Odsłony" to stringResource(R.string.sort_views),
        "Ocena" to stringResource(R.string.sort_rate)
    )

    val categoryMap = mapOf(
        "Wszystkie" to stringResource(R.string.filter_all),
        "Akcja" to stringResource(R.string.cat_action),
        "Animacja" to stringResource(R.string.cat_animation),
        "Czarna Komedia" to stringResource(R.string.cat_black_comedy),
        "Dla Młodzieży" to stringResource(R.string.cat_youth),
        "Erotyczny" to stringResource(R.string.cat_erotic),
        "Familijny" to stringResource(R.string.cat_family),
        "Horror" to stringResource(R.string.cat_horror),
        "Komedia" to stringResource(R.string.cat_comedy),
        "Sci-Fi" to stringResource(R.string.cat_scifi),
        "Thriller" to stringResource(R.string.cat_thriller)
    )

    // Helper to get display name from key
    val getSortDisplay = { key: String -> sortMap[key] ?: key }
    val getCategoryDisplay = { key: String -> categoryMap[key] ?: key }

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

    LaunchedEffect(currentPage, selectedSortKey, selectedCategoryKey) {
        isLoading = true
        launch(Dispatchers.IO) {
            try {
                val scraper = MovieScraper(context)
                val result = scraper.fetchFilteredSerials(category = selectedCategoryKey, sort = selectedSortKey, page = currentPage)
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
                FilterButton(stringResource(R.string.sort_label_format, getSortDisplay(selectedSortKey))) { showSortDialog = true }
                Spacer(modifier = Modifier.width(16.dp))
                FilterButton(stringResource(R.string.category_label_format, getCategoryDisplay(selectedCategoryKey))) { showCategoryDialog = true }
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.TopCenter) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else if (movies.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_serials_found), color = Color.Gray) }
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
                if (currentPage > 1) { PaginationButton(stringResource(R.string.pagination_first)) { currentPage = 1 }; PaginationButton(stringResource(R.string.pagination_prev)) { currentPage-- } }
                Text(stringResource(R.string.pagination_page_format, currentPage, maxPage), color = Color.LightGray, modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
                if (currentPage < maxPage) { PaginationButton(stringResource(R.string.pagination_next)) { currentPage++ }; PaginationButton(stringResource(R.string.pagination_last, maxPage)) { currentPage = maxPage } }
            }
        }
    }

    if (showSortDialog) {
        val keys = MovieScraper.SORT_OPTIONS_SERIALS.keys.toList()
        val displayValues = keys.map { getSortDisplay(it) }
        SimpleListDialog(stringResource(R.string.dialog_title_sort), displayValues, { showSortDialog = false }, { selectedDisplay ->
            val index = displayValues.indexOf(selectedDisplay)
            if(index != -1) selectedSortKey = keys[index]
        })
    }

    if (showCategoryDialog) {
        val allKeys = listOf("Wszystkie") + (MovieScraper.CATEGORIES.keys - "Wszystkie").sorted()
        val displayValues = allKeys.map { getCategoryDisplay(it) }
        SimpleListDialog(stringResource(R.string.dialog_title_category), displayValues, { showCategoryDialog = false }, { selectedDisplay ->
            val index = displayValues.indexOf(selectedDisplay)
            if(index != -1) selectedCategoryKey = allKeys[index]
        })
    }
}