package com.example.testes.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.SubjectPlanet
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.EmptyState
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.Spacing
import com.example.testes.viewmodel.MissionViewModel

@Composable
fun GalaxyScreenRoute(
    onBack: () -> Unit,
    onPlanetSelected: (String) -> Unit,
    viewModel: MissionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Missão", onBackClick = onBack) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Galáxia indisponível", body = state.errorMessage.orEmpty())
                }
                state.planets.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Sem planetas", body = "A campanha ainda está sendo preparada.")
                }
                else -> GalaxyScreen(
                    planets = state.planets,
                    onPlanetClick = { onPlanetSelected(it.id) }
                )
            }
        }
    }
}

@Composable
fun GalaxyScreen(
    planets: List<SubjectPlanet>,
    onPlanetClick: (SubjectPlanet) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        GalaxyHeader(planets)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md, end = Spacing.md,
                top = Spacing.sm, bottom = Spacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(planets, key = { it.id }) { planet ->
                PlanetCard(
                    planet = planet,
                    onClick = { onPlanetClick(planet) }
                )
            }
        }
    }
}

@Composable
private fun GalaxyHeader(planets: List<SubjectPlanet>) {
    val total = planets.size
    val completed = planets.count { it.progress >= 1f }
    val overall = if (total == 0) 0f else planets.sumOf { it.progress.toDouble() }.toFloat() / total
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                "Sua galáxia de estudo",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$completed de $total planetas dominados · ${(overall * 100).toInt()}% no total",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlanetCard(
    planet: SubjectPlanet,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "planetCardScale"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp,
        interactionSource = interaction,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlanetOrb(color = planet.color, progress = planet.progress, size = 96.dp)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                planet.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${(planet.progress * 100).toInt()}% concluído",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlanetOrb(
    color: Color,
    progress: Float,
    size: androidx.compose.ui.unit.Dp,
    ringStroke: androidx.compose.ui.unit.Dp = 3.dp
) {
    val trackColor = Color.White.copy(alpha = 0.08f)
    val coerced = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = coerced,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "planetOrbProgress"
    )
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                val strokePx = ringStroke.toPx()
                val inset = strokePx / 2 + 2f
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.74f)
                .background(
                    brush = Brush.radialGradient(
                        0f to color.copy(alpha = 0.95f),
                        0.7f to color.copy(alpha = 0.55f),
                        1f to color.copy(alpha = 0.18f)
                    ),
                    shape = CircleShape
                )
        )
    }
}
