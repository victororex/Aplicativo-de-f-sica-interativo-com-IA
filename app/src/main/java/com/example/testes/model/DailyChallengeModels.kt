package com.example.testes.model

import java.time.LocalDate

enum class Difficulty { EASY, MEDIUM, HARD, MASTER }

data class DailyChallengeDefinition(
    val id: String,
    val title: String,
    val difficulty: Difficulty,
    val minMissionIndex: Int,
    val maxMissionIndex: Int,
    val numQuestions: Int,
    val timeLimitSeconds: Int,
    val rewardXp: Int
)

data class DailyDimensionalQuestion(
    val id: String,
    val difficulty: Difficulty,
    val missionIndexRange: List<Int>,
    val statement: String,
    val options: List<String>,
    val correctIndex: Int,
    val tags: List<String> = emptyList()
)

data class DailyChallengeInstance(
    val instanceId: String,
    val definition: DailyChallengeDefinition,
    val questions: List<DailyDimensionalQuestion>,
    val date: LocalDate,
    val completed: Boolean = false,
    val lastScore: Int = 0
)

val dimensionalDailyChallenges: List<DailyChallengeDefinition> = listOf(
    DailyChallengeDefinition(
        id = "daily_easy_dimensional",
        title = "Desafio Diário – Base Dimensional",
        difficulty = Difficulty.EASY,
        minMissionIndex = 1,
        maxMissionIndex = 3,
        numQuestions = 5,
        timeLimitSeconds = 300,
        rewardXp = 20
    ),
    DailyChallengeDefinition(
        id = "daily_medium_dimensional",
        title = "Desafio Diário – Ferramentas Dimensionais",
        difficulty = Difficulty.MEDIUM,
        minMissionIndex = 1,
        maxMissionIndex = 6,
        numQuestions = 7,
        timeLimitSeconds = 420,
        rewardXp = 40
    ),
    DailyChallengeDefinition(
        id = "daily_hard_dimensional",
        title = "Desafio Diário – Forja e Grupos Π",
        difficulty = Difficulty.HARD,
        minMissionIndex = 4,
        maxMissionIndex = 7,
        numQuestions = 8,
        timeLimitSeconds = 600,
        rewardXp = 70
    ),
    DailyChallengeDefinition(
        id = "daily_master_dimensional",
        title = "Desafio Mestre – Universo Dimensional",
        difficulty = Difficulty.MASTER,
        minMissionIndex = 4,
        maxMissionIndex = 8,
        numQuestions = 10,
        timeLimitSeconds = 900,
        rewardXp = 120
    )
)
