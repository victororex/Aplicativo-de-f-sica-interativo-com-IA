package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
                ProgressCard(title = "Física I - Geral", progress = progress.overallCompletion)
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                Text(text = "Módulos", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Mock module progress
            items(1) {
                ProgressCard(title = "Mecânica Clássica", progress = 0.6f)
                ProgressCard(title = "Termodinâmica", progress = 0.1f)
                ProgressCard(title = "Eletromagnetismo", progress = 0.0f)
            }
        }
    }
}