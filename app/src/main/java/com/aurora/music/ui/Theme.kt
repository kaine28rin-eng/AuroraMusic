package com.aurora.music.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF8C7BFF), onPrimary = Color.Black,
    secondary = Color(0xFFFF5C8A), tertiary = Color(0xFF3DDC97),
    background = Color(0xFF0B0B10), surface = Color(0xFF15151C),
    surfaceVariant = Color(0xFF22222C),
    onBackground = Color(0xFFF2F2F7), onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFFA0A0B0),
)

@Composable
fun AuroraTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
