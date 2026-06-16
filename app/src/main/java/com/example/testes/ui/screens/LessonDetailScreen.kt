package com.example.testes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.ContentApiClient
import com.example.testes.data.api.SessionManager
import com.example.testes.data.local.LocalBackend
import com.example.testes.model.Lesson
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.GhostButton
import com.example.testes.ui.components.GlassCard
import com.example.testes.ui.components.PrimaryButton
import com.example.testes.ui.components.SectionHeader
import com.example.testes.ui.components.StatusChip
import com.example.testes.ui.components.StatusTone
import com.example.testes.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun LessonDetailScreen(
    lessonId: String?,
    onBackClick: () -> Unit,
    onStartChat: () -> Unit,
    contentApiClient: ContentApiClient = ContentApiClient()
) {
    var lesson by remember { mutableStateOf<Lesson?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(lessonId) {
        if (lessonId == null) {
            isLoading = false
            errorMessage = "Aula não encontrada."
        } else {
            isLoading = true
            contentApiClient.getLesson(lessonId)
                .onSuccess {
                    lesson = it
                    completed = it.isCompleted
                    errorMessage = null
                    SessionManager.accessToken?.let { token ->
                        LocalBackend.recordLessonOpened(token, lessonId)
                    }
                }
                .onFailure { errorMessage = "Não consegui mostrar esta aula agora." }
            isLoading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Aula", onBackClick = onBackClick) },
        floatingActionButton = {
            if (lesson != null) {
                ExtendedFloatingActionButton(
                    onClick = onStartChat,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    text = { Text("Renato") }
                )
            }
        }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            val currentLesson = lesson
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                currentLesson != null -> LessonContent(
                    lesson = currentLesson,
                    completed = completed,
                    errorMessage = errorMessage,
                    onComplete = {
                        scope.launch {
                            contentApiClient.completeLesson(currentLesson.id)
                                .onSuccess { completed = true }
                                .onFailure { errorMessage = "Não consegui salvar a conclusão agora." }
                        }
                    }
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage ?: "Aula não encontrada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LessonContent(
    lesson: Lesson,
    completed: Boolean,
    errorMessage: String?,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        SectionHeader(title = lesson.title, subtitle = lesson.module)

        if (completed) {
            StatusChip(label = "Concluída", tone = StatusTone.Success)
        }

        GlassCard {
            Text(
                lesson.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LessonContentBlocks(lesson.content)

        Spacer(Modifier.height(Spacing.sm))

        if (completed) {
            GhostButton(label = "Aula concluída", onClick = {})
        } else {
            PrimaryButton(label = "Marcar como concluída", onClick = onComplete)
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun LessonContentBlocks(content: String) {
    val blocks = content.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        blocks.forEach { block ->
            val title = block.substringBefore(":", missingDelimiterValue = "")
            val hasShortTitle = title.length in 3..28 && !title.contains(".")
            GlassCard {
                if (hasShortTitle) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = block.substringAfter(":").trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(text = block, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
