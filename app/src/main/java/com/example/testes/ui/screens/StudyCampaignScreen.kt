package com.example.testes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testes.data.api.LearningApiClient
import com.example.testes.model.CampaignExercise
import com.example.testes.model.CampaignNode
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.EmptyState
import com.example.testes.ui.components.PrimaryButton
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.Spacing
import com.example.testes.ui.theme.SpaceSuccess
import kotlinx.coroutines.launch

/* ============================================================
 *  Hierarquia desta tela
 *  ------------------------------------------------------------
 *  StudyCampaignScreen   (entry point — mantém o nome p/ NavHost)
 *    └─ MissionScreen
 *         ├─ TrackHeaderCard          (resumo da trilha + progresso)
 *         └─ MissionTimeline          (LazyColumn vertical)
 *               └─ MissionPhaseCard   (estados: atual / bloqueada / concluída)
 *                     └─ StageMission (drill-in com as questões da fase)
 *
 *  Visual:
 *   - Dark mode com fundo cinza-azulado (SpaceBackground = #07101D).
 *   - Cards em superfície ligeiramente mais clara, raio 14dp.
 *   - Cor primária (#4F86E8) para fase atual; verde discreto p/ concluída.
 *   - Sem neon, sem glow, sem partículas — só uma animação de check
 *     ao concluir e fade/scale ao desbloquear.
 * ============================================================ */

private val PhaseShape = RoundedCornerShape(14.dp)
private const val CurrentPhaseAlpha = 0.92f
private const val LockedPhaseAlpha = 0.55f

@Composable
fun StudyCampaignScreen(
    onBackClick: () -> Unit,
    onSubjectClick: (String) -> Unit,
    learningApiClient: LearningApiClient = LearningApiClient()
) {
    var nodes by remember { mutableStateOf<List<CampaignNode>>(emptyList()) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        learningApiClient.getCampaign()
            .onSuccess {
                nodes = it
                selectedNodeId = it.firstOrNull { n -> n.isUnlocked && n.progress < 1f }?.id
                    ?: it.firstOrNull { n -> n.isUnlocked }?.id
                errorMessage = null
            }
            .onFailure { errorMessage = "Não consegui mostrar sua trilha agora." }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Missão", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Não foi possível abrir", body = errorMessage.orEmpty())
                }
                nodes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Trilha vazia", body = "Aguarde — a campanha ainda está sendo preparada.")
                }
                else -> MissionScreen(
                    nodes = nodes,
                    selectedNodeId = selectedNodeId,
                    onSelect = { id -> selectedNodeId = id },
                    learningApiClient = learningApiClient,
                    onStageCompleted = { completedNode, score, total ->
                        scope.launch {
                            learningApiClient.submitCampaignStage(completedNode.id, score, total)
                            nodes = nodes.unlockAfter(completedNode.id, score, total)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MissionScreen(
    nodes: List<CampaignNode>,
    selectedNodeId: String?,
    onSelect: (String) -> Unit,
    learningApiClient: LearningApiClient,
    onStageCompleted: (CampaignNode, Int, Int) -> Unit
) {
    val totalPhases = nodes.size
    val completedPhases = nodes.count { it.progress >= 1f }
    val overall = if (totalPhases == 0) 0f
        else nodes.sumOf { it.progress.toDouble() }.toFloat() / totalPhases

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.md, end = Spacing.md,
            top = Spacing.md, bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "header") {
            TrackHeaderCard(
                title = "Trilha de Análise Dimensional",
                subtitle = "$completedPhases de $totalPhases fases concluídas",
                progress = overall
            )
        }
        itemsIndexed(nodes, key = { _, n -> n.id }) { index, node ->
            val state = phaseStateOf(node)
            MissionPhaseCard(
                index = index,
                isLast = index == nodes.lastIndex,
                node = node,
                state = state,
                isSelected = selectedNodeId == node.id,
                onSelect = { if (node.isUnlocked) onSelect(node.id) },
                learningApiClient = learningApiClient,
                onStageCompleted = onStageCompleted
            )
        }
    }
}

/* ---------------- TrackHeaderCard ---------------- */

@Composable
private fun TrackHeaderCard(title: String, subtitle: String, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "trackProgress"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PhaseShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Rocket,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(Spacing.md))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        }
    }
}

/* ---------------- MissionPhaseCard ---------------- */

private enum class PhaseState { Locked, Current, Completed }

private fun phaseStateOf(node: CampaignNode): PhaseState = when {
    node.progress >= 1f -> PhaseState.Completed
    node.isUnlocked     -> PhaseState.Current
    else                -> PhaseState.Locked
}

@Composable
private fun MissionPhaseCard(
    index: Int,
    isLast: Boolean,
    node: CampaignNode,
    state: PhaseState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    learningApiClient: LearningApiClient,
    onStageCompleted: (CampaignNode, Int, Int) -> Unit
) {
    val unlockedAlpha by animateFloatAsState(
        targetValue = if (state == PhaseState.Locked) LockedPhaseAlpha else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "unlockedAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(unlockedAlpha)
    ) {
        // Coluna esquerda: indicador circular + linha vertical fina
        TimelineRail(
            index = index,
            isLast = isLast,
            state = state,
            isSelected = isSelected
        )

        Spacer(Modifier.width(Spacing.md))

        // Card da fase à direita
        PhaseCardBody(
            node = node,
            state = state,
            isSelected = isSelected,
            onSelect = onSelect,
            learningApiClient = learningApiClient,
            onStageCompleted = onStageCompleted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineRail(
    index: Int,
    isLast: Boolean,
    state: PhaseState,
    isSelected: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val border = CardBorder
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(40.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(
                    color = when (state) {
                        PhaseState.Completed -> SpaceSuccess.copy(alpha = 0.18f)
                        PhaseState.Current   -> primary
                        PhaseState.Locked    -> Color.Transparent
                    },
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = when (state) {
                        PhaseState.Completed -> SpaceSuccess.copy(alpha = 0.55f)
                        PhaseState.Current   -> primary
                        PhaseState.Locked    -> border
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                PhaseState.Completed -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Concluída",
                    tint = SpaceSuccess,
                    modifier = Modifier.size(18.dp)
                )
                PhaseState.Current -> Text(
                    "${index + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onPrimary
                )
                PhaseState.Locked -> Icon(
                    Icons.Default.Lock,
                    contentDescription = "Bloqueada",
                    tint = onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (!isLast) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(if (isSelected) 320.dp else 72.dp)
                    .background(
                        color = when (state) {
                            PhaseState.Completed -> SpaceSuccess.copy(alpha = 0.30f)
                            PhaseState.Current   -> primary.copy(alpha = 0.30f)
                            PhaseState.Locked    -> border
                        }
                    )
            )
        }
    }
}

@Composable
private fun PhaseCardBody(
    node: CampaignNode,
    state: PhaseState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    learningApiClient: LearningApiClient,
    onStageCompleted: (CampaignNode, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseSurface = MaterialTheme.colorScheme.surface
    val highlightedSurface = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = when (state) {
        PhaseState.Current   -> MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
        PhaseState.Completed -> SpaceSuccess.copy(alpha = 0.30f)
        PhaseState.Locked    -> CardBorder
    }
    val surfaceColor = if (state == PhaseState.Current) highlightedSurface else baseSurface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        shape = PhaseShape,
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (state == PhaseState.Current) 3.dp else 1.dp,
        onClick = { if (state != PhaseState.Locked) onSelect() }
    ) {
        Column(Modifier.padding(Spacing.md)) {
            // Cabeçalho do card: ícone, título, chip de status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(
                            color = when (state) {
                                PhaseState.Completed -> SpaceSuccess.copy(alpha = 0.14f)
                                PhaseState.Current   -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                PhaseState.Locked    -> Color.White.copy(alpha = 0.04f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        stageIcon(node, fallbackIndex = 0),
                        contentDescription = null,
                        tint = when (state) {
                            PhaseState.Completed -> SpaceSuccess
                            PhaseState.Current   -> MaterialTheme.colorScheme.primary
                            PhaseState.Locked    -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        node.stageLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        node.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state == PhaseState.Locked)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                PhaseStatusChip(state)
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(
                node.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state == PhaseState.Current) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = nextStepHint(node),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Barra de progresso (questão X de Y)
            val totalQuestions = node.exercises.size.takeIf { it > 0 } ?: 4
            val currentQuestion = (node.progress * totalQuestions).toInt().coerceAtMost(totalQuestions)
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { node.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when (state) {
                        PhaseState.Completed -> SpaceSuccess
                        PhaseState.Current   -> MaterialTheme.colorScheme.primary
                        PhaseState.Locked    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    },
                    trackColor = Color.White.copy(alpha = 0.06f)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Questão $currentQuestion de $totalQuestions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Drill-down com as questões (mantido do código antigo)
            AnimatedVisibility(visible = isSelected && state == PhaseState.Current) {
                StageMission(
                    node = node.withFallbackExercises(),
                    learningApiClient = learningApiClient,
                    onStageCompleted = onStageCompleted
                )
            }
        }
    }
}

@Composable
private fun PhaseStatusChip(state: PhaseState) {
    val (label, fg, bg) = when (state) {
        PhaseState.Current -> Triple(
            "Em curso",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        )
        PhaseState.Completed -> Triple(
            "Concluída",
            SpaceSuccess,
            SpaceSuccess.copy(alpha = 0.14f)
        )
        PhaseState.Locked -> Triple(
            "Bloqueado",
            MaterialTheme.colorScheme.onSurfaceVariant,
            Color.White.copy(alpha = 0.04f)
        )
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = fg,
            fontWeight = FontWeight.Medium
        )
    }
}

/* ---------------- Helpers ---------------- */

private fun nextStepHint(node: CampaignNode): String {
    val label = node.stageLabel.lowercase()
    return when {
        "base" in label || "1" in label -> "Próximo passo: reconhecer grandezas e traduzir unidades para dimensões."
        "fórmula" in label || "formula" in label || "2" in label -> "Próximo passo: montar fórmulas a partir das dimensões."
        "erro" in label || "caça" in label || "3" in label -> "Próximo passo: identificar inconsistências dimensionais."
        "final" in label || "desafio" in label || "4" in label -> "Próximo passo: aplicar tudo em um problema completo."
        else -> "Próximo passo: continue de onde parou."
    }
}

private fun stageIcon(node: CampaignNode, fallbackIndex: Int): ImageVector {
    val label = node.stageLabel.lowercase()
    return when {
        "base" in label || "1" in label -> Icons.Default.AutoAwesome
        "fórmula" in label || "formula" in label || "forja" in label -> Icons.Default.Calculate
        "erro" in label || "caça" in label -> Icons.Default.Search
        "final" in label || "desafio" in label -> Icons.Default.EmojiEvents
        else -> when (fallbackIndex % 5) {
            0 -> Icons.Default.AutoAwesome
            1 -> Icons.Default.Calculate
            2 -> Icons.Default.Science
            3 -> Icons.Default.FlashOn
            else -> Icons.Default.BarChart
        }
    }
}

/* ---------------- Drill-in com as questões da fase ---------------- */

@Composable
private fun StageMission(
    node: CampaignNode,
    learningApiClient: LearningApiClient,
    onStageCompleted: (CampaignNode, Int, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val exercises = node.exercises
    var exerciseIndex by remember(node.id) { mutableIntStateOf(0) }
    var selectedOption by remember(node.id, exerciseIndex) { mutableIntStateOf(-1) }
    var correctAnswers by remember(node.id) { mutableIntStateOf(0) }
    var savedCompletion by remember(node.id) { mutableStateOf(false) }
    var exerciseStartedAt by remember(node.id, exerciseIndex) { mutableLongStateOf(System.currentTimeMillis()) }
    val exercise = exercises.getOrNull(exerciseIndex)
    val answered = selectedOption >= 0
    val correct = exercise != null && selectedOption == exercise.correctIndex
    val completed = exerciseIndex >= exercises.size

    LaunchedEffect(completed, savedCompletion, node.id, correctAnswers, exercises.size) {
        if (completed && !savedCompletion) {
            savedCompletion = true
            onStageCompleted(node, correctAnswers, exercises.size)
        }
    }

    Column(
        modifier = Modifier.padding(top = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (completed) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SpaceSuccess.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(animationSpec = tween(360)) + fadeIn(animationSpec = tween(360))
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = SpaceSuccess, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text("Fase concluída", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("$correctAnswers de ${exercises.size} acertos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Column
        }

        if (exercise == null) {
            Text("Nenhum desafio disponível.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        Text(
            "Questão ${exerciseIndex + 1} de ${exercises.size}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Text(
                exercise.question,
                modifier = Modifier.padding(Spacing.md),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        exercise.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == index
            Surface(
                onClick = {
                    if (!answered) {
                        selectedOption = index
                        val elapsed = kotlin.math.max(1, ((System.currentTimeMillis() - exerciseStartedAt) / 1000).toInt())
                        scope.launch {
                            learningApiClient.recordCampaignAnswer(exercise, index == exercise.correctIndex, elapsed)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else CardBorder
                ),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Text(
                    option,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = 14.dp)
                )
            }
        }

        AnimatedVisibility(visible = answered) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (correct) SpaceSuccess.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (correct) "Correto. ${exercise.explanation}"
                        else "Quase. ${exercise.explanation}",
                    modifier = Modifier.padding(Spacing.sm),
                    fontSize = 13.sp,
                    color = if (correct) SpaceSuccess else MaterialTheme.colorScheme.error
                )
            }
        }

        PrimaryButton(
            label = when {
                !answered -> "Escolha uma resposta"
                !correct -> "Tentar de novo"
                exerciseIndex == exercises.lastIndex -> "Concluir fase"
                else -> "Próxima"
            },
            onClick = {
                if (correct) {
                    correctAnswers += 1
                    exerciseIndex += 1
                    exerciseStartedAt = System.currentTimeMillis()
                }
                selectedOption = -1
            },
            enabled = answered
        )
    }
}

private fun CampaignNode.withFallbackExercises(): CampaignNode {
    if (exercises.isNotEmpty()) return this
    return copy(
        exercises = fallbackCampaignExercises(subjectId, visualType),
        progress = 0f,
        usesCampaignProgress = false
    )
}

private fun List<CampaignNode>.unlockAfter(nodeId: String, score: Int, total: Int): List<CampaignNode> {
    val completedIndex = indexOfFirst { it.id == nodeId }
    if (completedIndex < 0) return this
    return mapIndexed { index, node ->
        when {
            index == completedIndex ->
                node.copy(progress = if (total > 0) score.toFloat() / total else 1f, isUnlocked = true)
            index == completedIndex + 1 -> node.copy(isUnlocked = true)
            else -> node
        }
    }
}

private fun fallbackCampaignExercises(subjectId: String, visualType: String): List<CampaignExercise> {
    return listOf(
        CampaignExercise("$subjectId-base-1", "Qual dimensão representa comprimento?",
            listOf("[L]", "[M]", "[T]", "[F]"), 0, "Comprimento é representado por [L].", visualType),
        CampaignExercise("$subjectId-base-2", "A expressão v = d/t resulta em:",
            listOf("[L][T]^-1", "[M][L]", "[T]^2", "[L]^2"), 0,
            "Distância dividida por tempo gera [L][T]^-1.", visualType),
        CampaignExercise("$subjectId-base-3", "Podemos somar massa com tempo?",
            listOf("Não", "Sim", "Só com calculadora", "Sempre"), 0,
            "Somas exigem grandezas de mesma dimensão.", visualType)
    )
}
