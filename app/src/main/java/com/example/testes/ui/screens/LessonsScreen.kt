package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.model.Lesson
import com.example.testes.ui.components.AppTopBar

@Composable
fun LessonsScreen(
    onModuleSelect: (String) -> Unit,
    onBackClick: () -> Unit
) {
    // Mock modules with progress
    val modules = listOf(
        Triple("Mecânica", 1.0f, true),
        Triple("Termologia", 0.4f, false),
        Triple("Eletromagnetismo", 0.0f, false),
        Triple("Ótica", 0.0f, false)
    )

    Scaffold(
        topBar = { AppTopBar(title = "Matérias", onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(modules) { (name, progress, isCompleted) ->
                ModuleCard(
                    name = name,
                    progress = progress,
                    isCompleted = isCompleted,
                    onClick = { onModuleSelect(name) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ModuleCard(
    name: String,
    progress: Float,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) Color(0xFF2E7D32) else Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isCompleted) {
                    Text("Concluído", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = Color.LightGray
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}