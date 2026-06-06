package com.example.testes.model

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val module: String,
    val subjectId: String = "",
    val examTags: String = "",
    val isCompleted: Boolean = false
)
