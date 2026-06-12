package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppScreenBackground
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Minha Evolucao", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Text(text = "Resumo geral", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    ProgressCard(title = "Analise Dimensional", progress = progress.overallCompletion)
                    Text(
                        text = "Estudo atual: ${progress.currentModule.ifBlank { "Comece uma aula" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text(text = "Aulas", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(subjects) { subject ->
                    ProgressCard(title = subject.name, progress = subject.progress)
                }
            }
        }
    }
}
