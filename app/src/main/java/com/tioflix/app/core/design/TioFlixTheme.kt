package com.tioflix.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TioFlixDarkColors = darkColorScheme(
    primary = Color(0xFFE50914),
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF111111),
    onSurface = Color.White,
    secondary = Color(0xFFB3B3B3),
    onSecondary = Color.Black
)

@Composable
fun TioFlixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TioFlixDarkColors,
        content = content
    )
}
