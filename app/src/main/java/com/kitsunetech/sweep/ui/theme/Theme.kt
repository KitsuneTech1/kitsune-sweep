package com.kitsunetech.sweep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SweepColors = darkColorScheme(
    primary = ColdMint,
    onPrimary = DeepCurrent,
    primaryContainer = SlateBin,
    onPrimaryContainer = Paper,
    secondary = WarningClay,
    onSecondary = DeepCurrent,
    background = DeepCurrent,
    onBackground = Paper,
    surface = SlateBin,
    onSurface = Paper,
    surfaceVariant = DeepCurrent,
    onSurfaceVariant = Mist,
    outline = Mist,
    error = WarningClay,
)

@Composable
fun SweepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SweepColors,
        typography = SweepTypography,
        content = content,
    )
}
