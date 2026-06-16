package com.example.testes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.EvolutionPoint
import com.example.testes.model.LearningDashboard
import com.example.testes.model.TopicPerformance
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.EmptyState
import com.example.testes.ui.components.GlassCard
import com.example.testes.ui.components.SectionHeader
import com.example.testes.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImprovementStatsScreen(
    onBackClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Painel adaptativo", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            val d = dashboard
            when {
                d != null -> {
                    if (d.questionsAnswered == 0 && d.questionsAsked == 0 && d.totalStudySeconds == 0) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                title = "Sem dados ainda",
                                body = "Conclua uma aula, responda o desafio diário ou converse com o Renato para ver seu painel."
                            )
                        }
                    } else {
                        DashboardContent(d)
                    }
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(dashboard: LearningDashboard) {
    var period by remember { mutableIntStateOf(0) }
    val series = when (period) {
        1 -> dashboard.weeklyEvolution
        2 -> dashboard.monthlyEvolution
        else -> dashboard.dailyEvolution
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard {
                Text(
                    "${dashboard.accuracyRate}% de acertos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Nível: ${dashboard.adaptiveProfile.exerciseDifficulty} · Foco: ${dashboard.adaptiveProfile.nextTopic}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Tempo", dashboard.studiedTimeLabel, Icons.Default.Schedule, Modifier.weight(1f))
                MetricCard("Questoes", dashboard.questionsAnswered.toString(), Icons.Default.Psychology, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Sessoes", dashboard.completedSessions.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                MetricCard(
                    "Resposta media",
                    if (dashboard.averageResponseTimeSeconds > 0) "${dashboard.averageResponseTimeSeconds}s" else "--",
                    Icons.Default.AutoAwesome,
                    Modifier.weight(1f)
                )
            }
        }
        item {
            GlassCard {
                Text("Evolucao", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Diaria", "Semanal", "Mensal").forEachIndexed { index, label ->
                        AssistChip(onClick = { period = index }, label = { Text(label) })
                    }
                }
                EvolutionChart(series)
            }
        }
        item {
            SectionHeading("Dominio por assunto", "${dashboard.topicsStudied.size} assuntos estudados")
        }
        if (dashboard.topicPerformance.isEmpty()) {
            item { EmptyAnalyticsCard("Responda exercicios para liberar a analise por assunto.") }
        } else {
            items(dashboard.topicPerformance, key = { it.topic }) { topic -> TopicMasteryCard(topic) }
        }
        item {
            SectionHeading("Plano recomendado", "Atualizado automaticamente pelo seu desempenho")
        }
        items(dashboard.recommendations, key = { it.title }) { recommendation ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(recommendation.priority.toString(), fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(recommendation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(recommendation.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(recommendation.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        item {
            SectionHeading("Historico recente", "${dashboard.questionsAsked} perguntas feitas ao Renato")
        }
        if (dashboard.performanceHistory.isEmpty()) {
            item { EmptyAnalyticsCard("Seu historico aparecera depois da primeira resposta.") }
        } else {
            items(dashboard.performanceHistory.take(8)) { item ->
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val correct = item.result == "Correta"
                        Box(
                            Modifier.size(10.dp).background(
                                if (correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                CircleShape
                            )
                        )
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(item.topic, fontWeight = FontWeight.Bold)
                            Text(
                                "${item.result} - ${item.difficulty} - ${formatDate(item.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.responseTimeSeconds > 0) Text("${item.responseTimeSeconds}s")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    GlassCard(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EvolutionChart(points: List<EvolutionPoint>) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Canvas(Modifier.fillMaxWidth().height(190.dp).padding(top = 12.dp)) {
        if (points.isEmpty()) return@Canvas
        repeat(5) { index ->
            val y = size.height * index / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val step = if (points.size == 1) size.width else size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index * step
            val y = size.height - (point.accuracyRate.coerceIn(0, 100) / 100f * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(primary, radius = 6f, center = Offset(x, y))
        }
        drawPath(path, primary, style = Stroke(width = 5f, cap = StrokeCap.Round))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        points.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun TopicMasteryCard(topic: TopicPerformance) {
    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(topic.topic, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${topic.masteryScore}%", color = if (topic.needsReview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
        }
        LinearProgressIndicator(
            progress = { topic.masteryScore / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (topic.needsReview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
        )
        Text(
            "${topic.attempts} respostas, ${topic.accuracyRate}% de acertos, media de ${topic.averageResponseTimeSeconds}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyAnalyticsCard(message: String) {
    GlassCard { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(timestamp))
