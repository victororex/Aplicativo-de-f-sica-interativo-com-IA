package com.example.testes.model

data class LearningEvent(
    val id: String,
    val eventType: String,
    val topic: String,
    val isCorrect: Boolean?,
    val difficulty: String?,
    val responseTimeSeconds: Int,
    val timeSpentSeconds: Int,
    val timestamp: Long
)

data class EvolutionPoint(
    val label: String,
    val accuracyRate: Int,
    val studySeconds: Int,
    val questions: Int
)

data class TopicPerformance(
    val topic: String,
    val attempts: Int,
    val correctAnswers: Int,
    val accuracyRate: Int,
    val averageResponseTimeSeconds: Int,
    val masteryScore: Int,
    val needsReview: Boolean
)

data class PerformanceHistoryItem(
    val id: String,
    val topic: String,
    val activity: String,
    val result: String,
    val difficulty: String,
    val responseTimeSeconds: Int,
    val timestamp: Long
)

data class StudyRecommendation(
    val title: String,
    val reason: String,
    val topic: String,
    val action: String,
    val priority: Int
)

data class AdaptiveProfile(
    val explanationLevel: String,
    val exerciseDifficulty: String,
    val trend: String,
    val nextTopic: String,
    val reviewTopics: List<String>,
    val suggestedQuestions: List<String>
)

data class LearningDashboard(
    val totalStudySeconds: Int,
    val questionsAnswered: Int,
    val questionsAsked: Int,
    val completedSessions: Int,
    val completedLessons: Int,
    val totalLessons: Int,
    val completedPhases: Int,
    val totalPhases: Int,
    val topicsStudied: List<String>,
    val difficultTopics: List<TopicPerformance>,
    val topicPerformance: List<TopicPerformance>,
    val dailyEvolution: List<EvolutionPoint>,
    val weeklyEvolution: List<EvolutionPoint>,
    val monthlyEvolution: List<EvolutionPoint>,
    val accuracyRate: Int,
    val averageResponseTimeSeconds: Int,
    val performanceHistory: List<PerformanceHistoryItem>,
    val recommendations: List<StudyRecommendation>,
    val adaptiveProfile: AdaptiveProfile
) {
    val studiedTimeLabel: String
        get() {
            val hours = totalStudySeconds / 3600
            val minutes = (totalStudySeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
        }
}
