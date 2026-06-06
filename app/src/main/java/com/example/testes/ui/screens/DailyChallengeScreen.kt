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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.LearningApiClient
import com.example.testes.model.DailyChallengeStatus
import com.example.testes.model.DailyQuestion
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

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
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        learningApiClient.getDailyChallengeStatus()
            .onSuccess { status = it }
            .onFailure { errorMessage = "Nao consegui mostrar o desafio agora." }
        learningApiClient.getDailyChallenge()
            .onSuccess { questions = it }
            .onFailure { errorMessage = "Nao consegui mostrar o desafio agora." }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Desafio Diario", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    errorMessage != null -> Text(errorMessage ?: "Nao foi possivel carregar o desafio.", modifier = Modifier.align(Alignment.Center))
                    status?.completedToday == true -> CompletedChallengeCard(status = status, onBackClick = onBackClick)
                    questions.isEmpty() -> Text("Nenhuma pergunta disponivel.", modifier = Modifier.align(Alignment.Center))
                    !showResults -> {
                        val question = questions[currentQuestionIndex]
                        Column(modifier = Modifier.fillMaxSize()) {
                            AppHeroPanel(
                                title = "Sprint de hoje",
                                subtitle = "Responda uma vez por dia e acompanhe sua consistencia."
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = Color(0xFFFFF3CC)
                                        ) {
                                            Icon(
                                                Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = Color(0xFFC78700),
                                                modifier = Modifier.padding(10.dp).size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Pergunta ${currentQuestionIndex + 1} de ${questions.size}", style = MaterialTheme.typography.titleMedium)
                                            Text(question.subjectName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("$score acertos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    LinearProgressIndicator(
                                        progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(22.dp))

                                    AnimatedContent(targetState = currentQuestionIndex, label = "dailyQuestion") { index ->
                                        val current = questions[index]
                                        Column {
                                            Text(
                                                text = current.question,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            current.options.forEachIndexed { optionIndex, option ->
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedExplanation = current.explanation
                                                        val newScore = if (optionIndex == current.correctIndex) score + 1 else score
                                                        score = newScore
                                                        if (currentQuestionIndex < questions.size - 1) {
                                                            currentQuestionIndex++
                                                        } else {
                                                            viewModel.completeQuiz()
                                                            scope.launch {
                                                                learningApiClient.submitDailyChallenge(newScore, questions.size)
                                                                    .onSuccess {
                                                                        accuracyRate = it
                                                                        status = DailyChallengeStatus(true, newScore, questions.size, it)
                                                                    }
                                                                    .onFailure {
                                                                        errorMessage = "Nao consegui salvar seu resultado agora."
                                                                        status = DailyChallengeStatus(completedToday = true)
                                                                    }
                                                            }
                                                            showResults = true
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                                    shape = MaterialTheme.shapes.medium
                                                ) {
                                                    Text(option, modifier = Modifier.padding(vertical = 5.dp))
                                                }
                                            }
                                        }
                                    }

                                    selectedExplanation?.let {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                it,
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
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
    Surface(
        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Resultado: ${status?.score ?: 0} de ${status?.total ?: 0} acertos", style = MaterialTheme.typography.titleMedium)
            status?.accuracyRate?.let {
                Text("Aproveitamento: $it%", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(22.dp))
            Button(onClick = onBackClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
                Text("Voltar")
            }
        }
    }
}
