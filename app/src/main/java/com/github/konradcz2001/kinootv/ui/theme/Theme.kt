package com.github.konradcz2001.kinootv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * The main entry point for the application's UI theming.
 * Wraps content in [MaterialTheme] with specific color schemes and typography configurations.
 *
 * @param isInDarkTheme Detects if the system is in dark mode (usually true for TV interfaces).
 * @param content The Composable content to display within the theme.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KinooTVTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isInDarkTheme) {
        darkColorScheme(
            primary = Purple80,
            secondary = PurpleGrey80,
            tertiary = Pink80
        )
    } else {
        lightColorScheme(
            primary = Purple40,
            secondary = PurpleGrey40,
            tertiary = Pink40
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}