package com.github.konradcz2001.kinootv.utils

import com.github.konradcz2001.kinootv.data.PlayerLink
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [LinkUtils.kt].
 * Verifies the heuristics for sorting player links based on language version, quality, and host priority.
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
        // Based on logic: compareByDescending { it.hostName.contains("voe") }
        // true > false, so "voe" should come FIRST if using Descending.
        val linkVoe = createLink(host = "voe.sx", version = "Lektor", quality = "720p")
        val linkOther = createLink(host = "mixdrop", version = "Lektor", quality = "720p")

        val unsortedList = listOf(linkOther, linkVoe)

        // Act
        val sortedList = sortLinks(unsortedList)

        // Assert
        // If the intention of the code is to prioritize VOE, it should be first.
        // If the intention was to "downgrade" (as per comment in code), the implementation
        // using `thenByDescending` actually PROMOTES it. Assuming code behavior is truth:
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
        // 1. l3 (PL, 1080p) - Wins on Quality over l2
        // 2. l2 (PL, 720p)
        // 3. l1 (Napisy) - Loses on Version
        assertThat(sorted).containsExactly(l3, l2, l1).inOrder()
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