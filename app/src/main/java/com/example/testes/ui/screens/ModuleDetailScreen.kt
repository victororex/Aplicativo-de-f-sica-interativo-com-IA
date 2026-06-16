package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.Lesson
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.MiniPhysicsMark
import com.example.testes.viewmodel.LessonsViewModel

@Composable
fun ModuleDetailScreen(
    moduleName: String?,
    onBackClick: () -> Unit,
    onLessonClick: (String) -> Unit,
    viewModel: LessonsViewModel = viewModel()
) {
    val lessons by viewModel.lessons.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val completedCount = lessons.count { it.isCompleted }

    LaunchedEffect(moduleName) {
        moduleName?.let { viewModel.loadLessons(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = selectedSubject?.name ?: "Aulas", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                isLoading && lessons.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                errorMessage != null && lessons.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Não consegui mostrar as aulas agora.")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AppHeroPanel(
                            title = selectedSubject?.name ?: "Aulas",
                            subtitle = "${completedCount}/${lessons.size} aulas concluidas. Escolha uma aula para continuar."
                        )
                    }
                    itemsIndexed(lessons) { index, lesson ->
                        LessonListCard(
                            index = index,
                            lesson = lesson,
                            onClick = { onLessonClick(lesson.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonListCard(index: Int, lesson: Lesson, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniPhysicsMark()
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Aula ${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(lesson.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (lesson.isCompleted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ) {
                Icon(
                    if (lesson.isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (lesson.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp).size(22.dp)
                )
            }
        }
    }
}
