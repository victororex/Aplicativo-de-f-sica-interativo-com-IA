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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.DailyChallengeInstance
import com.example.testes.model.DailyDimensionalQuestion
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.EmptyState
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.MissionCompleted
import com.example.testes.ui.theme.Spacing
import com.example.testes.viewmodel.DailyChallengeListViewModel
import kotlinx.coroutines.delay

@Composable
fun DailyChallengeRunScreenRoute(
    instanceId: String,
    onBack: () -> Unit,
    viewModel: DailyChallengeListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val instance = remember(state.instances, instanceId) { viewModel.getInstance(instanceId) }

    // Picks persistidos (só relevante quando 'completed' = true).
    var savedPicks by remember(instanceId) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(instanceId, instance?.completed) {
        if (instance?.completed == true) {
            savedPicks = viewModel.loadPicks(instanceId)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = instance?.definition?.title ?: "Desafio", onBackClick = onBack) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                instance == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Desafio indisponível", body = "Volte e escolha outro desafio do dia.")
                }
                instance.questions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Sem perguntas", body = "Avance na trilha para liberar este desafio.")
                }
                instance.completed -> DailyChallengeReviewScreen(instance = instance, picks = savedPicks)
                else -> DailyChallengeRunScreen(
                    instance = instance,
                    onFinish = { picks ->
                        viewModel.recordCompletion(instance, picks)
                        onBack()
                    }
                )
            }
        }
    }
}

/* ------------------------------- Modo prova ------------------------------- */

@Composable
private fun DailyChallengeRunScreen(
    instance: DailyChallengeInstance,
    onFinish: (picks: Map<String, Int>) -> Unit
) {
    val accent = difficultyColor(instance.definition.difficulty)
    val total = instance.questions.size
    val answers = remember(instance.instanceId) { mutableStateMapOf<String, Int>() }
    var finished by remember(instance.instanceId) { mutableStateOf(false) }
    var remaining by remember(instance.instanceId) { mutableStateOf(instance.definition.timeLimitSeconds) }

    LaunchedEffect(instance.instanceId) {
        while (!finished && remaining > 0) {
            delay(1000L)
            remaining -= 1
        }
        if (remaining <= 0 && !finished) {
            finished = true
            onFinish(answers.toMap())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.md, end = Spacing.md,
            top = Spacing.md, bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "header") {
            TimerHeader(
                accent = accent,
                remainingSeconds = remaining,
                totalSeconds = instance.definition.timeLimitSeconds,
                answered = answers.size,
                total = total
            )
        }
        items(instance.questions, key = { it.id }) { q ->
            QuizQuestionCard(
                question = q,
                accent = accent,
                picked = answers[q.id],
                review = false,
                onPick = { idx -> if (!finished) answers[q.id] = idx }
            )
        }
        item(key = "finish") {
            Button(
                onClick = {
                    finished = true
                    onFinish(answers.toMap())
                },
                enabled = !finished && answers.size == total,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    if (answers.size == total) "Concluir desafio"
                    else "Responda ${total - answers.size} pergunta(s) para concluir"
                )
            }
        }
    }
}

/* ------------------------------- Modo revisão ----------------------------- */

@Composable
private fun DailyChallengeReviewScreen(
    instance: DailyChallengeInstance,
    picks: Map<String, Int>
) {
    val accent = difficultyColor(instance.definition.difficulty)
    val correct = instance.questions.count { q -> picks[q.id] == q.correctIndex }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.md, end = Spacing.md,
            top = Spacing.md, bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "review_header") {
            ReviewHeader(
                accent = accent,
                correct = correct,
                total = instance.questions.size,
                rewardXp = instance.definition.rewardXp
            )
        }
        items(instance.questions, key = { it.id }) { q ->
            QuizQuestionCard(
                question = q,
                accent = accent,
                picked = picks[q.id]?.takeIf { it >= 0 },
                review = true,
                onPick = { /* read-only */ }
            )
        }
    }
}

@Composable
private fun ReviewHeader(accent: Color, correct: Int, total: Int, rewardXp: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MissionCompleted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    "Resultado do dia",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$correct / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Você ganhou +$rewardXp XP. Volte amanhã para um novo desafio sorteado.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* -------------------------------- Header timer ---------------------------- */

@Composable
private fun TimerHeader(
    accent: Color,
    remainingSeconds: Int,
    totalSeconds: Int,
    answered: Int,
    total: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    formatTime(remainingSeconds),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$answered / $total respondidas",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            val progress = if (totalSeconds == 0) 0f
                else (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.14f)
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "%02d:%02d".format(m, r)
}

/* ------------------------------ Card de pergunta -------------------------- */

@Composable
private fun QuizQuestionCard(
    question: DailyDimensionalQuestion,
    accent: Color,
    picked: Int?,
    review: Boolean,
    onPick: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                question.statement,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                question.options.forEachIndexed { i, label ->
                    val isPicked = picked == i
                    val isCorrect = review && i == question.correctIndex
                    val isWrongPick = review && isPicked && i != question.correctIndex
                    val border = when {
                        isCorrect -> MissionCompleted
                        isWrongPick -> MaterialTheme.colorScheme.error
                        isPicked -> accent
                        else -> CardBorder
                    }
                    OutlinedButton(
                        onClick = { if (!review) onPick(i) },
                        enabled = !review,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            when {
                                isCorrect -> Icon(Icons.Default.Check, null, tint = MissionCompleted, modifier = Modifier.size(16.dp))
                                isWrongPick -> Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                isPicked -> Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(16.dp))
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
