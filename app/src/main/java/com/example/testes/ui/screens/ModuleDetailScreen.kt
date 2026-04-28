package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar

@Composable
fun ModuleDetailScreen(
    moduleName: String?,
    onBackClick: () -> Unit,
    onLessonClick: (String) -> Unit
) {
    // Mock sub-topics for the module
    val subTopics = listOf(
        "Introdução", "1ª Lei de Newton", "2ª Lei de Newton", 
        "3ª Lei de Newton", "Atrito", "Plano Inclinado", "Exercícios"
    )

    Scaffold(
        topBar = { AppTopBar(title = moduleName ?: "Detalhes", onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "Escolha um tópico para estudar ou revisar:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subTopics) { topic ->
                    Card(
                        onClick = { onLessonClick(topic) },
                        modifier = Modifier.aspectRatio(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Text(
                                text = topic,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}