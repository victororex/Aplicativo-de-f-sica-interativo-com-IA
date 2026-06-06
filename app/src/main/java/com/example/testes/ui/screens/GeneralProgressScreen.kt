package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.Subject
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.MiniPhysicsMark
import com.example.testes.viewmodel.ProfileViewModel

@Composable
fun GeneralProgressScreen(
    onBackClick: () -> Unit,
    onSubjectClick: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val subjects by viewModel.subjects.collectAsState()
    val averageProgress = subjects.takeIf { it.isNotEmpty() }?.map { it.progress }?.average()?.toFloat() ?: 0f
    val completedLessons = subjects.sumOf { it.completedLessons }
    val totalLessons = subjects.sumOf { it.totalLessons }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Meu Progresso", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppHeroPanel(
                        title = "${(averageProgress * 100).toInt()}% do caminho",
                        subtitle = "$completedLessons de $totalLessons aulas concluidas. Continue pelo tema que mais precisa de atencao."
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ProgressSummaryCard(
                            label = "Aulas feitas",
                            value = completedLessons.toString(),
                            iconColor = Color(0xFF00A99D),
                            modifier = Modifier.weight(1f)
                        )
                        ProgressSummaryCard(
                            label = "Materias",
                            value = subjects.size.toString(),
                            iconColor = Color(0xFFFFC857),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                items(subjects) { subject ->
                    SubjectProgressCard(subject = subject, onClick = { onSubjectClick(subject.id) })
                }
            }
        }
    }
}

@Composable
private fun ProgressSummaryCard(label: String, value: String, iconColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(118.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = MaterialTheme.shapes.medium, color = iconColor.copy(alpha = 0.14f)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = iconColor, modifier = Modifier.padding(8.dp).size(22.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SubjectProgressCard(subject: Subject, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniPhysicsMark()
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${subject.completedLessons}/${subject.totalLessons} aulas concluidas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { subject.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${(subject.progress * 100).toInt()}% concluido",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
