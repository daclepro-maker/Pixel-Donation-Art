package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SleekDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF), // Elegant lavender accent
    onPrimary = Color(0xFF381E72), // Deep rich purple
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF1C1B1F), // Elegant Dark Charcoal
    onBackground = Color(0xFFE6E1E5), // Soft white text
    surface = Color(0xFF2B2930), // Slightly elevated grey
    onSurface = Color(0xFFE6E1E5),
    secondaryContainer = Color(0xFF49454F), // Border/stroke companion color
    onSecondaryContainer = Color(0xFFCAC4D0),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFF938F99) // Standard subtitle label gray
)

private val SleekLightColorScheme = SleekDarkColorScheme // Enforce dark theme as the primary aesthetic for the entire app!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default, meeting the user request "Make it a dark theme, minimalist aesthetic"
  dynamicColor: Boolean = false, // Disable dynamic colors to maintain the gorgeous custom neon-on-dark minimalist branding
  content: @Composable () -> Unit,
) {
  val colorScheme = SleekDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
