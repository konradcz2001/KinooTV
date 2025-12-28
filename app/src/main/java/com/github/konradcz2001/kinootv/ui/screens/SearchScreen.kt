package com.github.konradcz2001.kinootv.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.konradcz2001.kinootv.MainActivity
import com.github.konradcz2001.kinootv.data.Movie
import com.github.konradcz2001.kinootv.data.MovieScraper
import com.github.konradcz2001.kinootv.data.SearchResult
import com.github.konradcz2001.kinootv.ui.components.CategoryRow
import com.github.konradcz2001.kinootv.ui.components.MovieDescriptionBanner
import com.github.konradcz2001.kinootv.utils.SessionExpiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provides search functionality for the application.
 * Supports both on-screen keyboard text input and system voice search integration.
 * Displays results for both Movies and TV Series.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen() {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<SearchResult?>(null) }
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val textFieldRequester = remember { FocusRequester() }
    val micRequester = remember { FocusRequester() }
    val firstResultRequester = remember { FocusRequester() }

    val density = LocalDensity.current
    val topScrollOffset = remember(density) { with(density) { 50.dp.toPx() } }

    val bringIntoViewSpec = remember(topScrollOffset) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                return offset - topScrollOffset
            }
        }
    }

    val performSearch = { textToSearch: String ->
        if (textToSearch.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    val scraper = MovieScraper(context)
                    val result = scraper.searchMovies(textToSearch)
                    withContext(Dispatchers.Main) {
                        searchResult = result
                        focusedMovie = result.movies.firstOrNull() ?: result.serials.firstOrNull()
                    }
                } catch (_: SessionExpiredException) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                } catch (e: Exception) { Log.e("Search", "Error", e) }
            }
            keyboardController?.hide()
        }
    }

    // Launcher for Android Voice Search Intent
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                query = spokenText
                performSearch(spokenText)
                scope.launch {
                    delay(800)
                    try { firstResultRequester.requestFocus() } catch (_: Exception) {}
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        try {
            textFieldRequester.requestFocus()
            keyboardController?.show()
        } catch(_: Exception) { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.height(260.dp).fillMaxWidth()) {
            if (focusedMovie != null) MovieDescriptionBanner(movie = focusedMovie!!)
            else Box(modifier = Modifier.fillMaxSize().background(Color.Black))

            Row(modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp, start = 48.dp, end = 48.dp)
                .fillMaxWidth()
                .background(Color(0xCC1A1A1A), shape = MaterialTheme.shapes.extraSmall)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusProperties { down = firstResultRequester },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))

                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .focusRequester(textFieldRequester)
                        .focusable()
                        .onPreviewKeyEvent {
                            if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                try {
                                    firstResultRequester.requestFocus()
                                    return@onPreviewKeyEvent true
                                } catch (_: Exception) {}
                            }
                            false
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch(query) }),
                    singleLine = true,
                    decorationBox = { innerTextField -> if (query.isEmpty()) Text("Wpisz tytuł...", color = Color.Gray, fontSize = 16.sp); innerTextField() }
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
                            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayOf("pl-PL", "en-US"))
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Powiedz tytuł...")
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Wyszukiwanie głosowe niedostępne", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.focusRequester(micRequester)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Wyszukiwanie głosowe", tint = Color.LightGray)
                }
            }
        }

        if (searchResult != null) {
            val noResults = searchResult!!.movies.isEmpty() && searchResult!!.serials.isEmpty()
            if (noResults) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak rezultatów", color = Color.Gray)
                }
            } else {
                CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 0.dp, bottom = 220.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val moviesExist = searchResult!!.movies.isNotEmpty()
                        if (moviesExist) {
                            item(key = "movies_row") {
                                CategoryRow(
                                    title = "FILMY",
                                    movies = searchResult!!.movies,
                                    onMovieFocused = { focusedMovie = it },
                                    focusRequester = firstResultRequester
                                )
                            }
                        }

                        if (searchResult!!.serials.isNotEmpty()) {
                            item(key = "serials_row") {
                                CategoryRow(
                                    title = "SERIALE",
                                    movies = searchResult!!.serials,
                                    onMovieFocused = { focusedMovie = it },
                                    focusRequester = if (!moviesExist) firstResultRequester else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}