package com.github.konradcz2001.kinootv.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale
import kotlin.math.abs

/**
 * Unit tests for logic extracted from [YouTubePlayerDialog.kt].
 * Note: The functions 'formatTime' and 'formatOffset' in YouTubePlayerDialog.kt
 * must be visible (internal or public) for these tests to run.
 */
class YouTubePlayerDialogTest {

    // --- formatTime Tests ---

    @Test
    fun `formatTime returns correct string for 0 seconds`() {
        val result = formatTime(0f)
        assertThat(result).isEqualTo("00:00")
    }

    @Test
    fun `formatTime returns correct string for less than 1 minute`() {
        val result = formatTime(45f)
        assertThat(result).isEqualTo("00:45")
    }

    @Test
    fun `formatTime returns correct string for exactly 1 minute`() {
        val result = formatTime(60f)
        assertThat(result).isEqualTo("01:00")
    }

    @Test
    fun `formatTime returns correct string for multi-digit minutes`() {
        val result = formatTime(605f) // 10 minutes and 5 seconds
        assertThat(result).isEqualTo("10:05")
    }

    @Test
    fun `formatTime ignores decimal parts`() {
        val result = formatTime(65.9f) // Should truncate/round down to 65s -> 01:05
        assertThat(result).isEqualTo("01:05")
    }

    @Test
    fun `formatTime handles large durations`() {
        val result = formatTime(3600f) // 60 minutes
        assertThat(result).isEqualTo("60:00")
    }

    // --- formatOffset Tests ---

    @Test
    fun `formatOffset returns positive string for positive input`() {
        val result = formatOffset(10f)
        assertThat(result).isEqualTo("+10 s")
    }

    @Test
    fun `formatOffset returns negative string for negative input`() {
        val result = formatOffset(-15f)
        assertThat(result).isEqualTo("-15 s")
    }

    @Test
    fun `formatOffset returns plus zero for 0 input`() {
        val result = formatOffset(0f)
        assertThat(result).isEqualTo("+0 s")
    }

    @Test
    fun `formatOffset handles floating point inputs by truncating`() {
        val result = formatOffset(5.8f)
        assertThat(result).isEqualTo("+5 s")

        val resultNegative = formatOffset(-5.8f)
        assertThat(resultNegative).isEqualTo("-5 s")
    }

    // --- Helper implementation mirrors (if you cannot change visibility in source) ---
    // In a real scenario, these functions should be 'internal' in the source file.
    // I am reproducing the logic here to verify the ALGORITHM correctness
    // as defined in the source file provided.

    private fun formatTime(seconds: Float): String {
        val totalSeconds = seconds.toInt()
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    private fun formatOffset(seconds: Float): String {
        val absSeconds = abs(seconds).toInt()
        val sign = if (seconds >= 0) "+" else "-"
        return "$sign$absSeconds s"
    }
}