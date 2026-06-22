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
import java.util.concurrent.TimeUnit
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
        val sessionEvents = snapshot.events.filter { it.eventType == "study_session_completed" }
        val activeDays = snapshot.events
            .filter { it.timeSpentSeconds > 0 || it.eventType in ACTIVITY_EVENTS }
            .map { TimeUnit.MILLISECONDS.toDays(it.timestamp) }
            .distinct()
            .size
        val responseAverage = attempts.map { it.responseTimeSeconds }.filter { it > 0 }.averageOrZero()
        val fuzzy = FuzzyLearningEngine.infer(
            FuzzyLearningInputs(
                accuracy = if (attempts.isEmpty()) 45.0 else recentAccuracy.toDouble(),
                responseSpeed = FuzzyLearningEngine.responseSpeedFromSeconds(responseAverage),
                studyFrequency = (activeDays * 100.0 / 12.0).coerceAtMost(100.0),
                missionProgress = percentage(snapshot.completedPhases, snapshot.totalPhases).toDouble(),
                lessonProgress = percentage(snapshot.completedLessons, snapshot.totalLessons).toDouble()
            )
        )
        val difficulty = fuzzy.level
        val explanationLevel = when {
            attempts.size < 3 || recentAccuracy < 55 -> "fundamental, com analogias e passos curtos"
            recentAccuracy < 80 -> "intermediario, com exemplos guiados"
            else -> "avancado, direto e com desafios de vestibular"
        }
        val sessionSeconds = sessionEvents.sumOf { it.timeSpentSeconds.coerceAtLeast(0) }
        val totalStudy = if (sessionSeconds > 0) sessionSeconds else snapshot.legacyStudySeconds.coerceAtLeast(0)
        val learningVelocity = when {
            attempts.size < 4 -> "em formação"
            recentAccuracy >= olderAccuracy + 10 -> "acelerando"
            recentAccuracy + 10 <= olderAccuracy -> "desacelerando"
            else -> "constante"
        }
        val studiedTopicNames = topics.map { it.topic.lowercase(Locale.ROOT) }.toSet()
        val knownTopics = snapshot.events.map { normalizedTopic(it.topic) }.distinct()
        val ignoredTopics = knownTopics.filter { it.lowercase(Locale.ROOT) !in studiedTopicNames }.take(3)
        val profile = AdaptiveProfile(
            explanationLevel = explanationLevel,
            exerciseDifficulty = difficulty,
            trend = trend,
            nextTopic = nextTopic,
            reviewTopics = weak.map { it.topic },
            suggestedQuestions = suggestedQuestions(nextTopic, difficulty),
            fuzzyScore = fuzzy.score,
            learningVelocity = learningVelocity,
            ignoredTopics = ignoredTopics
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
            averageResponseTimeSeconds = responseAverage,
            performanceHistory = snapshot.events
                .filter { it.eventType in HISTORY_EVENTS }
                .sortedByDescending { it.timestamp }
                .take(20)
                .map(::historyItem),
            recommendations = recommendations,
            adaptiveProfile = profile,
            aiInteractions = snapshot.events.count { it.eventType == "chat_question_sent" },
            ocrUses = snapshot.events.count { it.eventType == "ocr_used" },
            voiceUses = snapshot.events.count { it.eventType == "voice_used" },
            missionsCompleted = snapshot.events.count {
                it.eventType in setOf(
                    "campaign_stage_completed", "track_mission_completed",
                    "daily_instance_completed", "daily_challenge_completed"
                )
            },
            activeStudyDays = activeDays
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
                questions = periodAttempts.size,
                activities = periodEvents.count { it.eventType in ACTIVITY_EVENTS }
            )
        }
    }

    private fun historyItem(event: LearningEvent): PerformanceHistoryItem {
        val details = when (event.eventType) {
            "exercise_answered" -> Triple(
                "Exercício",
                if (event.isCorrect == true) "Correta" else "Revisar",
                event.difficulty ?: "Não informada"
            )
            "chat_question_sent" -> Triple("Chat com Renato", "Pergunta enviada", "IA")
            "lesson_opened" -> Triple("Aula", "Aula acessada", "Estudo")
            "lesson_completed" -> Triple("Aula", "Aula concluída", "Estudo")
            "ocr_used" -> Triple("Análise de imagem", "OCR concluído", "IA")
            "voice_used" -> Triple("Voz personalizada", "Áudio reproduzido", "IA")
            "campaign_stage_completed", "track_mission_completed" ->
                Triple("Missão", "Missão concluída", event.difficulty ?: "Trilha")
            "daily_instance_completed", "daily_challenge_completed" ->
                Triple("Desafio diário", "Desafio concluído", event.difficulty ?: "Misto")
            else -> Triple("Atividade", "Concluída", event.difficulty ?: "Geral")
        }
        return PerformanceHistoryItem(
            id = event.id,
            topic = normalizedTopic(event.topic),
            activity = details.first,
            result = details.second,
            difficulty = details.third,
            responseTimeSeconds = event.responseTimeSeconds,
            timestamp = event.timestamp
        )
    }

    private fun normalizedTopic(topic: String): String = topic.trim().ifBlank { DEFAULT_TOPIC }
    private fun accuracyFor(events: List<LearningEvent>) = percentage(events.count { it.isCorrect == true }, events.size)
    private fun percentage(value: Int, total: Int) = if (total == 0) 0 else (value * 100f / total).roundToInt()
    private fun List<Int>.averageOrZero() = if (isEmpty()) 0 else average().roundToInt()

    private val ACTIVITY_EVENTS = setOf(
        "lesson_opened", "lesson_completed", "exercise_answered", "chat_question_sent",
        "ocr_used", "voice_used", "track_mission_completed", "campaign_stage_completed",
        "daily_instance_completed", "daily_challenge_completed"
    )
    private val HISTORY_EVENTS = ACTIVITY_EVENTS
}
