package com.example.testes.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.DailyChallengeInstance
import com.example.testes.model.Difficulty
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.EmptyState
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.MissionCompleted
import com.example.testes.ui.theme.PlanetAmber
import com.example.testes.ui.theme.PlanetBlue
import com.example.testes.ui.theme.PlanetCoral
import com.example.testes.ui.theme.PlanetMint
import com.example.testes.ui.theme.Spacing
import com.example.testes.viewmodel.DailyChallengeListViewModel

@Composable
fun DailyChallengesScreenRoute(
    onBack: () -> Unit,
    onInstanceSelected: (String) -> Unit,
    viewModel: DailyChallengeListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Desafios diários", onBackClick = onBack) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.instances.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = "Sem desafios por hoje",
                        body = state.errorMessage
                            ?: "Avance na trilha de Análise Dimensional para desbloquear novos desafios."
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.md, end = Spacing.md,
                        top = Spacing.md, bottom = Spacing.xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    item(key = "header") { ListHeader(state.highestMissionUnlocked) }
                    items(state.instances, key = { it.instanceId }) { inst ->
                        DailyChallengeCard(
                            instance = inst,
                            onClick = { onInstanceSelected(inst.instanceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(highestMissionUnlocked: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                "Desafios de hoje",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Você liberou até a missão $highestMissionUnlocked. Cada dia traz um desafio curto " +
                    "(1–2 perguntas) sorteado entre as dificuldades liberadas; perguntas não se repetem antes de 90 dias.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DailyChallengeCard(
    instance: DailyChallengeInstance,
    onClick: () -> Unit
) {
    val def = instance.definition
    val accent = difficultyColor(def.difficulty)
    // Concluído também é clicável: leva à tela de resultados.
    val clickable = instance.questions.isNotEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (instance.completed) MissionCompleted.copy(alpha = 0.45f) else CardBorder),
        shadowElevation = 2.dp,
        enabled = clickable,
        onClick = onClick
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        def.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DifficultyChip(def.difficulty, accent)
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaItem(
                    icon = Icons.Default.Schedule,
                    label = "${def.numQuestions} perguntas · ${def.timeLimitSeconds / 60} min"
                )
                Spacer(Modifier.size(Spacing.md))
                MetaItem(
                    icon = Icons.Default.Stars,
                    label = "+${def.rewardXp} XP",
                    tint = accent
                )
            }
            if (instance.completed) {
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MissionCompleted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        "Concluído hoje · ${instance.lastScore}/${def.numQuestions} · toque para revisar",
                        fontSize = 12.sp,
                        color = MissionCompleted
                    )
                }
            } else if (instance.questions.isEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Sem perguntas disponíveis para o seu progresso atual.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetaItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 12.sp, color = tint)
    }
}

@Composable
private fun DifficultyChip(difficulty: Difficulty, accent: Color) {
    val label = when (difficulty) {
        Difficulty.EASY -> "Fácil"
        Difficulty.MEDIUM -> "Médio"
        Difficulty.HARD -> "Difícil"
        Difficulty.MASTER -> "Mestre"
    }
    Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.16f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = accent
        )
    }
}

internal fun difficultyColor(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> PlanetMint
    Difficulty.MEDIUM -> PlanetBlue
    Difficulty.HARD -> PlanetAmber
    Difficulty.MASTER -> PlanetCoral
}
