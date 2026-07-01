package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = GlassPrimary,
    secondary = GlassSecondary,
    tertiary = GlassAccent,
    background = GlassBgDark,
    surface = GlassCardDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    primaryContainer = Color(0x338B5CF6),
    onPrimaryContainer = Color(0xFFDDD6FE),
    secondaryContainer = Color(0x334F46E5),
    onSecondaryContainer = Color(0xFFE0E7FF)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GlassPrimary,
    secondary = GlassSecondary,
    tertiary = GlassAccent,
    background = GlassBgLight,
    surface = GlassCardLight,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF6D28D9),
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF4338CA)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
