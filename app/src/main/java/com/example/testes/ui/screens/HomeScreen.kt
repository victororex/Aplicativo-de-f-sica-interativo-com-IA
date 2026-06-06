package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.AvatarBox
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
            title = { Text("Novo desafio") },
            text = { Text("Um quiz diario de fisica esta disponivel para hoje.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissQuizPopup()
                    onDailyChallenge()
                }) {
                    Text("Comecar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissQuizPopup() }) {
                    Text("Depois")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Fisica Interativa", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                AppHeroPanel(
                    title = "Ola, ${user.name}",
                    subtitle = "Escolha uma trilha, tire duvidas por voz e acompanhe sua evolucao."
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarBox(modifier = Modifier.size(96.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tutor ativo",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sua sessao esta pronta para estudar com aulas, trilha e conversa guiada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Comece por aqui",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                HomeMenuButton(
                    title = "Desafio Diario",
                    subtitle = "Questoes rapidas para aquecer",
                    icon = Icons.Default.EmojiEvents,
                    accent = Color(0xFFFFC857),
                    onClick = onDailyChallenge
                )

                Spacer(modifier = Modifier.height(12.dp))

                HomeMenuButton(
                    title = "Campanha de Estudo",
                    subtitle = "Trilha guiada por materia",
                    icon = Icons.Default.Map,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onStudyCampaign
                )

                Spacer(modifier = Modifier.height(12.dp))

                HomeMenuButton(
                    title = "Chat de Duvidas",
                    subtitle = "Converse escrevendo ou falando",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    accent = Color(0xFFFF6B5F),
                    onClick = onChatDoubt
                )

                Spacer(modifier = Modifier.height(12.dp))

                HomeMenuButton(
                    title = "Minha Evolucao",
                    subtitle = "Veja sua evolucao real",
                    icon = Icons.Default.AutoGraph,
                    accent = Color(0xFF4C6FFF),
                    onClick = onImprovementStats
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
