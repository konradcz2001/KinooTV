package com.github.konradcz2001.kinootv.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.github.konradcz2001.kinootv.utils.AppConstants
import com.github.konradcz2001.kinootv.utils.SessionExpiredException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * Data class representing the result of a paginated movie fetch operation.
 *
 * @property movies List of [Movie] objects found on the page.
 * @property maxPage The maximum number of pages available for the current filter settings.
 */
data class FilteredResult(
    val movies: List<Movie>,
    val maxPage: Int
)

/**
 * Core service class responsible for scraping data from the target website (filman.cc).
 * Handles HTML parsing, HTTP requests, session management, and data extraction for movies, serials, and episodes.
 *
 * @param context The application context, used for accessing SharedPreferences (cookies/User-Agent).
 */
class MovieScraper(private val context: Context) {

    private val client = OkHttpClient()

    // Headers identifying sections on the main page
    private val ROW_TITLES = listOf("FILMY NA CZASIE", "FILMY NA TOPIE", "SERIALE NA CZASIE")

    companion object {
        /**
         * Mapping of category names to their internal IDs used in URL parameters.
         */
        val CATEGORIES = mapOf(
            "Wszystkie" to "",
            "Akcja" to "1", "Animacja" to "2", "Czarna Komedia" to "95",
            "Dla Młodzieży" to "90", "Erotyczny" to "72", "Familijny" to "22",
            "Horror" to "29", "Komedia" to "32", "Sci-Fi" to "57", "Thriller" to "63"
        )

        /**
         * Sort options for movies mapping UI labels to URL parameters.
         */
        val SORT_OPTIONS = mapOf(
            "Nowe Linki" to "link",
            "Liczba Głosów" to "vote",
            "Premiera" to "premiere",
            "Odsłony" to "view",
            "Ocena" to "rate",
            "Ocena Filmweb" to "filmweb"
        )

        /**
         * Sort options specific to TV serials.
         */
        val SORT_OPTIONS_SERIALS = mapOf(
            "Nowe Odcinki" to "newepisode",
            "Nowe Seriale" to "date",
            "Liczba Głosów" to "vote",
            "Odsłony" to "view",
            "Ocena" to "rate"
        )
    }

    /**
     * Verifies if the current session is valid by checking for specific login indicators in the HTML.
     *
     * @param doc The Jsoup Document of the fetched page.
     * @throws SessionExpiredException If the "Logged in as" indicator is missing.
     */
    @Throws(SessionExpiredException::class)
    private fun checkSession(doc: Document) {
        val pageText = doc.text()
        // Checks for the presence of "Zalogowany jako" (Logged in as)
        val isLoggedIn = pageText.contains("Zalogowany jako", ignoreCase = true)

        if (!isLoggedIn) {
            Log.e("MovieScraper", "Session expired - missing login indicator. Clearing cookies.")
            context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
            throw SessionExpiredException()
        }
    }

    /**
     * Searches YouTube for a trailer ID based on a query string (usually Title + Year).
     *
     * @param query The search query.
     * @return The YouTube video ID string, or null if not found.
     */
    fun getYouTubeTrailerId(query: String): String? {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                // Use a desktop User-Agent to ensure standard HTML response
                .header("User-Agent", AppConstants.DEFAULT_USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            val html = response.body.string()
            // Regex to extract the first videoId from the JSON data inside HTML
            val pattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")
            val matcher = pattern.matcher(html)

            if (matcher.find()) {
                return matcher.group(1)
            }
        } catch (e: Exception) {
            Log.e("MovieScraper", "Error searching for trailer", e)
        }
        return null
    }

    /**
     * Fetches movies from the "Kids" section (cartoons).
     *
     * @param page The page number to fetch.
     * @return [FilteredResult] containing the list of movies and max pages.
     */
    @Throws(SessionExpiredException::class)
    fun fetchKidsMovies(page: Int = 1): FilteredResult {
        var url = "https://filman.cc/dla-dzieci-pl/"
        if (page > 1) {
            url += "?page=$page"
        }
        return executeScraping(url, page)
    }

    /**
     * Fetches TV serials based on category and sort order.
     *
     * @param category The category name (key from [CATEGORIES]).
     * @param sort The sort option (key from [SORT_OPTIONS_SERIALS]).
     * @param page The page number.
     */
    @Throws(SessionExpiredException::class)
    fun fetchFilteredSerials(category: String = "Wszystkie", sort: String = "Nowe Odcinki", page: Int = 1): FilteredResult {
        val baseUrl = "https://filman.cc/seriale"
        val parts = mutableListOf<String>()

        val sortId = SORT_OPTIONS_SERIALS[sort] ?: "newepisode"
        parts.add("sort:$sortId")

        val catId = CATEGORIES[category] ?: ""
        if (catId.isNotEmpty()) {
            parts.add("category:$catId")
        }

        var finalUrl = baseUrl
        if (parts.isNotEmpty()) finalUrl += "/" + parts.joinToString("/")
        if (page > 1) finalUrl += "?page=$page"

        return executeScraping(finalUrl, page)
    }

    /**
     * Fetches movies with advanced filtering (category, sort, year range).
     */
    @Throws(SessionExpiredException::class)
    fun fetchFilteredMovies(category: String = "Wszystkie", sort: String = "Nowe Linki", yearFrom: Int? = null, yearTo: Int? = null, page: Int = 1): FilteredResult {
        val baseUrl = "https://filman.cc/filmy"
        val parts = mutableListOf<String>()

        val catId = CATEGORIES[category] ?: ""
        if (catId.isNotEmpty()) {
            parts.add("category:$catId")
        }

        if (yearFrom != null && yearTo != null) {
            val start = minOf(yearFrom, yearTo)
            val end = maxOf(yearFrom, yearTo)
            val yearsString = (start..end).sortedDescending().joinToString(",")
            parts.add("year:$yearsString")
        }

        val sortId = SORT_OPTIONS[sort] ?: "link"
        parts.add("sort:$sortId")

        var finalUrl = baseUrl
        if (parts.isNotEmpty()) finalUrl += "/" + parts.joinToString("/") + "/"
        if (page > 1) finalUrl += "?page=$page"

        return executeScraping(finalUrl, page)
    }

    /**
     * Helper method to execute the HTTP request and parse the standard list layout.
     */
    private fun executeScraping(url: String, page: Int): FilteredResult {
        val (request, _) = createRequest(url)
        try {
            val response = client.newCall(request).execute()
            val html = response.body.string()
            val doc = Jsoup.parse(html)
            checkSession(doc)
            val itemList = doc.select("#item-list").first()
            val movies = if (itemList != null) parseMovies(itemList) else emptyList()

            var maxPage = 1
            val paginationLinks = doc.select("ul.pagination li a")
            if (paginationLinks.isNotEmpty()) {
                val maxFound = paginationLinks.mapNotNull { it.attr("data-pagenumber").toIntOrNull() }.maxOrNull()
                if (maxFound != null) maxPage = maxFound
            }
            if (page > maxPage) maxPage = page
            return FilteredResult(movies, maxPage)
        } catch (e: Exception) {
            if (e is SessionExpiredException) throw e
            return FilteredResult(emptyList(), 1)
        }
    }

    /**
     * Parses a DOM element containing a list of movie items.
     * Handles lazy-loaded images and extracts basic metadata.
     */
    private fun parseMovies(row: Element): List<Movie> {
        val movies = mutableListOf<Movie>()
        val children = row.children()
        for (el in children) {
            val link = el.select("a.textOverImage, a.textOverImage2").first()
            val img = el.select("img").first()
            if (link != null && img != null) {
                var title = link.attr("data-title")
                if (title.isEmpty()) title = el.select("h1.film_title").text()
                if (title.isEmpty()) title = el.select(".film_title").text()
                val url = link.attr("href")
                val desc = link.attr("data-text")

                // --- Lazy Loading Handling ---
                // Prioritize 'data-src' usually used for lazy loading
                var imgUrl = img.attr("data-src")
                if (imgUrl.isEmpty()) {
                    imgUrl = img.attr("src")
                }
                // Fallback to <source> tags (e.g., for WebP)
                if (imgUrl.isEmpty()) {
                    imgUrl = img.parent()?.select("source")?.first()?.let { source ->
                        source.attr("data-src").ifEmpty { source.attr("srcset") }
                    } ?: ""
                }

                val year = el.select("div.film_year").text()
                val quality = el.select(".quality-version").text().trim()
                val views = el.select(".view").text().trim()
                var ratingText = el.select(".direct-version span").text().trim()
                if (ratingText.isEmpty()) ratingText = el.select(".rate").text().trim()

                movies.add(
                    Movie(
                        title = title,
                        description = desc,
                        imageUrl = imgUrl,
                        moviePageUrl = url,
                        year = year,
                        rating = ratingText.ifEmpty { null },
                        views = views.ifEmpty { null },
                        qualityLabel = quality.ifEmpty { null }
                    )
                )
            }
        }
        return movies
    }

    /**
     * Performs a search request for a specific phrase.
     * Parses separate sections for Movies and Serials from the advanced search results.
     */
    @Throws(SessionExpiredException::class)
    fun searchMovies(phrase: String): SearchResult {
        val encodedPhrase = URLEncoder.encode(phrase, "UTF-8")
        val url = "https://filman.cc/item?phrase=$encodedPhrase"
        val (request, _) = createRequest(url)
        try {
            val response = client.newCall(request).execute()
            val html = response.body.string()
            val doc = Jsoup.parse(html)
            checkSession(doc)
            var movies = listOf<Movie>()
            var serials = listOf<Movie>()
            val advancedSearch = doc.select("#advanced-search")
            val headers = advancedSearch.select("h3")
            for (header in headers) {
                val title = header.text().trim().uppercase()
                val headerRow = header.parent()?.parent()
                val itemsRow = headerRow?.nextElementSibling()
                if (itemsRow != null) {
                    val itemList = itemsRow.select("#item-list").first()
                    if (itemList != null) {
                        val items = parseMovies(itemList)
                        if (title.contains("FILMY")) movies = items
                        else if (title.contains("SERIALE")) serials = items
                    }
                }
            }
            return SearchResult(movies, serials)
        } catch (e: Exception) {
            if (e is SessionExpiredException) throw e
            return SearchResult(emptyList(), emptyList())
        }
    }

    /**
     * Fetches rows for the main page (e.g., "Trending Movies", "Top Movies").
     */
    @Throws(SessionExpiredException::class)
    fun fetchMainPageRows(): LinkedHashMap<String, List<Movie>> {
        val (request, _) = createRequest("https://filman.cc")
        try {
            val response = client.newCall(request).execute()
            val html = response.body.string()
            val doc = Jsoup.parse(html)
            checkSession(doc)
            val results = LinkedHashMap<String, List<Movie>>()
            val headers = doc.select("h3")
            for (header in headers) {
                val headerText = header.text().trim().uppercase()
                if (ROW_TITLES.any { headerText.contains(it) }) {
                    val row = header.nextElementSibling()
                    if (row != null && row.id() == "item-list") {
                        val movies = parseMovies(row)
                        if (movies.isNotEmpty()) results[headerText] = movies
                    }
                }
            }
            return results
        } catch (e: Exception) {
            if (e is SessionExpiredException) throw e
            return linkedMapOf()
        }
    }

    /**
     * Fetches detailed information for a specific movie or series page.
     * Extracts poster, description, rating, seasons/episodes (if series), and player links.
     */
    @Throws(SessionExpiredException::class)
    fun fetchMovieDetails(pageUrl: String): MovieDetails? {
        val (request, _) = createRequest(pageUrl)
        try {
            val response = client.newCall(request).execute()
            val html = response.body.string()
            val doc = Jsoup.parse(html)
            checkSession(doc)
            val title = doc.select("h1 span[itemprop='title']").text().ifEmpty {
                doc.select("#item-headline h2").text().ifEmpty { "No title" }
            }
            val description = doc.select("p.description").first()?.text() ?: "No description"
            val posterUrl = doc.select("#single-poster img").attr("src")
            val backgroundStyle = doc.select("#item-headline").attr("style")
            val pattern = Pattern.compile("url\\((.*?)\\)")
            val matcher = pattern.matcher(backgroundStyle)
            val backgroundUrl = if (matcher.find()) {
                matcher.group(1)?.replace("'", "")?.replace(")", "") ?: posterUrl
            } else {
                posterUrl
            }
            val ratingValue = doc.select("span[itemprop='ratingValue']").text()
            val reviewCount = doc.select("span[itemprop='reviewCount']").text()
            val rating = if (ratingValue.isNotEmpty()) "$ratingValue ($reviewCount)" else ""
            var year = ""
            var views = ""
            val infoLists = doc.select(".info ul")
            for (ul in infoLists) {
                val text = ul.text()
                if (text.contains("Rok:") || text.contains("Premiera:")) year = ul.select("li").last()?.text() ?: ""
                if (text.contains("Odsłony:")) views = ul.select("li").last()?.text() ?: ""
            }
            val genres = doc.select("ul.categories li a").map { it.text() }
            val countries = doc.select("ul.country li a").map { it.text() }
            val seasons = mutableListOf<Season>()
            val episodeListRoot = doc.select("#episode-list > li")
            val isSeries = episodeListRoot.isNotEmpty()
            if (isSeries) {
                for (seasonLi in episodeListRoot) {
                    val seasonName = seasonLi.selectFirst("span")?.text() ?: "Season"
                    val episodes = mutableListOf<Episode>()
                    val epLinks = seasonLi.select("ul li a")
                    for (epLink in epLinks) episodes.add(Episode(epLink.text(), epLink.attr("href")))
                    seasons.add(Season(seasonName, episodes))
                }
            }
            val playerLinks = mutableListOf<PlayerLink>()
            if (!isSeries) playerLinks.addAll(extractLinks(doc))

            val comments = extractComments(doc)

            return MovieDetails(title, posterUrl, backgroundUrl, description, rating, year, views, genres, countries, playerLinks, comments, seasons, isSeries)
        } catch (e: Exception) {
            if (e is SessionExpiredException) throw e
            return null
        }
    }

    /**
     * Fetches details for a specific episode page.
     * Similar to movie details but focused on episode-specific metadata and navigation (prev/next).
     */
    @Throws(SessionExpiredException::class)
    fun fetchEpisodeDetails(episodeUrl: String): EpisodeDetails? {
        val (request, _) = createRequest(episodeUrl)
        try {
            val response = client.newCall(request).execute()
            val html = response.body.string()
            val doc = Jsoup.parse(html)
            checkSession(doc)
            val seriesTitle = doc.select("#item-headline h2").text()
            val episodeTitle = doc.select("#item-headline h3").text()

            var description = ""
            val itemInfo = doc.select("#item-info")
            val headers = itemInfo.select("h4")

            for (h4 in headers) {
                val headerText = h4.text().trim()
                if (headerText.equals("Opis Odcinka", ignoreCase = true) || headerText.equals("Streszczenie", ignoreCase = true)) {
                    var next = h4.nextElementSibling()
                    while (next != null && next.tagName() != "p" && next.tagName() != "div") {
                        next = next.nextElementSibling()
                    }
                    description = next?.text()?.trim() ?: ""
                    break
                }
            }

            val backgroundStyle = doc.select("#item-headline").attr("style")
            val pattern = Pattern.compile("url\\((.*?)\\)")
            val matcher = pattern.matcher(backgroundStyle)
            val backgroundUrl = if (matcher.find()) {
                matcher.group(1)?.replace("'", "")?.replace(")", "") ?: ""
            } else {
                ""
            }
            val playerLinks = extractLinks(doc)
            val comments = extractComments(doc)

            var prevUrl: String? = null
            var nextUrl: String? = null
            val navLinks = doc.select(".btn-group a.btn")
            for (link in navLinks) {
                val text = link.text()
                if (text.contains("Poprzedni", ignoreCase = true)) prevUrl = link.attr("href")
                if (text.contains("Następny", ignoreCase = true)) nextUrl = link.attr("href")
            }
            return EpisodeDetails(seriesTitle, episodeTitle, description, backgroundUrl, playerLinks, comments, prevUrl, nextUrl)
        } catch (e: Exception) {
            if (e is SessionExpiredException) throw e
            return null
        }
    }

    private fun createRequest(url: String): Pair<Request, String> {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val cookie = prefs.getString(AppConstants.COOKIE_KEY, null) ?: throw SessionExpiredException()

        // Use user agent from preferences if available (synced from WebView), otherwise use default from constants
        val userAgent = prefs.getString(AppConstants.USER_AGENT_KEY, AppConstants.DEFAULT_USER_AGENT)
            ?: AppConstants.DEFAULT_USER_AGENT

        val request = Request.Builder().url(url).addHeader("Cookie", cookie).addHeader("User-Agent", userAgent).build()
        return Pair(request, userAgent)
    }

    /**
     * Extracts player links (iframes) from the page.
     * Cleans up host names and dates.
     */
    private fun extractLinks(doc: Document): MutableList<PlayerLink> {
        val playerLinks = mutableListOf<PlayerLink>()
        val linkRows = doc.select("#links tbody tr")
        for (row in linkRows) {
            val linkElement = row.select("a[data-iframe]").first()
            if (linkElement != null) {
                // Raw text example: "voe.sx dodane 10 godzin temu przez KtosTam"
                val rawText = linkElement.text()

                // Split host name from added date
                val parts = rawText.split(" dodane ")
                val hostName = parts.getOrNull(0)?.trim() ?: rawText
                var addedDate = parts.getOrNull(1)?.trim() ?: ""

                // Remove user information "przez Użytkownik"
                if (addedDate.contains("przez")) {
                    addedDate = addedDate.split("przez")[0].trim()
                }

                val dataIframe = linkElement.attr("data-iframe")
                val version = row.select("td").getOrNull(1)?.text()?.trim() ?: "Inne"
                val quality = row.select("td").getOrNull(2)?.text()?.trim() ?: ""

                if(dataIframe.isNotEmpty()) {
                    playerLinks.add(PlayerLink(hostName, dataIframe, quality, version, addedDate))
                }
            }
        }
        return playerLinks
    }

    /**
     * Extracts hierarchical comments from the page.
     */
    private fun extractComments(doc: Document): List<Comment> {
        val commentsList = mutableListOf<Comment>()
        val rootComments = doc.select("#comments > .comment")
        for (element in rootComments) {
            recursiveParseComments(element, 0, commentsList)
        }
        return commentsList
    }

    /**
     * Recursively parses comment elements to handle nested replies (depth).
     * Cleans up unwanted UI text like "Spoiler warning".
     */
    private fun recursiveParseComments(element: Element, depth: Int, targetList: MutableList<Comment>) {
        val cleanElement = element.clone()
        // Remove nested comments temporarily to process only current node's text
        cleanElement.select(".comment").remove()
        cleanElement.select(".spoiler-click").remove()
        cleanElement.select(".response-comment").remove()
        cleanElement.select(".modal").remove()

        val author = cleanElement.select("h5 b").text()
        val date = cleanElement.select("h5 sup.pull-right").text()
        var text = cleanElement.select("p").text().trim()

        text = text.replace("Zawartosc tego komentarza moze zawierac spoiler", "")
        text = text.replace("Pokaz ukryta zawartosc", "").trim()

        if (author.isNotEmpty() && text.isNotEmpty()) {
            targetList.add(Comment(author, date, text, depth))
        }

        // Process children
        for (child in element.children()) {
            if (child.hasClass("comment")) {
                recursiveParseComments(child, depth + 1, targetList)
            }
        }
    }
}