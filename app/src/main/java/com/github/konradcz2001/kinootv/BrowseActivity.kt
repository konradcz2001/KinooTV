package com.github.konradcz2001.kinootv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.rememberDrawerState
import com.github.konradcz2001.kinootv.ui.screens.HomeScreen
import com.github.konradcz2001.kinootv.ui.screens.KidsScreen
import com.github.konradcz2001.kinootv.ui.screens.MoviesScreen
import com.github.konradcz2001.kinootv.ui.screens.SearchScreen
import com.github.konradcz2001.kinootv.ui.screens.SerialsScreen
import com.github.konradcz2001.kinootv.ui.screens.WatchlistScreen
import kotlinx.coroutines.delay

/**
 * The main container activity for the Android TV interface.
 * Sets up the navigation drawer and handles screen switching (Home, Search, Movies, etc.).
 */
class BrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    surface = Color(0xFF121212),
                    onSurface = Color.White,
                    primary = Color(0xFFE50914),
                    secondary = Color(0xFFE50914)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), colors = SurfaceDefaults.colors(containerColor = Color(0xFF0F0F0F))) {
                    AppLayout()
                }
            }
        }
    }
}

data class MenuItemData(val title: String, val icon: ImageVector)

val CartoonFontFamily = FontFamily(
    Font(R.font.cartoon_font)
)

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppLayout() {
    var selectedMenuIndex by remember { mutableIntStateOf(1) } // Default start at Home (index 1)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val menuItems = listOf(
        MenuItemData(stringResource(R.string.menu_search), Icons.Default.Search),
        MenuItemData(stringResource(R.string.menu_home), Icons.Default.Home),
        MenuItemData(stringResource(R.string.menu_watchlist), Icons.Default.Visibility),
        MenuItemData(stringResource(R.string.menu_movies), Icons.Default.Movie),
        MenuItemData(stringResource(R.string.menu_serials), Icons.Default.Tv),
        MenuItemData(stringResource(R.string.menu_kids), Icons.Default.ChildCare)
    )

    // Focus requesters for menu items
    val focusRequesters = remember { List(menuItems.size) { FocusRequester() } }

    // --- BACK HANDLER LOGIC ---
    // 1. If a Dialog is open -> BackHandler ignores it (Dialog has priority), Dialog closes.
    // 2. If no Dialog and menu is CLOSED -> enabled=true -> Intercept Return and open menu.
    // 3. If menu is OPEN -> enabled=false -> Do not intercept -> System exits app.
    BackHandler(enabled = drawerState.currentValue == DrawerValue.Closed) {
        drawerState.setValue(DrawerValue.Open)
    }

    // Auto-focus on "Home" at startup
    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequesters[1].requestFocus() } catch (_: Exception) {}
    }

    // When menu opens, focus the currently selected item
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            try { focusRequesters[selectedMenuIndex].requestFocus() } catch (_: Exception) {}
        }
    }

    NavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Using Box to position title and menu independently
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color(0xFF0F0F0F))
                    .padding(12.dp)
            ) {
                // 1. TITLE - Positioned at TopStart
                if (drawerState.currentValue == DrawerValue.Open) {
                    Text(
                        text = stringResource(R.string.app_logo_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = CartoonFontFamily,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopStart) // Pin to top
                            .padding(16.dp, 30.dp)
                    )
                }

                // 2. OPTION LIST - Positioned exactly at CenterStart
                Column(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.Center
                ) {
                    menuItems.forEachIndexed { index, item ->
                        NavigationDrawerItem(
                            selected = selectedMenuIndex == index,
                            onClick = {
                                selectedMenuIndex = index
                                drawerState.setValue(DrawerValue.Closed)
                            },
                            leadingContent = { Icon(imageVector = item.icon, contentDescription = item.title) },
                            content = { Text(text = item.title, modifier = Modifier.padding(start = 8.dp)) },
                            modifier = Modifier.focusRequester(focusRequesters[index])
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            // Screen switching logic
            when (selectedMenuIndex) {
                0 -> SearchScreen()
                1 -> HomeScreen()
                2 -> WatchlistScreen()
                3 -> MoviesScreen()
                4 -> SerialsScreen()
                5 -> KidsScreen()
            }
        }
    }
}