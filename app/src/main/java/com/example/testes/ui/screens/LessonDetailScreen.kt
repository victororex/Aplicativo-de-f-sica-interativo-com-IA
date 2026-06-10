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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
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
            when (lesson.subjectId) {
                "analise-dimensional" -> DimensionalLessonVisual(lesson.id)
                "mecanica" -> MechanicsVisual(lesson.id)
                "termologia" -> ThermalVisual()
                "ondulatoria" -> WaveVisual()
                "optica" -> OpticsVisual()
                "eletromagnetismo" -> CircuitVisual()
                "fisica-moderna" -> ModernPhysicsVisual()
                else -> GenericVisual()
            }
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

@Composable
private fun MechanicsVisual(lessonId: String) {
    if (lessonId == "leis-de-newton") {
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val center = Offset(size.width * 0.45f, size.height * 0.55f)
            drawRect(Color(0xFF90CAF9), topLeft = Offset(center.x - 60, center.y - 35), size = androidx.compose.ui.geometry.Size(120f, 70f))
            drawLine(Color(0xFFE53935), center, Offset(center.x + 170, center.y), strokeWidth = 8f)
            drawLine(Color(0xFF43A047), center, Offset(center.x, center.y - 90), strokeWidth = 8f)
            drawLine(Color(0xFF5E35B1), center, Offset(center.x, center.y + 90), strokeWidth = 8f)
        }
        Text("Um bloco sendo empurrado para o lado enquanto o chao sustenta o peso.", style = MaterialTheme.typography.labelMedium)
    } else {
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val baseY = size.height * 0.8f
            drawLine(Color.Gray, Offset(40f, baseY), Offset(size.width - 40f, baseY), strokeWidth = 4f)
            drawLine(Color(0xFF1976D2), Offset(80f, baseY - 10), Offset(size.width - 80f, baseY - 110), strokeWidth = 8f)
            drawCircle(Color(0xFFE53935), radius = 14f, center = Offset(size.width - 80f, baseY - 110))
        }
        Text("Linha de movimento: quanto mais inclinada, mais rapido o objeto esta.", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ThermalVisual() {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val left = 70f
        val bottom = size.height - 25f
        drawLine(Color.Gray, Offset(left, bottom), Offset(size.width - 40f, bottom), strokeWidth = 4f)
        drawLine(Color.Gray, Offset(left, bottom), Offset(left, 20f), strokeWidth = 4f)
        drawLine(Color(0xFFE53935), Offset(left, bottom - 10), Offset(size.width * 0.38f, bottom - 85), strokeWidth = 7f)
        drawLine(Color(0xFFE53935), Offset(size.width * 0.38f, bottom - 85), Offset(size.width * 0.62f, bottom - 85), strokeWidth = 7f)
        drawLine(Color(0xFFE53935), Offset(size.width * 0.62f, bottom - 85), Offset(size.width - 50f, bottom - 125), strokeWidth = 7f)
    }
    Text("Quando a linha sobe, a temperatura aumenta. Quando fica reta, acontece uma mudanca de estado.", style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun WaveVisual() {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val mid = size.height / 2
        var previous = Offset(0f, mid)
        for (x in 0..size.width.toInt() step 8) {
            val y = mid + kotlin.math.sin(x / 34f) * 38f
            val current = Offset(x.toFloat(), y)
            drawLine(Color(0xFF1976D2), previous, current, strokeWidth = 5f)
            previous = current
        }
        drawLine(Color.Gray, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2f)
    }
    Text("A distancia entre dois pontos altos mostra o tamanho de uma onda.", style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun OpticsVisual() {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val lensX = size.width * 0.52f
        drawOval(Color(0x663F51B5), topLeft = Offset(lensX - 18, 20f), size = androidx.compose.ui.geometry.Size(36f, size.height - 40f))
        drawLine(Color(0xFFFFC107), Offset(25f, 40f), Offset(lensX, 70f), strokeWidth = 5f)
        drawLine(Color(0xFFFFC107), Offset(lensX, 70f), Offset(size.width - 35f, size.height * 0.5f), strokeWidth = 5f)
        drawLine(Color(0xFFFFC107), Offset(25f, 110f), Offset(lensX, 80f), strokeWidth = 5f)
        drawLine(Color(0xFFFFC107), Offset(lensX, 80f), Offset(size.width - 35f, size.height * 0.5f), strokeWidth = 5f)
    }
    Text("Os caminhos da luz se encontram no ponto onde a imagem aparece.", style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun CircuitVisual() {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val c = Color(0xFF263238)
        drawRect(c, topLeft = Offset(70f, 35f), size = androidx.compose.ui.geometry.Size(size.width - 140f, 80f), style = Stroke(width = 5f))
        drawRect(Color(0xFFFFCC80), topLeft = Offset(size.width * 0.45f, 25f), size = androidx.compose.ui.geometry.Size(70f, 25f))
        drawCircle(Color(0xFFFFEB3B), radius = 18f, center = Offset(size.width - 110f, 75f))
        drawLine(Color(0xFFE53935), Offset(45f, 115f), Offset(95f, 115f), strokeWidth = 8f)
    }
    Text("Quando o caminho esta fechado, a energia circula e acende a lampada.", style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun ModernPhysicsVisual() {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val center = Offset(size.width * 0.5f, size.height * 0.55f)
        drawCircle(Color(0xFFE53935), radius = 18f, center = center)
        drawOval(Color(0x553F51B5), topLeft = Offset(center.x - 120, center.y - 35), size = androidx.compose.ui.geometry.Size(240f, 70f), style = Stroke(width = 4f))
        drawOval(Color(0x553F51B5), topLeft = Offset(center.x - 35, center.y - 85), size = androidx.compose.ui.geometry.Size(70f, 170f), style = Stroke(width = 4f))
        drawCircle(Color(0xFF1976D2), radius = 9f, center = Offset(center.x + 100, center.y))
    }
    Text("Uma visao simples do centro do atomo e das particulas ao redor.", style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun GenericVisual() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Observe as informacoes e acompanhe cada passo com calma.",
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace
        )
    }
}
