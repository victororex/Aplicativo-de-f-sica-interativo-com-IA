package com.example.testes.data.api

import com.example.testes.data.analytics.AdaptiveLearningEngine
import com.example.testes.data.analytics.AnalyticsSnapshot
import com.example.testes.data.local.LocalBackend
import com.example.testes.model.LearningDashboard
import com.example.testes.model.LearningEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ImprovementStatsResponse(
    val accuracyRate: Int,
    val studyQuality: String,
    val studiedSeconds: Int,
    val completedLessons: Int,
    val totalLessons: Int,
    val answeredExercises: Int,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val dimensionalProgress: Int,
    val questionsAsked: Int,
    val completedPhases: Int,
    val totalPhases: Int,
    val lastLesson: String,
    val weakTopic: String,
    val recommendedDifficulty: String,
    val nextAction: String,
    val averageResponseTimeSeconds: Int,
    val easyAccuracyRate: Int,
    val mediumAccuracyRate: Int,
    val recommendation: String
) {
    val studiedTimeLabel: String
        get() = "${studiedSeconds / 3600}h ${(studiedSeconds % 3600) / 60}min"
}

class StatsApiClient {
    suspend fun getDashboard(): Result<LearningDashboard> = withContext(Dispatchers.IO) {
        runCatching {
            val json = LocalBackend.analyticsSnapshot(SessionManager.accessToken)
            AdaptiveLearningEngine.buildDashboard(json.toSnapshot()).also { dashboard ->
                SessionManager.accessToken?.let { token ->
                    LocalBackend.saveAdaptiveProfile(
                        token,
                        dashboard.adaptiveProfile.fuzzyScore,
                        dashboard.adaptiveProfile.exerciseDifficulty,
                        dashboard.adaptiveProfile.nextTopic,
                        dashboard.adaptiveProfile.learningVelocity
                    )
                }
            }
        }
    }

    suspend fun getImprovementStats(): Result<ImprovementStatsResponse> =
        getDashboard().map { dashboard ->
            val correct = (dashboard.questionsAnswered * dashboard.accuracyRate / 100f).toInt()
            ImprovementStatsResponse(
                accuracyRate = dashboard.accuracyRate,
                studyQuality = when {
                    dashboard.questionsAnswered == 0 -> "Sem dados"
                    dashboard.accuracyRate >= 80 -> "Muito boa"
                    dashboard.accuracyRate >= 60 -> "Boa evolucao"
                    else -> "Em desenvolvimento"
                },
                studiedSeconds = dashboard.totalStudySeconds,
                completedLessons = dashboard.completedLessons,
                totalLessons = dashboard.totalLessons,
                answeredExercises = dashboard.questionsAnswered,
                correctAnswers = correct,
                incorrectAnswers = dashboard.questionsAnswered - correct,
                dimensionalProgress = if (dashboard.totalLessons == 0) 0 else dashboard.completedLessons * 100 / dashboard.totalLessons,
                questionsAsked = dashboard.questionsAsked,
                completedPhases = dashboard.completedPhases,
                totalPhases = dashboard.totalPhases,
                lastLesson = "Consulte o historico",
                weakTopic = dashboard.difficultTopics.firstOrNull()?.topic ?: "Nenhum topico critico",
                recommendedDifficulty = dashboard.adaptiveProfile.exerciseDifficulty,
                nextAction = dashboard.recommendations.firstOrNull()?.action ?: "Continue estudando.",
                averageResponseTimeSeconds = dashboard.averageResponseTimeSeconds,
                easyAccuracyRate = dashboard.topicPerformance.firstOrNull()?.accuracyRate ?: 0,
                mediumAccuracyRate = dashboard.accuracyRate,
                recommendation = dashboard.recommendations.firstOrNull()?.reason ?: "Continue estudando."
            )
        }
}

private fun JSONObject.toSnapshot(): AnalyticsSnapshot {
    val eventsJson = getJSONArray("events")
    val events = List(eventsJson.length()) { index ->
        val event = eventsJson.getJSONObject(index)
        LearningEvent(
            id = event.optString("id", "event-$index"),
            eventType = event.optString("event_type"),
            topic = event.optString("topic", "Analise Dimensional"),
            isCorrect = if (!event.has("is_correct") || event.isNull("is_correct")) null else event.optBoolean("is_correct"),
            difficulty = if (!event.has("difficulty") || event.isNull("difficulty")) null else event.optString("difficulty"),
            responseTimeSeconds = event.optInt("response_time_seconds"),
            timeSpentSeconds = event.optInt("time_spent_seconds"),
            timestamp = event.optLong("timestamp")
        )
    }
    val stats = optJSONObject("stats") ?: JSONObject()
    val chatQuestionsFromEvents = events.count { it.eventType == "chat_question_sent" }
    return AnalyticsSnapshot(
        events = events,
        legacyStudySeconds = stats.optInt("study_seconds"),
        questionsAsked = maxOf(stats.optInt("chat_questions"), chatQuestionsFromEvents),
        completedLessons = optInt("completed_lessons"),
        totalLessons = optInt("total_lessons"),
        completedPhases = optInt("completed_phases"),
        totalPhases = optInt("total_phases")
    )
}
