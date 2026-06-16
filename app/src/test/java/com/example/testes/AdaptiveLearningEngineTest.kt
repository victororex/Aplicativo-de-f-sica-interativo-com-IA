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
                timestamp = 1_700_000_000_000L + index
            )
        }
        val dashboard = AdaptiveLearningEngine.buildDashboard(
            AnalyticsSnapshot(events, 400, 0, 0, 7, 0, 4),
            now = 1_700_000_100_000L
        )

        assertEquals("Facil", dashboard.adaptiveProfile.exerciseDifficulty)
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
                timestamp = 1_700_000_000_000L + index
            )
        }
        val dashboard = AdaptiveLearningEngine.buildDashboard(
            AnalyticsSnapshot(events, 500, 2, 3, 7, 2, 4),
            now = 1_700_000_100_000L
        )

        assertEquals("Avancada", dashboard.adaptiveProfile.exerciseDifficulty)
        assertEquals(100, dashboard.accuracyRate)
        assertTrue(dashboard.difficultTopics.isEmpty())
    }
}
