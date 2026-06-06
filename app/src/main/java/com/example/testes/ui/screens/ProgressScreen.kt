package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.ProgressCard
import com.example.testes.viewmodel.ProfileViewModel

@Composable
fun ProgressScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val progress by viewModel.progress.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "Meu Progresso", onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text(text = "Resumo Geral", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                ProgressCard(title = "Fisica do Ensino Medio - Geral", progress = progress.overallCompletion)
                Text(
                    text = "Modulo atual: ${progress.currentModule.ifBlank { "Comece uma aula" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(text = "Materias", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(subjects) { subject ->
                ProgressCard(title = subject.name, progress = subject.progress)
            }
        }
    }
}
