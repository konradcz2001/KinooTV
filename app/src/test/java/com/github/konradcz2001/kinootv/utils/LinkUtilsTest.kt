package com.github.konradcz2001.kinootv.utils

import android.content.Context
import android.content.res.Resources
import com.github.konradcz2001.kinootv.R
import com.github.konradcz2001.kinootv.data.PlayerLink
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Unit tests for [LinkUtils.kt].
 * Verifies the heuristics for sorting player links based on language version, quality, and host priority.
 * Also verifies date parsing and localization logic.
 */
class LinkUtilsTest {

    // --- Version Score Tests ---

    @Test
    fun `getVersionScore returns 0 for PL`() {
        val score = getVersionScore("PL")
        assertThat(score).isEqualTo(0)
    }

    @Test
    fun `getVersionScore prioritizes standard Dubbing over Lektor`() {
        val dubbingScore = getVersionScore("Dubbing")
        val lektorScore = getVersionScore("Lektor")

        // Dubbing (1) should be better (lower) than Lektor (2)
        assertThat(dubbingScore).isLessThan(lektorScore)
    }

    @Test
    fun `getVersionScore handles case insensitivity`() {
        val scoreLower = getVersionScore("dubbing")
        val scoreUpper = getVersionScore("DUBBING")

        assertThat(scoreLower).isEqualTo(1)
        assertThat(scoreUpper).isEqualTo(1)
    }

    @Test
    fun `getVersionScore penalizes AI and IVO Lectors`() {
        val standardLector = getVersionScore("Lektor")
        val aiLector = getVersionScore("Lektor_AI")
        val ivoLector = getVersionScore("Lektor_IVO")

        // Standard Lektor (2) < AI (4) < IVO (5)
        assertThat(standardLector).isLessThan(aiLector)
        assertThat(aiLector).isLessThan(ivoLector)
    }

    @Test
    fun `getVersionScore returns high score for unknown versions`() {
        val score = getVersionScore("UnknownVersion")
        assertThat(score).isEqualTo(100)
    }

    // --- Quality Score Tests ---

    @Test
    fun `getQualityScore prioritizes 4k and 2160p`() {
        val score4k = getQualityScore("4k")
        val score2160 = getQualityScore("2160p")

        assertThat(score4k).isEqualTo(4)
        assertThat(score2160).isEqualTo(0)
    }

    @Test
    fun `getQualityScore correctly ranks standard resolutions`() {
        val fhd = getQualityScore("1080p")
        val hd = getQualityScore("720p")
        val sd = getQualityScore("480p")
        val unknown = getQualityScore("cam")

        assertThat(fhd).isGreaterThan(hd)
        assertThat(hd).isGreaterThan(sd)
        assertThat(sd).isGreaterThan(unknown)
    }

    // --- Sorting Logic Tests ---

    @Test
    fun `sortLinks sorts primarily by Version Priority (lower is better)`() {
        // Arrange
        val linkBest = createLink(version = "PL", quality = "720p") // Score 0
        val linkMid = createLink(version = "Lektor", quality = "1080p") // Score 2 (even if quality is higher, version rules)
        val linkWorst = createLink(version = "Napisy", quality = "4k") // Score 6

        val unsortedList = listOf(linkWorst, linkBest, linkMid)

        // Act
        val sortedList = sortLinks(unsortedList)

        // Assert
        assertThat(sortedList).containsExactly(linkBest, linkMid, linkWorst).inOrder()
    }

    @Test
    fun `sortLinks sorts secondarily by Quality (higher is better)`() {
        // Arrange: Same version "PL", different qualities
        val q1080 = createLink(version = "PL", quality = "1080p")
        val q720 = createLink(version = "PL", quality = "720p")
        val q4k = createLink(version = "PL", quality = "4k")

        val unsortedList = listOf(q720, q4k, q1080)

        // Act
        val sortedList = sortLinks(unsortedList)

        // Assert: Expect 4k -> 1080p -> 720p
        assertThat(sortedList).containsExactly(q4k, q1080, q720).inOrder()
    }

    @Test
    fun `sortLinks sorts tertiarily by Host preference`() {
        // Arrange: Same version, Same quality
        val linkVoe = createLink(host = "voe.sx", version = "Lektor", quality = "720p")
        val linkOther = createLink(host = "mixdrop", version = "Lektor", quality = "720p")

        val unsortedList = listOf(linkOther, linkVoe)

        // Act
        val sortedList = sortLinks(unsortedList)

        // Assert
        assertThat(sortedList).containsExactly(linkVoe, linkOther).inOrder()
    }

    @Test
    fun `sortLinks handles complex mixed list correctly`() {
        // Arrange
        val l1 = createLink(host = "other", version = "Napisy", quality = "1080p") // Priority: Low (Ver: 6)
        val l2 = createLink(host = "voe", version = "PL", quality = "720p")      // Priority: High (Ver: 0, Qual: 2, Host: Voe)
        val l3 = createLink(host = "other", version = "PL", quality = "1080p")   // Priority: High (Ver: 0, Qual: 3) -> Winner

        val unsorted = listOf(l1, l2, l3)

        // Act
        val sorted = sortLinks(unsorted)

        // Assert
        assertThat(sorted).containsExactly(l3, l2, l1).inOrder()
    }

    // --- Date Parsing Tests ---

    @Test
    fun `parseAndLocalizeDate parses implicit singular units correctly (default to 1)`() {
        // Given
        val rawDate = "rok temu"
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)

        every { context.resources } returns resources
        every { resources.getQuantityString(R.plurals.time_years_ago, 1, 1) } returns "1 rok temu"

        // When
        val result = parseAndLocalizeDate(rawDate, context)

        // Then
        verify { resources.getQuantityString(R.plurals.time_years_ago, 1, 1) }
        assertThat(result).isEqualTo("1 rok temu")
    }

    @Test
    fun `parseAndLocalizeDate parses explicit numbers correctly`() {
        // Given
        val rawDate = "5 minut temu"
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)

        every { context.resources } returns resources
        every { resources.getQuantityString(R.plurals.time_minutes_ago, 5, 5) } returns "5 minut temu"

        // When
        val result = parseAndLocalizeDate(rawDate, context)

        // Then
        verify { resources.getQuantityString(R.plurals.time_minutes_ago, 5, 5) }
        assertThat(result).isEqualTo("5 minut temu")
    }

    @Test
    fun `parseAndLocalizeDate handles keywords like wczoraj`() {
        // Given
        val rawDate = "dodano wczoraj"
        val context = mockk<Context>(relaxed = true)

        every { context.getString(R.string.time_yesterday) } returns "Wczoraj"

        // When
        val result = parseAndLocalizeDate(rawDate, context)

        // Then
        verify { context.getString(R.string.time_yesterday) }
        assertThat(result).isEqualTo("Wczoraj")
    }

    @Test
    fun `parseAndLocalizeDate handles unknown units by returning original string`() {
        // Given
        val rawDate = "5 eonów temu"
        val context = mockk<Context>(relaxed = true)

        // When
        val result = parseAndLocalizeDate(rawDate, context)

        // Then
        assertThat(result).isEqualTo("5 eonów temu")
    }

    // --- Helper Methods ---

    private fun createLink(
        host: String = "host",
        iframe: String = "http://url",
        quality: String = "720p",
        version: String = "Lektor",
        date: String = "today"
    ): PlayerLink {
        return PlayerLink(host, iframe, quality, version, date)
    }
}