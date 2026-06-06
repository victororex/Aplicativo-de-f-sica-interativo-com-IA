package com.example.testes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.LearningApiClient
import com.example.testes.model.CampaignExercise
import com.example.testes.model.CampaignNode
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import kotlinx.coroutines.launch

@Composable
fun StudyCampaignScreen(
    onBackClick: () -> Unit,
    onSubjectClick: (String) -> Unit,
    learningApiClient: LearningApiClient = LearningApiClient()
) {
    var nodes by remember { mutableStateOf<List<CampaignNode>>(emptyList()) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        learningApiClient.getCampaign()
            .onSuccess {
                nodes = it
                selectedNodeId = it.firstOrNull { node -> node.isUnlocked && node.progress < 1f }?.id
                    ?: it.firstOrNull { node -> node.isUnlocked }?.id
                errorMessage = null
            }
            .onFailure { errorMessage = "Nao consegui mostrar sua trilha agora." }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Campanha", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                errorMessage != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(errorMessage ?: "Nao foi possivel carregar a campanha.")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        AppHeroPanel(
                            title = "Mapa de fases",
                            subtitle = "Avance por desafios visuais da materia e prove que entendeu a cena."
                        )
                    }
                    itemsIndexed(nodes) { index, node ->
                        CampaignGameStage(
                            index = index,
                            node = node,
                            isLast = index == nodes.lastIndex,
                            isSelected = selectedNodeId == node.id,
                            onSelect = { if (node.isUnlocked) selectedNodeId = node.id },
                            learningApiClient = learningApiClient,
                            onStageCompleted = { completedNode, score, total ->
                                scope.launch {
                                    learningApiClient.submitCampaignStage(completedNode.id, score, total)
                                    nodes = nodes.unlockAfter(completedNode.id, score, total)
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CampaignGameStage(
    index: Int,
    node: CampaignNode,
    isLast: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    learningApiClient: LearningApiClient,
    onStageCompleted: (CampaignNode, Int, Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        StageRail(index = index, node = node, isLast = isLast)
        Card(
            onClick = onSelect,
            enabled = node.isUnlocked,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (node.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            imageVector = if (node.isUnlocked) Icons.Default.SportsEsports else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (node.isUnlocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(node.stageLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(node.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(
                        if (node.progress >= 1f) Icons.Default.CheckCircle else if (node.isUnlocked) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (node.progress >= 1f) Color(0xFF00A99D) else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(node.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!node.isUnlocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Conclua a etapa anterior", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { node.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF00A99D),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("${(node.progress * 100).toInt()}% concluido", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                AnimatedVisibility(visible = isSelected && node.isUnlocked) {
                    StageMission(
                        node = node.withFallbackExercises(),
                        learningApiClient = learningApiClient,
                        onStageCompleted = onStageCompleted
                    )
                }
            }
        }
    }
}

@Composable
private fun StageRail(index: Int, node: CampaignNode, isLast: Boolean) {
    Box(
        modifier = Modifier.width(48.dp).height(if (node.isUnlocked) 360.dp else 160.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            if (!isLast) {
                drawLine(
                    color = Color(0xFF00A99D).copy(alpha = if (node.isUnlocked) 0.58f else 0.18f),
                    start = Offset(centerX, 48f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }
        Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.medium,
            color = if (node.isUnlocked) Color(0xFF00A99D) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (node.isUnlocked) {
                    Text("${index + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StageMission(
    node: CampaignNode,
    learningApiClient: LearningApiClient,
    onStageCompleted: (CampaignNode, Int, Int) -> Unit
) {
    val exercises = node.exercises
    var exerciseIndex by remember(node.id) { mutableIntStateOf(0) }
    var selectedOption by remember(node.id, exerciseIndex) { mutableIntStateOf(-1) }
    var correctAnswers by remember(node.id) { mutableIntStateOf(0) }
    var savedCompletion by remember(node.id) { mutableStateOf(false) }
    val exercise = exercises.getOrNull(exerciseIndex)
    val answered = selectedOption >= 0
    val correct = exercise != null && selectedOption == exercise.correctIndex
    val completed = exerciseIndex >= exercises.size

    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (completed) {
            if (!savedCompletion) {
                savedCompletion = true
                onStageCompleted(node, correctAnswers, exercises.size)
            }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color(0xFFE3F8F3),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00A99D), modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Fase concluida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "$correctAnswers de ${exercises.size} desafios certos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF006B5E)
                    )
                }
            }
            return@Column
        }

        if (exercise == null) {
            Text("Nenhum desafio disponivel para esta fase.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Text(
            "Desafio ${exerciseIndex + 1} de ${exercises.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        ElevatedCard(shape = MaterialTheme.shapes.large, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Exercicio visual", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                CampaignVisual(type = exercise.visualType)
                Spacer(modifier = Modifier.height(10.dp))
                Text(exercise.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        exercise.options.forEachIndexed { index, option ->
            OutlinedButton(
                onClick = { if (!answered) selectedOption = index },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(option)
            }
        }

        AnimatedVisibility(visible = answered) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (correct) Color(0xFFE3F8F3) else Color(0xFFFFEFE9)
            ) {
                Text(
                    text = if (correct) "Boa! ${exercise.explanation}" else "Quase. ${exercise.explanation}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (correct) Color(0xFF006B5E) else Color(0xFF9A3412)
                )
            }
        }

        Button(
            onClick = {
                if (correct) {
                    correctAnswers += 1
                    exerciseIndex += 1
                }
                selectedOption = -1
            },
            enabled = answered,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                when {
                    !answered -> "Escolha uma resposta"
                    !correct -> "Tentar de novo"
                    exerciseIndex == exercises.lastIndex -> "Concluir fase"
                    else -> "Proximo desafio"
                }
            )
        }
    }
}

private fun CampaignNode.withFallbackExercises(): CampaignNode {
    if (exercises.isNotEmpty()) return this
    return copy(
        exercises = fallbackCampaignExercises(subjectId, visualType),
        progress = 0f,
        usesCampaignProgress = false
    )
}

private fun List<CampaignNode>.unlockAfter(nodeId: String, score: Int, total: Int): List<CampaignNode> {
    val completedIndex = indexOfFirst { it.id == nodeId }
    if (completedIndex < 0) return this
    return mapIndexed { index, node ->
        when {
            index == completedIndex -> node.copy(progress = if (total > 0) score.toFloat() / total else 1f, isUnlocked = true)
            index == completedIndex + 1 -> node.copy(isUnlocked = true)
            else -> node
        }
    }
}

private fun fallbackCampaignExercises(subjectId: String, visualType: String): List<CampaignExercise> {
    return when (subjectId) {
        "mecanica" -> listOf(
            CampaignExercise("mecanica-1", "Qual bloco muda de movimento com mais facilidade?", listOf("O mais leve", "O mais pesado", "Os dois iguais"), 0, "Com o mesmo empurrao, o mais leve acelera mais.", "motion"),
            CampaignExercise("mecanica-2", "Se o bloco nao se mexe, o que pode estar acontecendo?", listOf("As forcas se equilibram", "Ele perdeu a massa", "O tempo parou"), 0, "Quando as forcas se equilibram, o movimento nao muda.", "motion"),
            CampaignExercise("mecanica-3", "Linha mais inclinada no movimento indica:", listOf("Maior rapidez", "Mais calor", "Menos luz"), 0, "A inclinacao mostra que a rapidez cresce mais.", "motion")
        )
        "termologia" -> listOf(
            CampaignExercise("termologia-1", "Para onde o calor vai?", listOf("Do quente para o frio", "Do frio para o quente", "Para lugar nenhum"), 0, "O calor vai naturalmente do mais quente para o mais frio.", "heat"),
            CampaignExercise("termologia-2", "Linha reta durante aquecimento pode indicar:", listOf("Mudanca de estado", "Massa sumindo", "Luz refletindo"), 0, "A energia esta mudando o estado do material.", "heat"),
            CampaignExercise("termologia-3", "Quem perde calor primeiro?", listOf("O mais quente", "O mais frio", "Os dois frios"), 0, "O objeto mais quente entrega energia.", "heat")
        )
        "ondulatoria" -> listOf(
            CampaignExercise("ondulatoria-1", "Som mais agudo aparece como:", listOf("Onda mais apertada", "Onda mais espalhada", "Linha parada"), 0, "Mais repeticoes em menos tempo indicam som mais agudo.", "wave"),
            CampaignExercise("ondulatoria-2", "Crista mais alta indica:", listOf("Mais intensidade", "Mais peso", "Mais frio"), 0, "A altura mostra a intensidade da onda.", "wave"),
            CampaignExercise("ondulatoria-3", "Distancia entre cristas mostra:", listOf("Tamanho da onda", "Peso da onda", "Cor da onda"), 0, "Essa distancia e o comprimento da onda.", "wave")
        )
        "optica" -> listOf(
            CampaignExercise("optica-1", "Quando caminhos da luz se encontram:", listOf("A imagem aparece", "A luz some", "O objeto pesa mais"), 0, "A imagem se forma onde os raios se encontram.", "light"),
            CampaignExercise("optica-2", "No espelho, a luz:", listOf("Volta em outro caminho", "Sempre vira calor", "Perde tudo"), 0, "A luz reflete.", "light"),
            CampaignExercise("optica-3", "A lente ajuda porque:", listOf("Muda o caminho da luz", "Corta o objeto", "Aumenta a massa"), 0, "A lente desvia a luz para formar imagem.", "light")
        )
        "eletromagnetismo" -> listOf(
            CampaignExercise("eletro-1", "A lampada acende quando:", listOf("O caminho esta fechado", "O fio esta cortado", "Nao ha energia"), 0, "A energia precisa circular pelo caminho completo.", "circuit"),
            CampaignExercise("eletro-2", "Se o fio e cortado:", listOf("A lampada apaga", "Brilha mais", "Vira ima"), 0, "Sem caminho completo, a energia nao circula.", "circuit"),
            CampaignExercise("eletro-3", "A bateria serve para:", listOf("Empurrar a energia", "Esfriar a lampada", "Cortar o fio"), 0, "A bateria faz a energia circular.", "circuit")
        )
        "fisica-moderna" -> listOf(
            CampaignExercise("moderna-1", "No centro do atomo fica:", listOf("O nucleo", "Uma lampada", "Uma onda sonora"), 0, "O nucleo fica no centro.", "atom"),
            CampaignExercise("moderna-2", "Parece invisivel porque estuda coisas:", listOf("Muito pequenas", "Muito barulhentas", "Muito lentas"), 0, "Sao fenomenos pequenos demais para ver diretamente.", "atom"),
            CampaignExercise("moderna-3", "Ao redor do nucleo ha:", listOf("Uma regiao de particulas", "Uma lente", "Um fio cortado"), 0, "O modelo ajuda a imaginar a regiao ao redor do nucleo.", "atom")
        )
        else -> listOf(
            CampaignExercise("$subjectId-1", "Qual e o primeiro passo para entender um problema?", listOf("Observar a situacao", "Chutar", "Pular a leitura"), 0, "Observe a cena antes de responder.", visualType)
        )
    }
}

@Composable
private fun CampaignVisual(type: String) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        when (type) {
            "motion" -> {
                drawRect(Color(0xFF4DD0E1), topLeft = Offset(90f, 78f), size = Size(92f, 48f))
                drawRect(Color(0xFFFFB74D), topLeft = Offset(size.width - 220f, 58f), size = Size(140f, 68f))
                drawLine(primary, Offset(40f, 132f), Offset(size.width - 40f, 132f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawLine(Color(0xFFE53935), Offset(184f, 100f), Offset(270f, 100f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(Color(0xFFE53935), Offset(size.width - 76f, 92f), Offset(size.width - 10f, 92f), strokeWidth = 8f, cap = StrokeCap.Round)
            }
            "heat" -> {
                drawCircle(Color(0xFFE53935), radius = 38f, center = Offset(110f, 78f))
                drawCircle(Color(0xFF42A5F5), radius = 38f, center = Offset(size.width - 110f, 78f))
                drawLine(Color(0xFFFF9800), Offset(158f, 78f), Offset(size.width - 158f, 78f), strokeWidth = 9f, cap = StrokeCap.Round)
            }
            "wave" -> {
                var previous = Offset(20f, 78f)
                for (x in 20..size.width.toInt() - 20 step 7) {
                    val y = 78f + kotlin.math.sin(x / 18f) * 36f
                    val current = Offset(x.toFloat(), y)
                    drawLine(Color(0xFF1976D2), previous, current, strokeWidth = 5f, cap = StrokeCap.Round)
                    previous = current
                }
            }
            "light" -> {
                val lensX = size.width * 0.5f
                drawOval(Color(0x663F51B5), topLeft = Offset(lensX - 18f, 22f), size = Size(36f, 106f))
                drawLine(Color(0xFFFFC107), Offset(40f, 45f), Offset(lensX, 70f), strokeWidth = 5f)
                drawLine(Color(0xFFFFC107), Offset(lensX, 70f), Offset(size.width - 45f, 78f), strokeWidth = 5f)
                drawLine(Color(0xFFFFC107), Offset(40f, 112f), Offset(lensX, 84f), strokeWidth = 5f)
                drawLine(Color(0xFFFFC107), Offset(lensX, 84f), Offset(size.width - 45f, 78f), strokeWidth = 5f)
            }
            "circuit" -> {
                drawRect(Color(0xFF263238), topLeft = Offset(70f, 36f), size = Size(size.width - 140f, 86f), style = Stroke(width = 6f))
                drawCircle(Color(0xFFFFEB3B), radius = 24f, center = Offset(size.width - 115f, 80f))
                drawRect(Color(0xFFFFCC80), topLeft = Offset(95f, 68f), size = Size(52f, 24f))
            }
            "atom" -> {
                val center = Offset(size.width / 2f, 78f)
                drawCircle(Color(0xFFE53935), radius = 20f, center = center)
                drawOval(Color(0x663F51B5), topLeft = Offset(center.x - 120f, center.y - 36f), size = Size(240f, 72f), style = Stroke(width = 4f))
                drawOval(Color(0x663F51B5), topLeft = Offset(center.x - 36f, center.y - 70f), size = Size(72f, 140f), style = Stroke(width = 4f))
                drawCircle(Color(0xFF1976D2), radius = 9f, center = Offset(center.x + 98f, center.y))
            }
            else -> {
                drawRoundRect(Color(0xFFE3F8F3), topLeft = Offset(30f, 30f), size = Size(size.width - 60f, 90f))
                drawLine(primary, Offset(70f, 92f), Offset(size.width - 70f, 58f), strokeWidth = 7f, cap = StrokeCap.Round)
            }
        }
    }
}
