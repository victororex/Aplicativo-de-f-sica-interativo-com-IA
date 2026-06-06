package com.example.testes.model

data class Subject(
    val id: String,
    val name: String,
    val description: String,
    val examFocus: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val progress: Float,
    val isCompleted: Boolean
)
