package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit, onBackClick: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    
    val pages = listOf(
        OnboardingPage(
            "Aprenda Física com IA",
            "Tenha um tutor particular disponível 24h para tirar todas as suas dúvidas de física universitária.",
            Icons.Default.School
        ),
        OnboardingPage(
            "Avatar Interativo",
            "Estude com um avatar que explica o conteúdo por voz e interage com você em tempo real.",
            Icons.Default.Chat
        ),
        OnboardingPage(
            "Acompanhe seu Progresso",
            "Visualize sua evolução em cada módulo e esteja pronto para suas provas acadêmicas.",
            Icons.Default.AutoGraph
        )
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "",
                onBackClick = {
                    if (currentPage > 0) {
                        currentPage--
                    } else {
                        onBackClick()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val page = pages[currentPage]

            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Page Indicator
            Row {
                pages.forEachIndexed { index, _ ->
                    Surface(
                        modifier = Modifier
                            .size(10.dp)
                            .padding(horizontal = 2.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (index == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentPage == pages.size - 1) "Continuar" else "Próximo")
            }
        }
    }
}
