package com.example.testes.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.MissionNode
import com.example.testes.model.MissionStatus
import com.example.testes.model.SubjectPlanet
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.EmptyState
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.MissionCompleted
import com.example.testes.ui.theme.MissionLocked
import com.example.testes.ui.theme.Spacing
import com.example.testes.viewmodel.MissionViewModel
import kotlinx.coroutines.launch

@Composable
fun PlanetMapScreenRoute(
    planetId: String,
    onBack: () -> Unit,
    onMissionSelected: (MissionNode, SubjectPlanet) -> Unit,
    viewModel: MissionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val planet = remember(state.planets, planetId) { viewModel.getPlanet(planetId) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshFromStorage()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = planet?.name ?: "Planeta", onBackClick = onBack) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                planet == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Planeta não encontrado", body = "Volte para a galáxia e escolha outro.")
                }
                else -> PlanetMapScreen(
                    planet = planet,
                    onMissionClick = { node -> onMissionSelected(node, planet) },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
fun PlanetMapScreen(
    planet: SubjectPlanet,
    onMissionClick: (MissionNode) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.md, end = Spacing.md,
                top = Spacing.md, bottom = Spacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(key = "header") { PlanetHeader(planet) }
            itemsIndexed(planet.missions, key = { _, m -> m.id }) { index, node ->
                MissionRow(
                    isLast = index == planet.missions.lastIndex,
                    node = node,
                    accent = planet.color,
                    onClick = {
                        when (node.status) {
                            MissionStatus.LOCKED -> scope.launch {
                                snackbarHostState.showSnackbar("Conclua a missão anterior para liberar.")
                            }
                            else -> onMissionClick(node)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlanetHeader(planet: SubjectPlanet) {
    val completed = planet.missions.count { it.status == MissionStatus.COMPLETED }
    val total = planet.missions.size
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlanetOrb(color = planet.color, progress = planet.progress, size = 64.dp, ringStroke = 3.dp)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        planet.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$completed de $total missões concluídas",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(planet.progress * 100).toInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = planet.color
                )
            }
            Spacer(Modifier.height(Spacing.md))
            val animatedProgress by animateFloatAsState(
                targetValue = planet.progress.coerceIn(0f, 1f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "planetHeaderProgress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = planet.color,
                trackColor = planet.color.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
private fun MissionRow(
    isLast: Boolean,
    node: MissionNode,
    accent: Color,
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        MissionRail(node = node, accent = accent, isLast = isLast)
        Spacer(Modifier.width(Spacing.md))
        MissionCard(
            node = node,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = onClick
        )
    }
}

@Composable
private fun MissionRail(
    node: MissionNode,
    accent: Color,
    isLast: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(40.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(
                    color = when (node.status) {
                        MissionStatus.COMPLETED -> MissionCompleted.copy(alpha = 0.18f)
                        MissionStatus.CURRENT   -> accent
                        MissionStatus.LOCKED    -> Color.Transparent
                    },
                    shape = CircleShape
                )
                .border(
                    1.dp,
                    when (node.status) {
                        MissionStatus.COMPLETED -> MissionCompleted.copy(alpha = 0.55f)
                        MissionStatus.CURRENT   -> accent
                        MissionStatus.LOCKED    -> CardBorder
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (node.status) {
                MissionStatus.COMPLETED -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Concluída",
                    tint = MissionCompleted,
                    modifier = Modifier.size(18.dp)
                )
                MissionStatus.CURRENT -> Text(
                    "${node.order + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                MissionStatus.LOCKED -> Icon(
                    Icons.Default.Lock,
                    contentDescription = "Bloqueada",
                    tint = MissionLocked,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (!isLast) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(72.dp)
                    .background(
                        when (node.status) {
                            MissionStatus.COMPLETED -> MissionCompleted.copy(alpha = 0.30f)
                            MissionStatus.CURRENT   -> accent.copy(alpha = 0.30f)
                            MissionStatus.LOCKED    -> CardBorder
                        }
                    )
            )
        }
    }
}

@Composable
fun MissionCard(
    node: MissionNode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    MissionCard(
        node = node,
        accent = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun MissionCard(
    node: MissionNode,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val border = when (node.status) {
        MissionStatus.CURRENT   -> accent.copy(alpha = 0.45f)
        MissionStatus.COMPLETED -> MissionCompleted.copy(alpha = 0.30f)
        MissionStatus.LOCKED    -> CardBorder
    }
    val surfaceColor = if (node.status == MissionStatus.CURRENT)
        MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        shape = RoundedCornerShape(14.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, border),
        shadowElevation = if (node.status == MissionStatus.CURRENT) 3.dp else 1.dp,
        onClick = onClick
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (node.stageLabel.isNotBlank()) {
                        Text(
                            node.stageLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        node.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (node.status == MissionStatus.LOCKED)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                MissionStatusBadge(node.status, accent)
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                node.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MissionStatusBadge(status: MissionStatus, accent: Color) {
    val (label, fg, bg) = when (status) {
        MissionStatus.CURRENT -> Triple("Em curso", accent, accent.copy(alpha = 0.14f))
        MissionStatus.COMPLETED -> Triple("Concluída", MissionCompleted, MissionCompleted.copy(alpha = 0.14f))
        MissionStatus.LOCKED -> Triple("Bloqueada", MaterialTheme.colorScheme.onSurfaceVariant, Color.White.copy(alpha = 0.04f))
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = fg,
            fontWeight = FontWeight.Medium
        )
    }
}
