package com.example.testes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testes.data.mock.MockData
import com.example.testes.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _user = MutableStateFlow(MockData.user)
    val user: StateFlow<User> = _user
    
    private val _recentLessons = MutableStateFlow(MockData.lessons.take(2))
    val recentLessons: StateFlow<List<Lesson>> = _recentLessons

    private val _showQuizPopup = MutableStateFlow(true)
    val showQuizPopup: StateFlow<Boolean> = _showQuizPopup

    fun dismissQuizPopup() {
        _showQuizPopup.value = false
    }

    fun completeQuiz() {
        _showQuizPopup.value = false
    }
}

class LessonsViewModel : ViewModel() {
    private val _lessons = MutableStateFlow(MockData.lessons)
    val lessons: StateFlow<List<Lesson>> = _lessons
}

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow(listOf(
        ChatMessage("1", "Olá! Sou seu assistente de Física. Em que posso ajudar hoje?", false)
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage(System.currentTimeMillis().toString(), text, true)
        _messages.value = _messages.value + userMsg
        
        // Mock IA response
        viewModelScope.launch {
            delay(1000)
            val aiMsg = ChatMessage(
                (System.currentTimeMillis() + 1).toString(),
                "Entendi sua dúvida sobre '$text'. Vamos analisar isso sob a perspectiva das Leis de Newton...",
                false
            )
            _messages.value = _messages.value + aiMsg
        }
    }
}

class ProfileViewModel : ViewModel() {
    private val _user = MutableStateFlow(MockData.user)
    val user: StateFlow<User> = _user
    
    private val _progress = MutableStateFlow(MockData.progress)
    val progress: StateFlow<Progress> = _progress
}