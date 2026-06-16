package com.example.testes.model

data class DailyQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val subjectId: String,
    val subjectName: String,
    val difficulty: String,
    val topic: String
)

data class DailyChallengeStatus(
    val completedToday: Boolean,
    val score: Int? = null,
    val total: Int? = null,
    val accuracyRate: Int? = null,
    val completedAt: String? = null
)

data class CampaignNode(
    val id: String,
    val title: String,
    val description: String,
    val subjectId: String,
    val subjectName: String,
    val progress: Float,
    val isUnlocked: Boolean,
    val stageLabel: String,
    val visualType: String,
    val exercises: List<CampaignExercise>,
    val usesCampaignProgress: Boolean = true
)

data class CampaignExercise(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val visualType: String,
    val difficulty: String = "Media",
    val topic: String = "Analise Dimensional"
)

data class AvatarItem(
    val id: String,
    val category: String,
    val name: String
)
