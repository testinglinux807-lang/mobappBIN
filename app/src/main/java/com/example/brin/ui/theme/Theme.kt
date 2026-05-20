package com.example.brin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = CardBg,
    primaryContainer = GreenSurface,
    onPrimaryContainer = GreenDark,
    secondary        = GreenMedium,
    onSecondary      = CardBg,
    tertiary         = GreenLight,
    background       = AppBackground,
    surface          = CardBg,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    outline          = DividerColor,
)

@Composable
fun BRINTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}
