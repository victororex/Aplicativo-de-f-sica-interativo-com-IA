package com.example.testes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = FieldTealLight,
    onPrimary = Color(0xFF061528),
    secondary = PhotonAmber,
    onSecondary = SpaceInk,
    tertiary = CoralCharge,
    background = Color(0xFF07111F),
    onBackground = LabMist,
    surface = Color(0xFF101D33),
    onSurface = LabMist,
    surfaceVariant = Color(0xFF1B2A44),
    onSurfaceVariant = Color(0xFFC7D0DD),
    primaryContainer = Color(0xFF143A7A),
    onPrimaryContainer = Color(0xFFD9EAFF),
    secondaryContainer = Color(0xFF332C18),
    onSecondaryContainer = Color(0xFFFFE0A3)
)

private val LightColorScheme = lightColorScheme(
    primary = FieldTeal,
    onPrimary = Color.White,
    secondary = Color(0xFF0F2E63),
    onSecondary = Color.White,
    tertiary = CoralCharge,
    background = LabMist,
    onBackground = Graphite,
    surface = CardWhite,
    onSurface = Graphite,
    surfaceVariant = Color(0xFFE8EEF6),
    onSurfaceVariant = MutedSteel,
    primaryContainer = Color(0xFFDDEAFF),
    onPrimaryContainer = Color(0xFF092153),
    secondaryContainer = Color(0xFFE8EEF9),
    onSecondaryContainer = DeepNavy,
    tertiaryContainer = Color(0xFFFFE9D9),
    onTertiaryContainer = Color(0xFF4F1A16),
    outline = Color(0xFFB6C1D2)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

@Composable
fun TestesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
