package com.example.testes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.testes.R
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.GlassCard
import com.example.testes.ui.components.HomeMenuButton
import com.example.testes.ui.components.PrimaryButton
import com.example.testes.ui.components.SectionHeader
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.Radius
import com.example.testes.ui.theme.SpaceCyanSubtle
import com.example.testes.ui.theme.Spacing
import com.example.testes.ui.theme.TestesTheme
import com.example.testes.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDailyChallenge: () -> Unit,
    onStudyCampaign: () -> Unit,
    onChatDoubt: () -> Unit,
    onImprovementStats: () -> Unit,
    onMissions: () -> Unit = {}
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val showQuizPopup by viewModel.showQuizPopup.collectAsStateWithLifecycle()
    val xp by viewModel.xp.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val trailProgress by viewModel.trailProgress.collectAsStateWithLifecycle()

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    if (showQuizPopup) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissQuizPopup() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Desafio do dia") },
            text = { Text("Renato preparou um desafio rápido de Análise Dimensional para hoje.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissQuizPopup()
                    onDailyChallenge()
                }) { Text("Começar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissQuizPopup() }) { Text("Depois") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Física Interativa") }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            AnimatedVisibility(visible = contentVisible, enter = fadeIn(tween(220))) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Spacer(Modifier.height(Spacing.xs))

                    CommandBridgeHero(
                        userName = user.name.ifBlank { "Comandante" },
                        level = level,
                        xp = xp,
                        completion = progress.overallCompletion
                    )

                    DailyMissionCard(onClick = onDailyChallenge)

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionHeader(title = "Atalhos", subtitle = "Ferramentas para estudar mais rápido")
                        QuickActionsGrid(
                            onRenato = onChatDoubt,
                            onMissions = onMissions
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionHeader(title = "Sua missão", subtitle = "Avance pela trilha de Análise Dimensional")
                        GlassCard(onClick = onStudyCampaign) {
                            Text(
                                "Trilha em progresso",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "${(trailProgress * 100).toInt()}% concluído",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            LinearProgressIndicator(
                                progress = { trailProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        SectionHeader(title = "Seu progresso", subtitle = "Resumo do que já estudou")
                        StatsRow(
                            xp = xp,
                            level = level,
                            completion = progress.overallCompletion,
                            onClick = onImprovementStats
                        )
                    }

                    Spacer(Modifier.height(Spacing.lg))
                }
            }
        }
    }
}

@Composable
private fun CommandBridgeHero(
    userName: String,
    level: Int,
    xp: Int,
    completion: Float
) {
    val panelShape = RoundedCornerShape(Radius.lg)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .border(1.dp, CardBorder, panelShape)
    ) {
        // Background cockpit placeholder (usando cor sólida se o recurso falhar na preview)
        Image(
            painter = painterResource(R.drawable.hero_space_bridge),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.75f)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val planetCenter = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.28f)
            drawCircle(
                color = SpaceCyanSubtle.copy(alpha = 0.08f),
                radius = size.minDimension * 0.24f,
                center = planetCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.minDimension * 0.31f,
                center = planetCenter,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.035f),
                radius = size.minDimension * 0.39f,
                center = planetCenter,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        // Vinheta
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color(0x44000000),
                        1f to Color(0xE8070D17)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.42f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                userName.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        // Overlay de saudação
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Spacing.md, vertical = Spacing.md)
        ) {
            Text(
                "Bem-vindo de volta",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                userName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Nível $level · $xp XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Chip de progresso
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.md)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.26f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "${(completion * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DailyMissionCard(onClick: () -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    "Missão do dia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "5 questões rápidas para manter o ritmo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        PrimaryButton(label = "Começar desafio", onClick = onClick)
    }
}

@Composable
private fun QuickActionsGrid(
    onRenato: () -> Unit,
    onMissions: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ActionTile(
            modifier = Modifier.fillMaxWidth(),
            title = "Falar com Renato",
            subtitle = "Texto, foto, voz ou PDF",
            icon = Icons.Default.Psychology,
            onClick = onRenato
        )
        ActionTile(
            modifier = Modifier.fillMaxWidth(),
            title = "Missões",
            subtitle = "Trilha de Análise Dimensional",
            icon = Icons.Default.Explore,
            onClick = onMissions
        )
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeMenuButton(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun StatsRow(xp: Int, level: Int, completion: Float, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    "Painel adaptativo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Nível $level · $xp XP · ${(completion * 100).toInt()}% completo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TestesTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08111F))
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            CommandBridgeHero(
                userName = "Comandante Gustavo",
                level = 12,
                xp = 4500,
                completion = 0.78f
            )
            DailyMissionCard(onClick = {})
            QuickActionsGrid(onRenato = {}, onMissions = {})
        }
    }
}
