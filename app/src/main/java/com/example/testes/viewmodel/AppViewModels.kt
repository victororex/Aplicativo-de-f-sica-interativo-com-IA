package com.example.testes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testes.data.api.ChatApiClient
import com.example.testes.data.api.ContentApiClient
import com.example.testes.data.api.FormulaApiClient
import com.example.testes.data.api.LearningApiClient
import com.example.testes.data.api.QuestionRequest
import com.example.testes.data.api.SessionManager
import com.example.testes.data.api.StatsApiClient
import com.example.testes.data.local.LocalBackend
import com.example.testes.data.state.AppEvent
import com.example.testes.data.state.AppStateBus
import com.example.testes.model.ChatMessage
import com.example.testes.model.FormulaAnalysis
import com.example.testes.model.Lesson
import com.example.testes.model.LearningDashboard
import com.example.testes.model.MessageSection
import com.example.testes.model.Progress
import com.example.testes.model.RelatedLesson
import com.example.testes.model.SectionStyle
import com.example.testes.model.Subject
import com.example.testes.model.User
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val contentApiClient: ContentApiClient = ContentApiClient(),
    private val learningApiClient: LearningApiClient = LearningApiClient()
) : ViewModel() {
    private val _user = MutableStateFlow(SessionManager.user?.let {
        User(it.id.toString(), it.name, it.email, it.phone, it.privateAccount, it.notificationsEnabled)
    } ?: User("", "Aluno", ""))
    val user: StateFlow<User> = _user

    private val _recentLessons = MutableStateFlow<List<Lesson>>(emptyList())
    val recentLessons: StateFlow<List<Lesson>> = _recentLessons

    private val _progress = MutableStateFlow(Progress("", emptyList(), "", 0f))
    val progress: StateFlow<Progress> = _progress

    private val _showQuizPopup = MutableStateFlow(false)
    val showQuizPopup: StateFlow<Boolean> = _showQuizPopup

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp

    private val _level = MutableStateFlow(0)
    val level: StateFlow<Int> = _level

    private val _trailProgress = MutableStateFlow(0f)
    val trailProgress: StateFlow<Float> = _trailProgress

    init {
        loadHomeData()
        loadDailyChallengeStatus()
        viewModelScope.launch {
            AppStateBus.events.collect { event ->
                when (event) {
                    AppEvent.LessonCompleted,
                    AppEvent.DailyChallengeSubmitted,
                    AppEvent.CampaignStageSubmitted,
                    AppEvent.ChatMessageSent,
                    AppEvent.OcrAnalyzed,
                    AppEvent.ProfileUpdated,
                    AppEvent.TrackMissionCompleted,
                    AppEvent.DailyChallengeInstanceSubmitted -> {
                        loadHomeData()
                        loadDailyChallengeStatus()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            contentApiClient.getCurrentUser().onSuccess { _user.value = it }
            contentApiClient.getSubjects().onSuccess { subjects ->
                subjects.firstOrNull()?.let { firstSubject ->
                    contentApiClient.getLessons(firstSubject.id).onSuccess { lessons ->
                        _recentLessons.value = lessons.take(2)
                    }
                }
            }
            contentApiClient.getProgress().onSuccess { _progress.value = it }
            SessionManager.accessToken?.let { token ->
                _xp.value = LocalBackend.computeXp(token)
                _level.value = LocalBackend.computeLevel(token)
                // Trilha em progresso = trilha nova de Análise Dimensional, se disponível.
                val trackTotal = LocalBackend.dimensionalTrackTotal()
                if (trackTotal > 0) {
                    _trailProgress.value = LocalBackend.dimensionalTrackRatio(token).toFloat()
                } else {
                    // Fallback: ainda usa a campanha antiga até o MissionViewModel carregar a trilha.
                    learningApiClient.getCampaign().onSuccess { nodes ->
                        _trailProgress.value = if (nodes.isEmpty()) 0f
                            else nodes.sumOf { it.progress.toDouble() }.toFloat() / nodes.size
                    }
                }
            }
        }
    }

    fun loadDailyChallengeStatus() {
        viewModelScope.launch {
            learningApiClient.getDailyChallengeStatus()
                .onSuccess { status ->
                    _showQuizPopup.value = !status.completedToday
                }
                .onFailure {
                    _showQuizPopup.value = false
                }
        }
    }

    fun dismissQuizPopup() {
        _showQuizPopup.value = false
    }

    fun completeQuiz() {
        _showQuizPopup.value = false
    }
}

class LessonsViewModel(
    private val contentApiClient: ContentApiClient = ContentApiClient()
) : ViewModel() {
    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    private val _selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedSubject: StateFlow<Subject?> = _selectedSubject

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            contentApiClient.getSubjects()
                .onSuccess {
                    _subjects.value = it
                    _errorMessage.value = null
                }
                .onFailure { _errorMessage.value = "Não consegui mostrar as aulas agora." }
            _isLoading.value = false
        }
    }

    fun loadLessons(subjectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedSubject.value = _subjects.value.find { it.id == subjectId }
            contentApiClient.getLessons(subjectId)
                .onSuccess {
                    _lessons.value = it
                    _errorMessage.value = null
                }
                .onFailure { _errorMessage.value = "Não consegui mostrar as aulas agora." }
            _isLoading.value = false
        }
    }
}

enum class AttachmentKind { Camera, Gallery, Pdf }

class ChatViewModel(
    private val chatApiClient: ChatApiClient = ChatApiClient(),
    private val statsApiClient: StatsApiClient = StatsApiClient(),
    private val formulaApiClient: FormulaApiClient = FormulaApiClient(),
    private val contentApiClient: ContentApiClient = ContentApiClient()
) : ViewModel() {
    private val greeting = ChatMessage(
        "greeting",
        "Oi! Eu sou o titio Renato. Me mande uma fórmula, unidade ou dúvida de Análise Dimensional que eu explico com calma.",
        false
    )
    private val _messages = MutableStateFlow(
        listOf(greeting)
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _lastFailedPrompt = MutableStateFlow<String?>(null)
    val lastFailedPrompt: StateFlow<String?> = _lastFailedPrompt

    private val _suggestedQuestions = MutableStateFlow(
        listOf(
            "O que e Analise Dimensional?",
            "Qual a dimensao da velocidade?",
            "Como descobrir a dimensao da forca?",
            "Qual a diferenca entre unidade e dimensao?"
        )
    )
    val suggestedQuestions: StateFlow<List<String>> = _suggestedQuestions
    private var adaptiveLevel = "intermediario, com exemplos guiados"
    private var adaptiveTopic = "Analise Dimensional"

    init {
        loadHistory()
        loadAdaptiveProfile()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            chatApiClient.getHistory().onSuccess { history ->
                _messages.value = if (history.isEmpty()) listOf(greeting) else history
            }
        }
    }

    private fun loadAdaptiveProfile() {
        viewModelScope.launch {
            statsApiClient.getDashboard().onSuccess { dashboard ->
                adaptiveLevel = dashboard.adaptiveProfile.explanationLevel
                adaptiveTopic = dashboard.adaptiveProfile.nextTopic
                _suggestedQuestions.value = dashboard.adaptiveProfile.suggestedQuestions
            }
        }
    }

    fun sendMessage(
        text: String,
        contextTitle: String? = null,
        contextTopic: String? = null,
        source: String? = "chat"
    ) {
        if (text.isBlank() || _isSending.value) return

        val now = System.currentTimeMillis()
        val userMsg = ChatMessage("user-$now-${_messages.value.size}", text.trim(), true, now)
        _messages.value = _messages.value + userMsg
        _isSending.value = true
        _lastFailedPrompt.value = null
        _statusMessage.value = "Titio Renato esta pensando..."

        viewModelScope.launch {
            val response = chatApiClient.sendMessage(
                QuestionRequest(
                    message = text.trim(),
                    subject = "Análise Dimensional",
                    level = adaptiveLevel,
                    contextTitle = contextTitle,
                    contextTopic = contextTopic ?: adaptiveTopic,
                    source = source
                )
            )
            val responseValue = response.getOrNull()
            val answer = responseValue?.aiResponse
                ?: "Não consegui responder agora porque a conexão com a IA falhou. Tente novamente em instantes ou revise a aula de Análise Dimensional."

            val answerTime = System.currentTimeMillis()
            val aiMsg = ChatMessage("ai-$answerTime-${_messages.value.size}", answer, false, answerTime)
            _messages.value = _messages.value + aiMsg
            _statusMessage.value = responseValue?.fallbackReason
            _lastFailedPrompt.value = if (response.isFailure || responseValue?.fallbackReason != null) text.trim() else null
            _isSending.value = false
        }
    }

    fun retryLastFailed() {
        val prompt = _lastFailedPrompt.value ?: return
        sendMessage(prompt)
    }

    /** Envia um anexo (foto/imagem/PDF já convertido para JPG) processando OCR via /formula/analyze. */
    fun sendAttachment(compressedImage: File, displayUri: String, kind: AttachmentKind) {
        if (_isSending.value) return
        val now = System.currentTimeMillis()
        val userMsg = ChatMessage(
            id = "user-att-$now-${_messages.value.size}",
            text = when (kind) {
                AttachmentKind.Camera -> "Tirei uma foto"
                AttachmentKind.Gallery -> "Enviei uma imagem"
                AttachmentKind.Pdf -> "Enviei um PDF"
            },
            isFromUser = true,
            timestamp = now,
            imageUri = displayUri
        )
        val placeholderId = "ai-placeholder-$now"
        val placeholder = ChatMessage(
            id = placeholderId,
            text = "",
            isFromUser = false,
            timestamp = now + 1,
            isAnalyzing = true
        )
        _messages.value = _messages.value + userMsg + placeholder
        _isSending.value = true
        _statusMessage.value = "Renato está analisando…"

        viewModelScope.launch {
            val result = formulaApiClient.analyze(compressedImage, null)
            compressedImage.runCatching { delete() }
            result.onSuccess { analysis ->
                SessionManager.accessToken?.let { token ->
                    val topic = analysis.problemStatement.take(80).ifBlank { "Análise Dimensional" }
                    LocalBackend.recordOcrUsed(token, topic, 0)
                }
                val related = findRelatedLesson(analysis)
                val aiMsg = ChatMessage(
                    id = "ai-att-${System.currentTimeMillis()}",
                    text = analysis.problemStatement.ifBlank { "Aqui está a análise da imagem." },
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    sections = analysisToSections(analysis, kind),
                    relatedLesson = related
                )
                _messages.value = _messages.value.filterNot { it.id == placeholderId } + aiMsg
                _statusMessage.value = null
            }.onFailure { error ->
                val errorMsg = ChatMessage(
                    id = "ai-att-err-${System.currentTimeMillis()}",
                    text = "Não consegui processar a imagem agora. ${error.message ?: ""}".trim(),
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value.filterNot { it.id == placeholderId } + errorMsg
                _statusMessage.value = null
                _lastFailedPrompt.value = null
            }
            _isSending.value = false
        }
    }

    private fun analysisToSections(analysis: FormulaAnalysis, kind: AttachmentKind): List<MessageSection> {
        val sections = mutableListOf<MessageSection>()
        sections += MessageSection(
            icon = when (kind) {
                AttachmentKind.Camera -> "📷"
                AttachmentKind.Gallery -> "🖼"
                AttachmentKind.Pdf -> "📄"
            },
            title = when (kind) {
                AttachmentKind.Pdf -> "PDF recebido"
                else -> "Imagem recebida"
            },
            body = "Reconhecido em ${analysis.steps.size} passo(s)."
        )
        if (analysis.problemStatement.isNotBlank()) {
            sections += MessageSection("🔎", "O que encontrei", analysis.problemStatement)
        }
        if (analysis.latex.isNotBlank()) {
            sections += MessageSection("🧮", "Fórmula", analysis.latex, SectionStyle.Formula)
        }
        if (analysis.steps.isNotEmpty()) {
            val body = analysis.steps.joinToString("\n\n") { s ->
                val explanation = s.explanation.ifBlank { "" }
                val latex = s.latex?.takeIf { it.isNotBlank() }
                buildString {
                    append("• ").append(s.title.ifBlank { "Passo" })
                    if (explanation.isNotBlank()) { append("\n").append(explanation) }
                    if (latex != null) { append("\n").append(latex) }
                }
            }
            sections += MessageSection("📐", "Resolução", body)
        }
        if (analysis.finalAnswer.isNotBlank()) {
            sections += MessageSection("✅", "Resposta", analysis.finalAnswer)
        }
        if (analysis.narrationText.isNotBlank()) {
            sections += MessageSection("💡", "Dica", analysis.narrationText, SectionStyle.Tip)
        }
        return sections
    }

    private suspend fun findRelatedLesson(analysis: FormulaAnalysis): RelatedLesson? {
        val haystack = (analysis.problemStatement + " " + analysis.finalAnswer).lowercase()
        if (haystack.isBlank()) return null
        val subjects = contentApiClient.getSubjects().getOrNull().orEmpty()
        for (subject in subjects) {
            val lessons = contentApiClient.getLessons(subject.id).getOrNull().orEmpty()
            val match = lessons.firstOrNull { lesson ->
                val tokens = lesson.title.lowercase().split(" ").filter { it.length > 3 }
                tokens.any { haystack.contains(it) } ||
                    haystack.contains(subject.name.lowercase().take(8))
            }
            if (match != null) {
                return RelatedLesson(match.id, match.title, subject.name)
            }
        }
        return null
    }
}

class ProfileViewModel(
    private val contentApiClient: ContentApiClient = ContentApiClient()
) : ViewModel() {
    private val _user = MutableStateFlow(SessionManager.user?.let {
        User(it.id.toString(), it.name, it.email, it.phone, it.privateAccount, it.notificationsEnabled)
    } ?: User("", "Aluno", ""))
    val user: StateFlow<User> = _user

    private val _progress = MutableStateFlow(Progress("", emptyList(), "", 0f))
    val progress: StateFlow<Progress> = _progress

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    private val _xp = MutableStateFlow(0)
    val xp: StateFlow<Int> = _xp

    private val _level = MutableStateFlow(0)
    val level: StateFlow<Int> = _level

    init {
        loadProfile()
        viewModelScope.launch {
            AppStateBus.events.collect { loadProfile() }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            contentApiClient.getCurrentUser().onSuccess { _user.value = it }
            contentApiClient.getProgress().onSuccess { _progress.value = it }
            contentApiClient.getSubjects().onSuccess { _subjects.value = it }
            SessionManager.accessToken?.let { token ->
                _xp.value = LocalBackend.computeXp(token)
                _level.value = LocalBackend.computeLevel(token)
            }
        }
    }
}

class DashboardViewModel(
    private val statsApiClient: StatsApiClient = StatsApiClient()
) : ViewModel() {
    private val _dashboard = MutableStateFlow<LearningDashboard?>(null)
    val dashboard: StateFlow<LearningDashboard?> = _dashboard

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        load()
        viewModelScope.launch {
            AppStateBus.events.collect { load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                kotlinx.coroutines.withTimeout(10_000L) {
                    statsApiClient.getDashboard().getOrThrow()
                }
            }.onSuccess {
                _dashboard.value = it
                _errorMessage.value = null
            }.onFailure {
                _errorMessage.value = "Não foi possível calcular seu painel agora."
            }
            _isLoading.value = false
        }
    }
}
