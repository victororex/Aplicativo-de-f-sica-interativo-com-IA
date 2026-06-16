package com.example.testes.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.LearningApiClient
import com.example.testes.model.DailyChallengeStatus
import com.example.testes.model.DailyQuestion
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.GlassCard
import com.example.testes.ui.components.PrimaryButton
import com.example.testes.ui.components.SectionHeader
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.Spacing
import com.example.testes.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun DailyChallengeScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    learningApiClient: LearningApiClient = LearningApiClient()
) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var showResults by remember { mutableStateOf(false) }
    var accuracyRate by remember { mutableStateOf<Int?>(null) }
    var questions by remember { mutableStateOf<List<DailyQuestion>>(emptyList()) }
    var status by remember { mutableStateOf<DailyChallengeStatus?>(null) }
    var selectedExplanation by remember { mutableStateOf<String?>(null) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var answerWasCorrect by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var questionStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()

    fun finishChallenge(finalScore: Int) {
        viewModel.completeQuiz()
        scope.launch {
            learningApiClient.submitDailyChallenge(finalScore, questions.size)
                .onSuccess {
                    accuracyRate = it
                    status = DailyChallengeStatus(true, finalScore, questions.size, it)
                }
                .onFailure {
                    errorMessage = "Não consegui salvar seu resultado agora."
                    status = DailyChallengeStatus(completedToday = true)
                }
        }
        showResults = true
    }

    LaunchedEffect(Unit) {
        learningApiClient.getDailyChallengeStatus()
            .onSuccess { status = it }
            .onFailure { errorMessage = "Não consegui mostrar o desafio agora." }
        learningApiClient.getDailyChallenge()
            .onSuccess {
                questions = it
                if (it.isNotEmpty() && status?.completedToday != true) {
                    learningApiClient.recordDailyChallengeStarted()
                }
            }
            .onFailure { errorMessage = "Não consegui mostrar o desafio agora." }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Desafio de Análise", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    errorMessage != null -> Text(errorMessage ?: "Não foi possível carregar o desafio.", modifier = Modifier.align(Alignment.Center))
                    status?.completedToday == true -> CompletedChallengeCard(status = status, onBackClick = onBackClick)
                    questions.isEmpty() -> Text("Nenhuma pergunta disponível.", modifier = Modifier.align(Alignment.Center))
                    !showResults -> {
                        val question = questions[currentQuestionIndex]
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            SectionHeader(
                                title = "Desafio do dia",
                                subtitle = "Pergunta ${currentQuestionIndex + 1} de ${questions.size} · $score acertos"
                            )
                            LinearProgressIndicator(
                                progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            )
                            GlassCard {
                                Text(
                                    "${question.topic} · ${question.difficulty}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                AnimatedContent(targetState = currentQuestionIndex, label = "dailyQuestion") { index ->
                                    val current = questions[index]
                                    Column {
                                        Text(
                                            text = current.question,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(Spacing.md))
                                        current.options.forEachIndexed { optionIndex, option ->
                                            val isSelected = selectedOption == optionIndex
                                            val isCorrect = optionIndex == current.correctIndex
                                            val borderColor = when {
                                                selectedOption == null -> CardBorder
                                                isCorrect -> MaterialTheme.colorScheme.tertiary
                                                isSelected -> MaterialTheme.colorScheme.error
                                                else -> CardBorder
                                            }
                                            val containerColor = when {
                                                selectedOption == null -> androidx.compose.ui.graphics.Color.Transparent
                                                isCorrect -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                                isSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                                else -> androidx.compose.ui.graphics.Color.Transparent
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    if (selectedOption == null) {
                                                        selectedOption = optionIndex
                                                        answerWasCorrect = isCorrect
                                                        selectedExplanation = current.explanation
                                                        if (isCorrect) score += 1
                                                        val elapsed = max(1, ((System.currentTimeMillis() - questionStartedAt) / 1000).toInt())
                                                        scope.launch {
                                                            learningApiClient.recordDailyAnswer(current, optionIndex, elapsed)
                                                        }
                                                    }
                                                },
                                                enabled = selectedOption == null,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                shape = MaterialTheme.shapes.medium,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = containerColor,
                                                    contentColor = MaterialTheme.colorScheme.onSurface
                                                )
                                            ) {
                                                Text(option, modifier = Modifier.padding(vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                                selectedExplanation?.let {
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (answerWasCorrect == true) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    ) {
                                        Column(modifier = Modifier.padding(Spacing.sm)) {
                                            Text(
                                                if (answerWasCorrect == true) "Correto" else "Incorreto",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (answerWasCorrect == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(Spacing.md))
                                    PrimaryButton(
                                        label = if (currentQuestionIndex < questions.size - 1) "Próxima pergunta" else "Finalizar desafio",
                                        onClick = {
                                            if (currentQuestionIndex < questions.size - 1) {
                                                currentQuestionIndex++
                                                questionStartedAt = System.currentTimeMillis()
                                                selectedOption = null
                                                answerWasCorrect = null
                                                selectedExplanation = null
                                            } else {
                                                finishChallenge(score)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        CompletedChallengeCard(
                            status = DailyChallengeStatus(true, score, questions.size, accuracyRate),
                            onBackClick = onBackClick,
                            title = "Desafio concluido"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CompletedChallengeCard(
    status: DailyChallengeStatus?,
    onBackClick: () -> Unit,
    title: String = "Desafio de hoje concluido"
) {
    Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth()) {
        GlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text("${status?.score ?: 0} de ${status?.total ?: 0} acertos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                status?.accuracyRate?.let {
                    Text("Aproveitamento: $it%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                PrimaryButton(label = "Voltar", onClick = onBackClick)
            }
        }
    }
}
