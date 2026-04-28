package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.data.mock.MockData
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.AvatarBox

@Composable
fun LessonDetailScreen(
    lessonId: String?,
    onBackClick: () -> Unit,
    onStartChat: () -> Unit
) {
    val lesson = MockData.lessons.find { it.id == lessonId }

    Scaffold(
        topBar = { AppTopBar(title = "Detalhes da Aula", onBackClick = onBackClick) }
    ) { padding ->
        if (lesson != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarBox()
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(text = lesson.title, style = MaterialTheme.typography.headlineMedium)
                Text(text = lesson.module, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = lesson.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = lesson.content, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = onStartChat,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text("Iniciar Aula com Avatar")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aula não encontrada")
            }
        }
    }
}