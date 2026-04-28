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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar

@Composable
fun ImprovementStatsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = "Estatísticas de Melhora", onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text(text = "Seu Desempenho", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                StatItem("Taxa de Acerto", "85%", Icons.Default.AssignmentTurnedIn)
                StatItem("Qualidade de Estudo", "Excelente", Icons.Default.Star)
                StatItem("Horas Estudadas", "12h 30min", Icons.Default.Schedule)
                StatItem("Perguntas Feitas", "42", Icons.Default.HistoryEdu)
                StatItem("Fases Completadas", "5 / 20", Icons.Default.QueryStats)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
                Text(text = value, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}