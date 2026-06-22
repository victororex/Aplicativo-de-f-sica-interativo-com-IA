package com.example.testes.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testes.model.MissionContentBlock
import com.example.testes.model.MissionDetail
import com.example.testes.model.MissionQuestion
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.EmptyState
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.MissionCompleted
import com.example.testes.ui.theme.Spacing
import com.example.testes.viewmodel.MissionViewModel
import kotlin.math.abs

@Composable
fun MissionDetailScreenRoute(
    missionId: String,
    onBack: () -> Unit,
    viewModel: MissionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var mission by remember { mutableStateOf<MissionDetail?>(null) }
    LaunchedEffect(missionId, state.isLoading) {
        mission = viewModel.getMissionDetail(missionId)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = mission?.title ?: "Missão", onBackClick = onBack) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                mission == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(title = "Missão não encontrada", body = "Volte e escolha outra missão da trilha.")
                }
                else -> MissionDetailScreen(
                    mission = mission!!,
                    onAnswer = { questionId, correct, elapsed ->
                        viewModel.recordMissionAnswer(mission!!, questionId, correct, elapsed)
                    },
                    onCompleted = { viewModel.markMissionCompleted(missionId); onBack() }
                )
            }
        }
    }
}

@Composable
fun MissionDetailScreen(
    mission: MissionDetail,
    onAnswer: (String, Boolean, Int) -> Unit,
    onCompleted: () -> Unit
) {
    val totalQuestions = mission.questions.size
    val correctCount = remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.md, end = Spacing.md,
            top = Spacing.md, bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "intro") { MissionIntroCard(mission) }
        if (mission.objectives.isNotEmpty()) {
            item(key = "objectives") { ObjectivesCard(mission.objectives) }
        }
        items(mission.contentBlocks.size, key = { idx -> "content-${mission.contentBlocks[idx].hashCode()}" }) { idx ->
            ContentBlockCard(mission.contentBlocks[idx])
        }
        if (mission.questions.isNotEmpty()) {
            item(key = "quiz_header") {
                Text(
                    "Quiz da missão",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
        items(mission.questions.size, key = { idx -> mission.questions[idx].id }) { idx ->
            QuestionCard(
                index = idx + 1,
                question = mission.questions[idx],
                onAnswered = { correct, elapsed ->
                    onAnswer(mission.questions[idx].id, correct, elapsed)
                },
                onCorrect = { correctCount.value += 1 }
            )
        }
        item(key = "footer") {
            FinishButton(
                done = totalQuestions == 0 || correctCount.value >= (totalQuestions + 1) / 2,
                onCompleted = onCompleted
            )
        }
    }
}

@Composable
private fun MissionIntroCard(mission: MissionDetail) {
    Card {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                "Missão ${mission.index}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                mission.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (mission.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    mission.subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ObjectivesCard(objectives: List<String>) {
    Card {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                "Objetivos",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            objectives.forEach { obj ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(
                        "•  ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        obj,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentBlockCard(block: MissionContentBlock) {
    Card {
        Column(Modifier.padding(Spacing.md)) {
            if (block.title.isNotBlank()) {
                Text(
                    block.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
            }
            when (block) {
                is MissionContentBlock.Text -> Text(
                    block.body,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is MissionContentBlock.BulletList -> Column {
                    block.items.forEach { item ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("•  ", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text(item, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    index: Int,
    question: MissionQuestion,
    onAnswered: (Boolean, Int) -> Unit,
    onCorrect: () -> Unit
) {
    var answered by rememberSaveable(question.id) { mutableStateOf(false) }
    var correct by rememberSaveable(question.id) { mutableStateOf(false) }
    val startedAt by rememberSaveable(question.id) { mutableLongStateOf(System.currentTimeMillis()) }

    fun saveAnswer(answerCorrect: Boolean) {
        val elapsed = ((System.currentTimeMillis() - startedAt) / 1000L)
            .coerceAtLeast(1L)
            .toInt()
        onAnswered(answerCorrect, elapsed)
    }

    Card {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                "Pergunta $index",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                question.statement,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))

            when (question) {
                is MissionQuestion.MultipleChoice -> ChoiceList(
                    options = question.options,
                    correctIndex = question.correctIndex,
                    answered = answered,
                    onPick = { i ->
                        if (!answered) {
                            answered = true
                            correct = i == question.correctIndex
                            saveAnswer(correct)
                            if (correct) onCorrect()
                        }
                    }
                )
                is MissionQuestion.TrueFalse -> ChoiceList(
                    options = listOf("Verdadeiro", "Falso"),
                    correctIndex = if (question.correct) 0 else 1,
                    answered = answered,
                    onPick = { i ->
                        if (!answered) {
                            answered = true
                            correct = (i == 0) == question.correct
                            saveAnswer(correct)
                            if (correct) onCorrect()
                        }
                    }
                )
                is MissionQuestion.Numeric -> NumericInput(
                    question = question,
                    answered = answered,
                    onCheck = { value ->
                        answered = true
                        correct = value != null && abs(value - question.answer) <= question.tolerance
                        saveAnswer(correct)
                        if (correct) onCorrect()
                    }
                )
                is MissionQuestion.Open -> OpenInput(
                    question = question,
                    answered = answered,
                    onConfirm = {
                        answered = true
                        correct = true
                        saveAnswer(true)
                        onCorrect()
                    }
                )
                is MissionQuestion.MultiStep -> MultiStepCard(
                    question = question,
                    answered = answered,
                    onConfirm = {
                        answered = true
                        correct = true
                        saveAnswer(true)
                        onCorrect()
                    }
                )
            }

            if (answered) {
                Spacer(Modifier.height(Spacing.sm))
                AnswerFeedback(correct = correct, explanation = question.explanation)
            }
        }
    }
}

@Composable
private fun ChoiceList(
    options: List<String>,
    correctIndex: Int,
    answered: Boolean,
    onPick: (Int) -> Unit
) {
    var picked by rememberSaveable(options, correctIndex) { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        options.forEachIndexed { i, label ->
            val isPicked = picked == i
            val isCorrect = answered && i == correctIndex
            val isWrongPick = answered && isPicked && i != correctIndex
            val border = when {
                isCorrect -> MissionCompleted
                isWrongPick -> MaterialTheme.colorScheme.error
                isPicked -> MaterialTheme.colorScheme.primary
                else -> CardBorder
            }
            OutlinedButton(
                onClick = {
                    if (!answered) { picked = i; onPick(i) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(label, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun NumericInput(
    question: MissionQuestion.Numeric,
    answered: Boolean,
    onCheck: (Double?) -> Unit
) {
    var text by rememberSaveable(question.id) { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(if (question.unit.isBlank()) "Resposta" else "Resposta (${question.unit})") },
            enabled = !answered,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.xs))
        Button(
            onClick = { onCheck(text.replace(',', '.').toDoubleOrNull()) },
            enabled = !answered && text.isNotBlank()
        ) { Text("Verificar") }
    }
}

@Composable
private fun OpenInput(
    question: MissionQuestion.Open,
    answered: Boolean,
    onConfirm: () -> Unit
) {
    var text by rememberSaveable(question.id) { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(question.placeholder.ifBlank { "Sua resposta" }) },
            enabled = !answered,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.xs))
        Button(
            onClick = onConfirm,
            enabled = !answered && text.isNotBlank()
        ) { Text("Enviar resposta") }
    }
}

@Composable
private fun MultiStepCard(
    question: MissionQuestion.MultiStep,
    answered: Boolean,
    onConfirm: () -> Unit
) {
    var checkedSteps by rememberSaveable(question.id) { mutableStateOf<List<Int>>(emptyList()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        question.steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val on = i in checkedSteps
                Surface(
                    shape = CircleShape,
                    color = if (on) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (on) Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedButton(
                onClick = {
                    if (!answered) {
                        checkedSteps = if (i in checkedSteps) checkedSteps - i else checkedSteps + i
                    }
                },
                enabled = !answered
            ) { Text(if (i in checkedSteps) "Refazer passo" else "Marcar como feito") }
        }
        Spacer(Modifier.height(Spacing.xs))
        Button(
            onClick = onConfirm,
            enabled = !answered && question.steps.indices.all { it in checkedSteps }
        ) { Text("Concluir etapas") }
    }
}

@Composable
private fun AnswerFeedback(correct: Boolean, explanation: String) {
    val tint = if (correct) MissionCompleted else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            if (correct) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.size(Spacing.sm))
        Column {
            Text(
                if (correct) "Resposta correta" else "Reveja com cuidado",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
            if (explanation.isNotBlank()) {
                Text(
                    explanation,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinishButton(done: Boolean, onCompleted: () -> Unit) {
    Button(
        onClick = onCompleted,
        enabled = done,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(if (done) "Concluir missão" else "Responda as perguntas para concluir")
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 2.dp
    ) { content() }
}
