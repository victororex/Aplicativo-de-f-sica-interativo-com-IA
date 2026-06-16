package com.example.testes.model

enum class SectionStyle { Plain, Formula, Tip }

data class MessageSection(
    val icon: String,
    val title: String,
    val body: String,
    val style: SectionStyle = SectionStyle.Plain
)

data class RelatedLesson(
    val lessonId: String,
    val title: String,
    val module: String
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val sections: List<MessageSection> = emptyList(),
    val relatedLesson: RelatedLesson? = null,
    val isAnalyzing: Boolean = false
)
