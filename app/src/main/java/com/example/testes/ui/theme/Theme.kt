package com.example.testes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SpacePrimary,
    onPrimary = StarWhite,
    secondary = SpaceSecondary,
    onSecondary = SpaceBackground,
    tertiary = SpaceAccent,
    onTertiary = SpaceBackground,
    background = SpaceBackground,
    onBackground = StarWhite,
    surface = SpaceSurface,
    onSurface = StarWhite,
    surfaceVariant = SpaceSurfaceVar,
    onSurfaceVariant = TextSecondary,
    primaryContainer = SpacePrimary.copy(alpha = 0.16f),
    onPrimaryContainer = StarWhite,
    secondaryContainer = SpaceSecondary.copy(alpha = 0.16f),
    onSecondaryContainer = StarWhite,
    tertiaryContainer = SpaceAccent.copy(alpha = 0.16f),
    onTertiaryContainer = StarWhite,
    error = SpaceError,
    onError = StarWhite,
    outline = CardBorder,
    outlineVariant = DividerSubtle
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun TestesTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
