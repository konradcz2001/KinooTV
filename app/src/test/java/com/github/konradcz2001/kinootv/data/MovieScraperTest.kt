package com.github.konradcz2001.kinootv.data

import android.content.Context
import android.content.SharedPreferences
import com.github.konradcz2001.kinootv.utils.AppConstants
import com.github.konradcz2001.kinootv.utils.SessionExpiredException
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MovieScraper].
 * Uses MockK to mock dependencies (Context, OkHttp) and Reflection to inject the mock client.
 * This allows testing the HTML parsing logic without real network calls.
 */
class MovieScraperTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var mockClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var scraper: MovieScraper

    @Before
    fun setUp() {
        // 1. Mock Android Context and SharedPreferences
        context = mockk()
        prefs = mockk()

        every { context.getSharedPreferences(any(), any()) } returns prefs
        // Default behavior: valid cookie and user agent exists
        every { prefs.getString(AppConstants.COOKIE_KEY, any()) } returns "valid_cookie"
        every { prefs.getString(AppConstants.USER_AGENT_KEY, any()) } returns "TestAgent"

        // 2. Mock OkHttp Client and Call
        mockClient = mockk()
        mockCall = mockk()

        // When client.newCall is called, return our mockCall
        every { mockClient.newCall(any()) } returns mockCall

        // 3. Initialize Scraper
        scraper = MovieScraper(context)

        // 4. USE REFLECTION to replace the private 'client' field in MovieScraper
        // This is the trick that allows us to unit test the class without refactoring it.
        val clientField = MovieScraper::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        clientField.set(scraper, mockClient)
    }

    @Test
    fun `fetchKidsMovies parses HTML correctly into Movie objects`() {
        // Arrange - Create a fake HTML response that mimics the real website
        val fakeHtml = """
            <html>
            <body>
                <div>Zalogowany jako User</div> <div id="item-list">
                    <div class="item">
                        <a class="textOverImage" href="https://filman.cc/film/shrek" data-title="Shrek" data-text="Opis Shreka">
                           </a>
                        <img src="shrek.jpg" data-src="shrek_real.jpg" />
                        <div class="film_year">2001</div>
                        <div class="quality-version">1080p Lektor</div>
                        <div class="view">1000</div>
                        <div class="rate">8.5</div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        // Mock the network response
        mockResponse(fakeHtml)

        // Act
        val result = scraper.fetchKidsMovies(page = 1)

        // Assert
        assertThat(result.movies).hasSize(1)
        val movie = result.movies.first()

        assertThat(movie.title).isEqualTo("Shrek")
        assertThat(movie.year).isEqualTo("2001")
        assertThat(movie.imageUrl).isEqualTo("shrek_real.jpg") // Should verify data-src priority
        assertThat(movie.rating).isEqualTo("8.5")
    }

    @Test(expected = SessionExpiredException::class)
    fun `fetchKidsMovies throws SessionExpiredException when login indicator is missing`() {
        // Arrange - HTML WITHOUT "Zalogowany jako"
        val loggedOutHtml = """
            <html>
            <body>
                <div id="login-form">Please login</div>
            </body>
            </html>
        """.trimIndent()

        // Mock methods needed for clearing session
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.edit() } returns editor

        mockResponse(loggedOutHtml)

        // Act
        scraper.fetchKidsMovies(1)

        // Assert - Expects Exception defined in @Test annotation
    }

    @Test
    fun `fetchMovieDetails parses details and player links correctly`() {
        // Arrange
        val detailHtml = """
            <html>
            <body>
                <div>Zalogowany jako User</div>
                <h1 class="film_title"><span itemprop="title">Matrix</span></h1>
                <p class="description">W świecie matrixa...</p>
                <div id="single-poster"><img src="poster.jpg"/></div>
                
                <table id="links">
                    <tbody>
                        <tr>
                            <td>
                                <a data-iframe="base64code" href="#">voe.sx dodane 1 godz temu</a>
                            </td>
                            <td>Lektor</td>
                            <td>1080p</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()

        mockResponse(detailHtml)

        // Act
        val details = scraper.fetchMovieDetails("https://fake.url/matrix")

        // Assert
        assertThat(details).isNotNull()
        assertThat(details?.title).isEqualTo("Matrix")
        assertThat(details?.description).isEqualTo("W świecie matrixa...")

        assertThat(details?.playerLinks).hasSize(1)
        val link = details!!.playerLinks.first()
        assertThat(link.hostName).isEqualTo("voe.sx")
        assertThat(link.version).isEqualTo("Lektor")
        assertThat(link.quality).isEqualTo("1080p")
    }

    @Test
    fun `fetchFilteredSerials constructs correct URL parameters`() {
        // This test verifies if the scraper builds the URL correctly based on arguments.
        // We capture the Request sent to OkHttp using 'slot'.

        // Arrange
        val emptyHtml = "<html><body>Zalogowany jako User</body></html>"
        mockResponse(emptyHtml)

        // FIXED: Using standard MockK 'slot'
        val slot = slot<Request>()
        every { mockClient.newCall(capture(slot)) } returns mockCall

        // Act
        scraper.fetchFilteredSerials(category = "Akcja", sort = "Odsłony", page = 2)

        // Assert
        val capturedRequest = slot.captured
        val url = capturedRequest.url.toString()

        // Expected URL structure based on code:
        // /seriale/sort:view/category:1?page=2 (Akcja = ID 1, Odsłony = view)
        assertThat(url).contains("/seriale/")
        assertThat(url).contains("sort:view")
        assertThat(url).contains("category:1") // From Companion object mapping
        assertThat(url).contains("page=2")
    }

    // Helper to setup the mock response
    private fun mockResponse(bodyString: String) {
        val response = Response.Builder()
            .request(Request.Builder().url("https://mock.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(bodyString.toResponseBody(null))
            .build()

        every { mockCall.execute() } returns response
    }
}