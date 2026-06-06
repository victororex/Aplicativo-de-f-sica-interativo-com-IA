package com.example.testes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.PhysicsWaveGraphic

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit, onBackClick: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            "Aprenda Fisica com um tutor",
            "Tenha um tutor particular para transformar duvidas em explicacoes claras.",
            Icons.Default.School,
            Color(0xFF00A99D)
        ),
        OnboardingPage(
            "Converse por voz",
            "Pergunte falando, leia a resposta ou deixe o tutor explicar em voz alta.",
            Icons.AutoMirrored.Filled.Chat,
            Color(0xFFFF6B5F)
        ),
        OnboardingPage(
            "Evolua com trilhas",
            "Acompanhe progresso por materia e estude com uma campanha guiada.",
            Icons.Default.AutoGraph,
            Color(0xFFFFC857)
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "",
                onBackClick = {
                    if (currentPage > 0) currentPage-- else onBackClick()
                }
            )
        }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val page = pages[currentPage]

                Spacer(modifier = Modifier.weight(0.35f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    page.accent,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PhysicsWaveGraphic(modifier = Modifier.fillMaxSize().padding(36.dp))
                    Surface(
                        modifier = Modifier.size(118.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                modifier = Modifier.size(58.dp),
                                tint = page.accent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (index == currentPage) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) page.accent else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) currentPage++ else onFinished()
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(if (currentPage == pages.size - 1) "Continuar" else "Proximo")
                }
            }
        }
    }
}
