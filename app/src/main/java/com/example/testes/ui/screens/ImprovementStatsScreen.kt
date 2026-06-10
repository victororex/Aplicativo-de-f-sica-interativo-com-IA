package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.ImprovementStatsResponse
import com.example.testes.data.api.StatsApiClient
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar

@Composable
fun ImprovementStatsScreen(onBackClick: () -> Unit) {
    var stats by remember { mutableStateOf<ImprovementStatsResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val response = StatsApiClient().getImprovementStats()
        stats = response.getOrNull()
        errorMessage = if (response.isFailure) "Nao consegui mostrar sua evolucao agora." else null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Minha Evolucao", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppHeroPanel(
                        title = "Sua evolucao",
                        subtitle = "Veja acertos, tempo de estudo, perguntas feitas e fases vencidas."
                    )
                }

                item {
                    when {
                        stats != null -> {
                            val current = stats!!
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("Acertos nos exercicios", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("${current.accuracyRate}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { current.accuracyRate.coerceIn(0, 100) / 100f },
                                        modifier = Modifier.fillMaxWidth().height(9.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            StatItem("Como esta indo", current.studyQuality, Icons.Default.Star, Color(0xFFFFC857))
                            StatItem("Tempo estudando", current.studiedTimeLabel, Icons.Default.Schedule, Color(0xFF2454D6))
                            StatItem("Perguntas ao titio Renato", current.questionsAsked.toString(), Icons.Default.HistoryEdu, Color(0xFFFF6B5F))
                            StatItem("Fases vencidas", "${current.completedPhases} / ${current.totalPhases}", Icons.Default.QueryStats, Color(0xFF4C6FFF))
                        }
                        errorMessage != null -> Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                        else -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = accent.copy(alpha = 0.14f)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
