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
    onPrimary = SpaceInk,
    secondary = PhotonAmber,
    onSecondary = SpaceInk,
    tertiary = CoralCharge,
    background = SpaceInk,
    onBackground = LabMist,
    surface = DeepNavy,
    onSurface = LabMist,
    surfaceVariant = Color(0xFF243049),
    onSurfaceVariant = Color(0xFFC7D0DD),
    primaryContainer = FieldTeal,
    onPrimaryContainer = SpaceInk,
    secondaryContainer = Color(0xFF3A3140),
    onSecondaryContainer = Color(0xFFFFE0A3)
)

private val LightColorScheme = lightColorScheme(
    primary = FieldTeal,
    onPrimary = Color.White,
    secondary = DeepNavy,
    onSecondary = Color.White,
    tertiary = CoralCharge,
    background = LabMist,
    onBackground = Graphite,
    surface = CardWhite,
    onSurface = Graphite,
    surfaceVariant = Color(0xFFE8EEF6),
    onSurfaceVariant = MutedSteel,
    primaryContainer = Color(0xFFD9F6F2),
    onPrimaryContainer = Color(0xFF053B37),
    secondaryContainer = Color(0xFFE8ECF5),
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
