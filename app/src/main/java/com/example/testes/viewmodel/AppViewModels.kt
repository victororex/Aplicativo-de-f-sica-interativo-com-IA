package com.example.testes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testes.data.api.ChatApiClient
import com.example.testes.data.api.ContentApiClient
import com.example.testes.data.api.LearningApiClient
import com.example.testes.data.api.QuestionRequest
import com.example.testes.data.api.SessionManager
import com.example.testes.model.ChatMessage
import com.example.testes.model.Lesson
import com.example.testes.model.Progress
import com.example.testes.model.Subject
import com.example.testes.model.User
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

    private val _showQuizPopup = MutableStateFlow(false)
    val showQuizPopup: StateFlow<Boolean> = _showQuizPopup

    init {
        loadHomeData()
        loadDailyChallengeStatus()
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
        }
    }

    private fun loadDailyChallengeStatus() {
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
                .onFailure { _errorMessage.value = "Nao consegui mostrar as materias agora." }
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
                .onFailure { _errorMessage.value = "Nao consegui mostrar as aulas agora." }
            _isLoading.value = false
        }
    }
}

class ChatViewModel(
    private val chatApiClient: ChatApiClient = ChatApiClient()
) : ViewModel() {
    private val _messages = MutableStateFlow(
        listOf(ChatMessage("1", "Ola! Sou seu tutor de Fisica. Em que posso ajudar hoje?", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(System.currentTimeMillis().toString(), text, true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            val response = chatApiClient.sendMessage(
                QuestionRequest(
                    message = text,
                    subject = "Fisica",
                    level = "universitario"
                )
            )
            val answer = response.getOrNull()?.aiResponse
                ?: "Nao consegui responder agora. Tente novamente em alguns instantes."

            val aiMsg = ChatMessage((System.currentTimeMillis() + 1).toString(), answer, false)
            _messages.value = _messages.value + aiMsg
        }
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

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            contentApiClient.getCurrentUser().onSuccess { _user.value = it }
            contentApiClient.getProgress().onSuccess { _progress.value = it }
            contentApiClient.getSubjects().onSuccess { _subjects.value = it }
        }
    }
}
