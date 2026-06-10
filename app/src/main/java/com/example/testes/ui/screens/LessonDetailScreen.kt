package com.example.testes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.ContentApiClient
import com.example.testes.model.Lesson
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.AvatarBox
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
            errorMessage = "Aula nao encontrada."
        } else {
            isLoading = true
            contentApiClient.getLesson(lessonId)
                .onSuccess {
                    lesson = it
                    completed = it.isCompleted
                    errorMessage = null
                }
                .onFailure { errorMessage = "Nao consegui mostrar esta aula agora." }
            isLoading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Aula", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (lesson != null) {
            val currentLesson = lesson!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppHeroPanel(
                    title = currentLesson.title,
                    subtitle = currentLesson.description
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarBox(modifier = Modifier.size(84.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Estudo guiado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Leia a aula, veja o exemplo visual e marque quando terminar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                VisualDemoCard(currentLesson)
                Spacer(modifier = Modifier.height(16.dp))
                LessonContentBlocks(currentLesson.content)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            contentApiClient.completeLesson(currentLesson.id)
                                .onSuccess { completed = true }
                                .onFailure { errorMessage = "Nao consegui salvar a conclusao agora." }
                        }
                    },
                    enabled = !completed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (completed) "Aula concluida" else "Marcar como concluida")
                }

                Button(
                    onClick = onStartChat,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text("Conversar sobre esta aula")
                }

                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage ?: "Aula nao encontrada")
            }
        }
        }
    }
}

@Composable
private fun LessonContentBlocks(content: String) {
    val blocks = content.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            val title = block.substringBefore(":", missingDelimiterValue = "")
            val hasShortTitle = title.length in 3..28 && !title.contains(".")
            if (hasShortTitle) {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = block.substringAfter(":").trim(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text(text = block, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun VisualDemoCard(lesson: Lesson) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Exemplo visual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            DimensionalLessonVisual(lesson.id)
        }
    }
}

@Composable
private fun DimensionalLessonVisual(lessonId: String) {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val primary = Color(0xFF2454D6)
        val amber = Color(0xFFFFC857)
        val coral = Color(0xFFFF6B5F)
        drawRoundRect(
            color = Color(0xFFDDEAFF),
            topLeft = Offset(24f, 20f),
            size = androidx.compose.ui.geometry.Size(size.width - 48f, 110f)
        )
        when (lessonId) {
            "formulas", "exemplos", "treino" -> {
                drawLine(primary, Offset(54f, 62f), Offset(size.width * 0.34f, 62f), strokeWidth = 8f)
                drawLine(amber, Offset(size.width * 0.43f, 44f), Offset(size.width * 0.43f, 80f), strokeWidth = 6f)
                drawLine(amber, Offset(size.width * 0.39f, 62f), Offset(size.width * 0.47f, 62f), strokeWidth = 6f)
                drawLine(primary, Offset(size.width * 0.56f, 50f), Offset(size.width - 54f, 50f), strokeWidth = 8f)
                drawLine(primary.copy(alpha = 0.55f), Offset(size.width * 0.56f, 82f), Offset(size.width - 86f, 82f), strokeWidth = 8f)
                drawCircle(coral, radius = 13f, center = Offset(size.width - 58f, 82f))
            }
            "metodo" -> {
                repeat(4) { index ->
                    val x = 60f + index * ((size.width - 120f) / 3f)
                    drawCircle(if (index == 0) amber else primary, radius = 18f, center = Offset(x, 74f))
                    if (index < 3) {
                        drawLine(primary.copy(alpha = 0.55f), Offset(x + 22f, 74f), Offset(x + ((size.width - 120f) / 3f) - 22f, 74f), strokeWidth = 5f)
                    }
                }
            }
            else -> {
                drawCircle(amber, radius = 18f, center = Offset(78f, 62f))
                drawCircle(coral, radius = 18f, center = Offset(78f, 98f))
                drawLine(primary, Offset(116f, 62f), Offset(size.width - 62f, 62f), strokeWidth = 7f)
                drawLine(primary.copy(alpha = 0.55f), Offset(116f, 98f), Offset(size.width - 94f, 98f), strokeWidth = 7f)
            }
        }
    }
    Text(
        "Troque cada grandeza por [M], [L] e [T], simplifique e compare os dois lados.",
        style = MaterialTheme.typography.labelMedium
    )
}
