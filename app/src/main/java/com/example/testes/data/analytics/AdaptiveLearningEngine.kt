package com.example.testes.data.analytics

import com.example.testes.model.AdaptiveProfile
import com.example.testes.model.EvolutionPoint
import com.example.testes.model.LearningDashboard
import com.example.testes.model.LearningEvent
import com.example.testes.model.PerformanceHistoryItem
import com.example.testes.model.StudyRecommendation
import com.example.testes.model.TopicPerformance
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class AnalyticsSnapshot(
    val events: List<LearningEvent>,
    val legacyStudySeconds: Int,
    val questionsAsked: Int,
    val completedLessons: Int,
    val totalLessons: Int,
    val completedPhases: Int,
    val totalPhases: Int
)

object AdaptiveLearningEngine {
    private const val DEFAULT_TOPIC = "Analise Dimensional"

    fun buildDashboard(snapshot: AnalyticsSnapshot, now: Long = System.currentTimeMillis()): LearningDashboard {
        val attempts = snapshot.events.filter { it.eventType == "exercise_answered" && it.isCorrect != null }
        val topics = attempts.groupBy { normalizedTopic(it.topic) }
            .map { (topic, events) -> topicPerformance(topic, events) }
            .sortedWith(compareBy<TopicPerformance> { it.masteryScore }.thenByDescending { it.attempts })
        val correct = attempts.count { it.isCorrect == true }
        val accuracy = percentage(correct, attempts.size)
        val recentAttempts = attempts.sortedByDescending { it.timestamp }
        val olderAccuracy = accuracyFor(recentAttempts.drop(recentAttempts.size.coerceAtMost(5)).take(5))
        val recentAccuracy = accuracyFor(recentAttempts.take(5))
        val trend = when {
            attempts.size < 4 -> "dados_em_formacao"
            recentAccuracy >= olderAccuracy + 10 -> "evoluindo"
            recentAccuracy + 10 <= olderAccuracy -> "atencao"
            else -> "estavel"
        }
        val weak = topics.filter { it.needsReview }.take(3)
        val nextTopic = weak.firstOrNull()?.topic ?: topics.maxByOrNull { it.masteryScore }?.topic ?: DEFAULT_TOPIC
        val difficulty = recommendedDifficulty(attempts.size, recentAccuracy, topics)
        val explanationLevel = when {
            attempts.size < 3 || recentAccuracy < 55 -> "fundamental, com analogias e passos curtos"
            recentAccuracy < 80 -> "intermediario, com exemplos guiados"
            else -> "avancado, direto e com desafios de vestibular"
        }
        val sessionEvents = snapshot.events.filter { it.eventType == "study_session_completed" }
        val sessionSeconds = sessionEvents.sumOf { it.timeSpentSeconds.coerceAtLeast(0) }
        val totalStudy = maxOf(snapshot.legacyStudySeconds, sessionSeconds)
        val profile = AdaptiveProfile(
            explanationLevel = explanationLevel,
            exerciseDifficulty = difficulty,
            trend = trend,
            nextTopic = nextTopic,
            reviewTopics = weak.map { it.topic },
            suggestedQuestions = suggestedQuestions(nextTopic, difficulty)
        )
        val recommendations = recommendations(profile, weak, attempts.size, accuracy)

        return LearningDashboard(
            totalStudySeconds = totalStudy,
            questionsAnswered = attempts.size,
            questionsAsked = snapshot.questionsAsked,
            completedSessions = sessionEvents.size,
            completedLessons = snapshot.completedLessons,
            totalLessons = snapshot.totalLessons,
            completedPhases = snapshot.completedPhases,
            totalPhases = snapshot.totalPhases,
            topicsStudied = topics.map { it.topic },
            difficultTopics = weak,
            topicPerformance = topics,
            dailyEvolution = timeSeries(attempts, snapshot.events, now, 7, Calendar.DAY_OF_YEAR, "EEE"),
            weeklyEvolution = timeSeries(attempts, snapshot.events, now, 6, Calendar.WEEK_OF_YEAR, "'S'w"),
            monthlyEvolution = timeSeries(attempts, snapshot.events, now, 6, Calendar.MONTH, "MMM"),
            accuracyRate = accuracy,
            averageResponseTimeSeconds = attempts.map { it.responseTimeSeconds }.filter { it > 0 }.averageOrZero(),
            performanceHistory = recentAttempts.take(20).map(::historyItem),
            recommendations = recommendations,
            adaptiveProfile = profile
        )
    }

    private fun topicPerformance(topic: String, events: List<LearningEvent>): TopicPerformance {
        val correct = events.count { it.isCorrect == true }
        val accuracy = percentage(correct, events.size)
        val averageTime = events.map { it.responseTimeSeconds }.filter { it > 0 }.averageOrZero()
        val speedScore = when {
            averageTime == 0 -> 60
            averageTime <= 25 -> 100
            averageTime <= 50 -> 75
            averageTime <= 90 -> 50
            else -> 30
        }
        val confidence = (events.size.coerceAtMost(8) / 8f * 100).roundToInt()
        val mastery = (accuracy * 0.7f + speedScore * 0.15f + confidence * 0.15f).roundToInt()
        return TopicPerformance(topic, events.size, correct, accuracy, averageTime, mastery, events.size >= 2 && mastery < 65)
    }

    private fun recommendedDifficulty(
        attempts: Int,
        recentAccuracy: Int,
        topics: List<TopicPerformance>
    ): String = when {
        attempts < 3 || recentAccuracy < 55 || topics.any { it.attempts >= 2 && it.accuracyRate < 45 } -> "Facil"
        recentAccuracy >= 85 && attempts >= 6 -> "Avancada"
        else -> "Media"
    }

    private fun recommendations(
        profile: AdaptiveProfile,
        weak: List<TopicPerformance>,
        attempts: Int,
        accuracy: Int
    ): List<StudyRecommendation> {
        val items = mutableListOf<StudyRecommendation>()
        if (attempts == 0) {
            items += StudyRecommendation(
                "Crie sua linha de base",
                "Ainda nao ha respostas suficientes para medir seu dominio.",
                DEFAULT_TOPIC,
                "Conclua o desafio diario.",
                1
            )
        }
        weak.forEachIndexed { index, topic ->
            items += StudyRecommendation(
                "Revisar ${topic.topic}",
                "Dominio estimado em ${topic.masteryScore}% com ${topic.accuracyRate}% de acertos.",
                topic.topic,
                "Revise a explicacao e responda 3 questoes ${profile.exerciseDifficulty.lowercase(Locale.ROOT)}s.",
                index + 1
            )
        }
        if (weak.isEmpty() && attempts > 0) {
            items += StudyRecommendation(
                "Avancar com consistencia",
                "Sua taxa atual e de $accuracy% e nao ha topicos criticos.",
                profile.nextTopic,
                "Continue no nivel ${profile.exerciseDifficulty.lowercase(Locale.ROOT)}.",
                1
            )
        }
        if (profile.trend == "atencao") {
            items += StudyRecommendation(
                "Revisao curta recomendada",
                "O desempenho recente caiu em relacao ao bloco anterior.",
                profile.nextTopic,
                "Refaca um exemplo resolvido antes do proximo desafio.",
                1
            )
        }
        return items.distinctBy { it.title + it.topic }.take(4)
    }

    private fun suggestedQuestions(topic: String, difficulty: String): List<String> = listOf(
        "Explique $topic no nivel $difficulty.",
        "Mostre um exemplo resolvido de $topic.",
        "Quais erros devo evitar em $topic?",
        "Crie uma questao $difficulty sobre $topic."
    )

    private fun timeSeries(
        attempts: List<LearningEvent>,
        allEvents: List<LearningEvent>,
        now: Long,
        count: Int,
        calendarField: Int,
        labelPattern: String
    ): List<EvolutionPoint> {
        val formatter = SimpleDateFormat(labelPattern, Locale.forLanguageTag("pt-BR"))
        return (count - 1 downTo 0).map { offset ->
            val start = Calendar.getInstance().apply {
                timeInMillis = now
                when (calendarField) {
                    Calendar.DAY_OF_YEAR -> {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    Calendar.WEEK_OF_YEAR -> {
                        firstDayOfWeek = Calendar.MONDAY; set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    Calendar.MONTH -> {
                        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                }
                add(calendarField, -offset)
            }
            val end = (start.clone() as Calendar).apply { add(calendarField, 1) }
            val periodAttempts = attempts.filter { it.timestamp in start.timeInMillis until end.timeInMillis }
            val periodEvents = allEvents.filter { it.timestamp in start.timeInMillis until end.timeInMillis }
            EvolutionPoint(
                label = formatter.format(Date(start.timeInMillis)).replaceFirstChar { it.uppercase() },
                accuracyRate = accuracyFor(periodAttempts),
                studySeconds = periodEvents.sumOf { it.timeSpentSeconds.coerceAtLeast(0) },
                questions = periodAttempts.size
            )
        }
    }

    private fun historyItem(event: LearningEvent) = PerformanceHistoryItem(
        id = event.id,
        topic = normalizedTopic(event.topic),
        activity = "Exercicio",
        result = if (event.isCorrect == true) "Correta" else "Revisar",
        difficulty = event.difficulty ?: "Nao informada",
        responseTimeSeconds = event.responseTimeSeconds,
        timestamp = event.timestamp
    )

    private fun normalizedTopic(topic: String): String = topic.trim().ifBlank { DEFAULT_TOPIC }
    private fun accuracyFor(events: List<LearningEvent>) = percentage(events.count { it.isCorrect == true }, events.size)
    private fun percentage(value: Int, total: Int) = if (total == 0) 0 else (value * 100f / total).roundToInt()
    private fun List<Int>.averageOrZero() = if (isEmpty()) 0 else average().roundToInt()
}
