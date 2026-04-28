package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.HomeMenuButton
import com.example.testes.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDailyChallenge: () -> Unit,
    onStudyCampaign: () -> Unit,
    onChatDoubt: () -> Unit,
    onImprovementStats: () -> Unit,
    onBackClick: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val showQuizPopup by viewModel.showQuizPopup.collectAsState()

    if (showQuizPopup) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissQuizPopup() },
            title = { Text("Novo Desafio!") },
            text = { Text("Um novo quiz diário de física está disponível. Pronto para testar seus conhecimentos hoje?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissQuizPopup()
                    onDailyChallenge()
                }) {
                    Text("Vamos lá!")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissQuizPopup() }) {
                    Text("Mais tarde")
                }
            }
        )
    }

    Scaffold(
        topBar = { AppTopBar(title = "Olá, ${user.name}", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Avatar Image (using Icon as placeholder)
            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .padding(vertical = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Buttons
            HomeMenuButton(
                title = "Desafio Diário",
                icon = Icons.Default.EmojiEvents,
                onClick = onDailyChallenge
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuButton(
                title = "Campanha de Estudo",
                icon = Icons.Default.Map,
                onClick = onStudyCampaign
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuButton(
                title = "Chat de Dúvidas",
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = onChatDoubt
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            HomeMenuButton(
                title = "Percentual de Melhora",
                icon = Icons.Default.AutoGraph,
                onClick = onImprovementStats
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}