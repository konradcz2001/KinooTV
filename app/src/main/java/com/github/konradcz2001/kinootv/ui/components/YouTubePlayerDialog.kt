package com.github.konradcz2001.kinootv.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * A composable Dialog that overlays a YouTube player tailored for Android TV D-pad navigation.
 * Handles custom key events for seeking, pausing, and displaying OSD (On Screen Display).
 *
 * @param videoId The YouTube video identifier to load.
 * @param onDismiss Callback triggered when the dialog is closed (e.g., via Back button).
 */
@Composable
fun YouTubePlayerDialog(
    videoId: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var youTubePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }

        // Player State tracking
        var playerState by remember { mutableStateOf(PlayerConstants.PlayerState.UNKNOWN) }

        // Real-time tracking vs Total duration
        var currentVideoTime by remember { mutableFloatStateOf(0f) }
        var videoDuration by remember { mutableFloatStateOf(0f) }

        // Determine if video is logically playing (including buffering)
        val isVideoPlaying = playerState == PlayerConstants.PlayerState.PLAYING || playerState == PlayerConstants.PlayerState.BUFFERING

        // --- SEEKING LOGIC VARIABLES ---
        var isSeeking by remember { mutableStateOf(false) }
        var baseSeekTime by remember { mutableFloatStateOf(0f) }
        var seekOffset by remember { mutableFloatStateOf(0f) }

        val seekJob = remember { mutableStateOf<Job?>(null) }
        val coroutineScope = rememberCoroutineScope()

        // --- PROGRESS BAR VISIBILITY LOGIC ---
        var isInfoVisible by remember { mutableStateOf(false) }
        val infoJob = remember { mutableStateOf<Job?>(null) }

        // Debounce for key presses
        var lastSeekPressTime by remember { mutableLongStateOf(0L) }

        // Focus management for the player window
        val playerFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            playerFocusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(playerFocusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.nativeKeyEvent.keyCode) {
                            // Play/Pause Toggles
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (playerState == PlayerConstants.PlayerState.PLAYING) {
                                    youTubePlayer?.pause()
                                } else {
                                    youTubePlayer?.play()
                                }
                                return@onKeyEvent true
                            }

                            // FORWARD SEEKING
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                val currentTime = System.currentTimeMillis()
                                // Throttle rapid inputs
                                if (isSeeking && currentTime - lastSeekPressTime < 200) {
                                    return@onKeyEvent true
                                }
                                lastSeekPressTime = currentTime

                                if (videoDuration > 0) {
                                    if (!isSeeking) {
                                        baseSeekTime = if (playerState == PlayerConstants.PlayerState.ENDED) videoDuration else currentVideoTime
                                        seekOffset = 0f
                                        isSeeking = true
                                    }

                                    // Increment seek by 5 seconds
                                    val potentialNewTime = baseSeekTime + seekOffset + 5f
                                    val clampedNewTime = potentialNewTime.coerceIn(0f, videoDuration)

                                    seekOffset = clampedNewTime - baseSeekTime

                                    // Debounce the actual seek command to avoid player stutter
                                    seekJob.value?.cancel()
                                    seekJob.value = coroutineScope.launch {
                                        delay(800)
                                        youTubePlayer?.seekTo(clampedNewTime)
                                        // Optimistic UI update
                                        currentVideoTime = clampedNewTime
                                        delay(1000)
                                        isSeeking = false
                                    }
                                }
                                return@onKeyEvent true
                            }

                            // REWIND SEEKING
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                val currentTime = System.currentTimeMillis()
                                if (isSeeking && currentTime - lastSeekPressTime < 200) {
                                    return@onKeyEvent true
                                }
                                lastSeekPressTime = currentTime

                                if (videoDuration > 0) {
                                    if (!isSeeking) {
                                        baseSeekTime = if (playerState == PlayerConstants.PlayerState.ENDED) videoDuration else currentVideoTime
                                        seekOffset = 0f
                                        isSeeking = true
                                    }

                                    // Decrement seek by 5 seconds
                                    val potentialNewTime = baseSeekTime + seekOffset - 5f
                                    val clampedNewTime = potentialNewTime.coerceIn(0f, videoDuration)

                                    seekOffset = clampedNewTime - baseSeekTime

                                    seekJob.value?.cancel()
                                    seekJob.value = coroutineScope.launch {
                                        delay(800)
                                        youTubePlayer?.seekTo(clampedNewTime)
                                        // Optimistic UI update
                                        currentVideoTime = clampedNewTime
                                        delay(1000)
                                        isSeeking = false
                                    }
                                }
                                return@onKeyEvent true
                            }

                            // Show info overlay on Up/Down
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                isInfoVisible = true
                                infoJob.value?.cancel()
                                infoJob.value = coroutineScope.launch {
                                    delay(3000)
                                    isInfoVisible = false
                                }
                                return@onKeyEvent true
                            }

                            KeyEvent.KEYCODE_BACK -> {
                                onDismiss()
                                return@onKeyEvent true
                            }
                        }
                    }
                    false
                },
            contentAlignment = Alignment.Center
        ) {
            // Player Container
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            (ctx as? LifecycleOwner)?.lifecycle?.addObserver(this)
                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(player: YouTubePlayer) {
                                    youTubePlayer = player
                                    player.loadVideo(videoId, 0f)
                                }

                                override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                                    if (!isSeeking && playerState != PlayerConstants.PlayerState.ENDED) {
                                        currentVideoTime = second
                                    }
                                }

                                override fun onVideoDuration(player: YouTubePlayer, duration: Float) {
                                    videoDuration = duration
                                }

                                override fun onStateChange(
                                    player: YouTubePlayer,
                                    state: PlayerConstants.PlayerState
                                ) {
                                    playerState = state
                                    if (state == PlayerConstants.PlayerState.ENDED && videoDuration > 0) {
                                        currentVideoTime = videoDuration
                                    }
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- OVERLAY: SEEK ICON AND OFFSET ---
            if (isSeeking) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (seekOffset > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatOffset(seekOffset),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- OVERLAY: PLAY/PAUSE ICON ---
            if (!isVideoPlaying && !isSeeking && videoDuration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Odtwórz",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // --- OVERLAY: BOTTOM PROGRESS BAR ---
            if ((isInfoVisible || isSeeking || !isVideoPlaying) && videoDuration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(24.dp)
                ) {
                    Column {
                        val targetTimeAbs = if (isSeeking) {
                            (baseSeekTime + seekOffset).coerceIn(0f, videoDuration)
                        } else {
                            currentVideoTime
                        }

                        val progress = if (videoDuration > 0) {
                            (targetTimeAbs / videoDuration).coerceIn(0f, 1f)
                        } else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(targetTimeAbs),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = formatTime(videoDuration),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.Gray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(Color(0xFFE50914))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats seconds into MM:SS string.
 */
private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

/**
 * Formats seek offset into +Ns or -Ns string.
 */
private fun formatOffset(seconds: Float): String {
    val absSeconds = abs(seconds).toInt()
    val sign = if (seconds >= 0) "+" else "-"
    return "$sign$absSeconds s"
}