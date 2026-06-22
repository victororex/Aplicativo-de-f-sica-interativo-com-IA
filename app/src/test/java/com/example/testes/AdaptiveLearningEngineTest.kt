package com.example.testes

import com.example.testes.data.analytics.AdaptiveLearningEngine
import com.example.testes.data.analytics.AnalyticsSnapshot
import com.example.testes.model.LearningEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLearningEngineTest {
    @Test
    fun weakTopicReducesDifficultyAndCreatesReview() {
        val events = (1..5).map { index ->
            LearningEvent(
                id = index.toString(),
                eventType = "exercise_answered",
                topic = "Forca",
                isCorrect = index == 1,
                difficulty = "Media",
                responseTimeSeconds = 80,
                timeSpentSeconds = 80,
                timestamp = 1_700_000_000_000L + index * 86_400_000L
            )
        }
        val dashboard = AdaptiveLearningEngine.buildDashboard(
            AnalyticsSnapshot(events, 400, 0, 0, 7, 0, 4),
            now = 1_700_000_100_000L
        )

        assertTrue(dashboard.adaptiveProfile.exerciseDifficulty in setOf("Muito Fácil", "Fácil"))
        assertEquals("Forca", dashboard.adaptiveProfile.nextTopic)
        assertTrue(dashboard.difficultTopics.any { it.topic == "Forca" })
        assertTrue(dashboard.recommendations.any { it.topic == "Forca" })
    }

    @Test
    fun strongRecentPerformanceRaisesDifficulty() {
        val events = (1..8).map { index ->
            LearningEvent(
                id = index.toString(),
                eventType = "exercise_answered",
                topic = "Energia",
                isCorrect = true,
                difficulty = "Media",
                responseTimeSeconds = 18,
                timeSpentSeconds = 18,
                timestamp = 1_700_000_000_000L + index * 86_400_000L
            )
        }
        val dashboard = AdaptiveLearningEngine.buildDashboard(
            AnalyticsSnapshot(events, 500, 2, 7, 7, 4, 4),
            now = 1_700_800_000_000L
        )

        assertTrue(dashboard.adaptiveProfile.exerciseDifficulty in setOf("Avançado", "Muito Avançado"))
        assertEquals(100, dashboard.accuracyRate)
        assertTrue(dashboard.difficultTopics.isEmpty())
    }

    @Test
    fun chatActivityAppearsInEvolutionAndHistoryWithoutInventingMastery() {
        val now = 1_700_000_000_000L
        val events = listOf(
            LearningEvent(
                id = "chat-1",
                eventType = "chat_question_sent",
                topic = "Cinemática",
                isCorrect = null,
                difficulty = null,
                responseTimeSeconds = 0,
                timeSpentSeconds = 0,
                timestamp = now
            )
        )

        val dashboard = AdaptiveLearningEngine.buildDashboard(
            AnalyticsSnapshot(events, 0, 1, 0, 7, 0, 4),
            now = now
        )

        assertEquals(1, dashboard.dailyEvolution.sumOf { it.activities })
        assertEquals("Chat com Renato", dashboard.performanceHistory.single().activity)
        assertTrue(dashboard.topicPerformance.isEmpty())
        assertEquals(0, dashboard.accuracyRate)
    }
}
