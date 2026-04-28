package com.example.testes.model

data class Progress(
    val userId: String,
    val completedLessons: List<String>,
    val currentModule: String,
    val overallCompletion: Float // 0.0 to 1.0
)