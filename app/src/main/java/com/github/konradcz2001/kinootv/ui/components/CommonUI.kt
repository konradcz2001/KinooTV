package com.github.konradcz2001.kinootv.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.github.konradcz2001.kinootv.data.Movie
import kotlinx.coroutines.delay

/**
 * A shared UI component that displays a hero banner with the movie's backdrop,
 * gradients for readability, and metadata (title, year, rating).
 *
 * @param movie The movie object to display data for.
 */
@Composable
fun MovieDescriptionBanner(movie: Movie) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(model = movie.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.6f), contentScale = ContentScale.Crop)
        // Horizontal gradient (left-to-right) for text readability
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(Color.Black, Color.Transparent), startX = 0f, endX = 1500f)))
        // Vertical gradient (bottom-up)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface), startY = 50f)))

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 20.dp, end = 48.dp).fillMaxWidth(0.7f)) {
            Text(text = movie.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = movie.year, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                if (movie.rating != null) { Spacer(modifier = Modifier.width(12.dp)); Text(text = "★ ${movie.rating}", color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodyMedium) }
                if (movie.qualityLabel != null) { Spacer(modifier = Modifier.width(12.dp)); Text(text = movie.qualityLabel, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Gray.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small).padding(horizontal = 6.dp, vertical = 2.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = movie.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBBBBBB), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * A standard filter button used in search or category screens.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FilterButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.colors(containerColor = Color(0xCC222222), contentColor = Color.White, focusedContainerColor = Color(0xFFE50914)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text(text, fontSize = 12.sp) }
}

/**
 * A smaller button optimized for pagination controls.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PaginationButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.colors(containerColor = Color(0xFF222222), contentColor = Color.LightGray, focusedContainerColor = Color(0xFFE50914), focusedContentColor = Color.White), modifier = Modifier.padding(horizontal = 4.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text(text, fontSize = 12.sp) }
}

/**
 * A generic dialog that displays a list of string options.
 * Used for selecting filters, categories, etc.
 *
 * @param title The dialog title.
 * @param items The list of options to display.
 * @param onDismiss Callback when dialog is closed.
 * @param onSelected Callback returning the selected string item.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SimpleListDialog(title: String, items: List<String>, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(350.dp)
                .height(500.dp)
                .focusProperties { canFocus = false },
            onClick = { },
            colors = CardDefaults.colors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                val listRequester = remember { FocusRequester() }
                LazyColumn {
                    items(items.size) { index ->
                        val item = items[index]
                        Button(
                            onClick = { onSelected(item); onDismiss() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .then(if (index == 0) Modifier.focusRequester(listRequester) else Modifier),
                            colors = ButtonDefaults.colors(containerColor = Color(0xFF333333), contentColor = Color.LightGray, focusedContainerColor = Color(0xFFE50914), focusedContentColor = Color.White)
                        ) { Text(item) }
                    }
                }
                LaunchedEffect(Unit) { delay(100); try { listRequester.requestFocus() } catch (_: Exception) {} }
            }
        }
    }
}