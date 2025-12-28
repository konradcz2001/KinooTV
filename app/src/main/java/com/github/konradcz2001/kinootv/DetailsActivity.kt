package com.github.konradcz2001.kinootv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.github.konradcz2001.kinootv.ui.screens.DetailsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Activity responsible for displaying detailed information about a specific movie or series.
 * Initializes the Compose UI and handles the data passed via Intent.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
class DetailsActivity : ComponentActivity() {

    private var moviePageUrl: String? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moviePageUrl = intent.getStringExtra("MOVIE_PAGE_URL")

        if (moviePageUrl == null) {
            finish()
            return
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    surface = Color(0xFF121212),
                    onSurface = Color.White,
                    primary = Color(0xFFE50914)
                )
            ) {
                DetailsScreen(pageUrl = moviePageUrl!!, scope = scope)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}