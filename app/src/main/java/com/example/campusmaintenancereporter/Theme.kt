package com.example.campusmaintenancereporter

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E9B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6F7),
    onPrimaryContainer = Color(0xFF0B2E4F),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3EFEA),
    onSecondaryContainer = Color(0xFF00423A),
    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF1B232B),
    surface = Color.White,
    onSurface = Color(0xFF1B232B),
    surfaceVariant = Color(0xFFE3EAF1),
    onSurfaceVariant = Color(0xFF475563),
    error = Color(0xFFC62828),
    errorContainer = Color(0xFFFDEBE9),
    onErrorContainer = Color(0xFF5F1412),
    outline = Color(0xFF8A97A5)
)

@Composable
fun MaintenanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
