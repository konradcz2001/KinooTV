package com.github.konradcz2001.kinootv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.github.konradcz2001.kinootv.ui.screens.EpisodeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Activity responsible for displaying content related to a specific TV series episode.
 * Supports handling new intents to refresh content without recreating the activity.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
class EpisodeActivity : ComponentActivity() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val episodeUrl = intent.getStringExtra("EPISODE_URL")
        if (episodeUrl == null) {
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
                EpisodeScreen(url = episodeUrl, scope = scope)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}