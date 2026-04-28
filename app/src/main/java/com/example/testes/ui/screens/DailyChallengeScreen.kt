package com.example.testes.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar
import com.example.testes.viewmodel.HomeViewModel

@Composable
fun DailyChallengeScreen(viewModel: HomeViewModel, onBackClick: () -> Unit) {
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    val questions = listOf(
        "Qual a unidade de força no SI?" to listOf("Newton", "Joule", "Watt", "Pascal"),
        "A primeira lei de Newton trata de?" to listOf("Inércia", "Ação e Reação", "Força", "Gravidade"),
        "Qual a velocidade da luz no vácuo?" to listOf("300.000 km/s", "150.000 km/s", "1.000.000 km/s", "1.000 km/s")
    )

    Scaffold(
        topBar = { AppTopBar(title = "Sequência de Desafios", onBackClick = onBackClick) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (!showResults) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Progress
                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    AnimatedContent(targetState = currentQuestionIndex) { index ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Pergunta ${index + 1} de ${questions.size}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = questions[index].first,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            questions[index].second.forEach { option ->
                                Button(
                                    onClick = {
                                        if (option == questions[index].second[0]) score++ // Mock correct answer is always index 0
                                        if (currentQuestionIndex < questions.size - 1) {
                                            currentQuestionIndex++
                                        } else {
                                            viewModel.completeQuiz()
                                            showResults = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                ) {
                                    Text(option)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Desafio Concluído!", style = MaterialTheme.typography.headlineMedium)
                    Text(text = "Você acertou $score de ${questions.size} perguntas", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onBackClick) {
                        Text("Voltar para Home")
                    }
                }
            }
        }
    }
}