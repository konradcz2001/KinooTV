package com.github.konradcz2001.kinootv.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.konradcz2001.kinootv.data.Comment
import com.github.konradcz2001.kinootv.data.PlayerLink
import org.json.JSONObject

/**
 * A clickable button representing a streaming player link.
 * Handles the logic of Base64 decoding the iframe data, extracting the source URL,
 * and launching an external intent to play the video in a browser or player.
 *
 * @param link The [PlayerLink] data object containing host info and encrypted src.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerLinkButton(link: PlayerLink, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Button(
        onClick = {
            try {
                // Decode Base64 data to get the real URL
                val decodedBytes = Base64.decode(link.dataIframe, Base64.DEFAULT)
                val jsonString = String(decodedBytes)
                val json = JSONObject(jsonString)
                var playerUrl = json.getString("src")

                // Fix common URL protocol issues
                if (playerUrl.startsWith("httpss:")) playerUrl = playerUrl.replace("httpss:", "https:")
                playerUrl = playerUrl.replace("\\/", "/")

                val browserIntent = Intent(Intent.ACTION_VIEW, playerUrl.toUri())
                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                try {
                    context.startActivity(browserIntent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "Nie znaleziono aplikacji do odtwarzania", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("PlayerLink", "Error opening link", e)
                Toast.makeText(context, "Błąd otwarcia linku", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF222222),
            contentColor = Color.LightGray,
            focusedContainerColor = Color(0xFFE50914),
            focusedContentColor = Color.White
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display Host Name and Quality (data already cleaned by scraper)
            Text("${link.hostName} ${link.quality}", fontSize = 12.sp, fontWeight = FontWeight.Bold)

            if (link.addedDate.isNotEmpty()) {
                Text(
                    text = link.addedDate,
                    fontSize = 10.sp,
                    color = Color.Gray.copy(alpha = 0.8f) // Slightly darker for subtitle text
                )
            }
        }
    }
}

/**
 * Displays a single user comment with visual indentation based on reply depth.
 *
 * @param comment The comment data object.
 */
@Composable
fun CommentView(comment: Comment) {
    // Limit indentation depth to prevent UI breaking on deep threads
    val indentLevel = minOf(comment.depth, 20)
    val startPadding = (indentLevel * 20).dp

    Column(
        modifier = Modifier
            .padding(start = startPadding)
            .fillMaxWidth()
            .focusable()
            .background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Text(
            text = "${comment.author}  •  ${comment.date}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}