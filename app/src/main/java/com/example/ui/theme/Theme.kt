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

private val LightColorScheme =
  lightColorScheme(
    primary = DarkGreenPrimary,
    secondary = GentlePurpleSecondary,
    background = SoftGrayBackground,
    surface = SoftGraySurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = CustomOnSurface,
    onSurface = CustomOnSurface,
    primaryContainer = DarkGreenContainer,
    secondaryContainer = LightPurpleContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
