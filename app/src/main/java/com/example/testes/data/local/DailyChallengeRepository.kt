package com.example.testes.data.local

import com.example.testes.data.api.SessionManager
import com.example.testes.model.DailyChallengeInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate

class DailyChallengeRepository(
    private val questionsRepo: DailyQuestionsRepository
) {

    /**
     * Hoje só liberamos UM desafio diário (1–2 perguntas), com dificuldade sorteada entre as
     * liberadas pelo `highestMissionUnlocked`. Perguntas usadas em menos de 90 dias são evitadas.
     */
    suspend fun buildForToday(
        highestMissionUnlocked: Int,
        today: LocalDate = LocalDate.now()
    ): List<DailyChallengeInstance> = withContext(Dispatchers.IO) {
        val allowed = unlockedDifficulties(highestMissionUnlocked)
        if (allowed.isEmpty()) return@withContext emptyList()
        val pool = questionsRepo.getAllDailyQuestions()
        val usage = recentUsageMap()

        // Tenta cada dificuldade liberada (ordem aleatória determinística) até achar uma com
        // perguntas elegíveis NÃO usadas em 90 dias; cai para 'qualquer elegível' se nenhuma sobrar.
        for (diff in difficultyOrderForToday(allowed, today)) {
            val def = definitionForDifficulty(diff)
            val eligible = filterQuestionsForChallenge(pool, def, highestMissionUnlocked)
            val picked = pickDailyQuestions(eligible, today, usage, count = DAILY_QUESTIONS_PER_DAY)
            if (picked.isNotEmpty()) {
                val instanceId = "daily-${today.toEpochDay()}"
                val saved = runCatching {
                    LocalBackend.dailyChallengeInstance(SessionManager.accessToken, instanceId)
                }.getOrNull()
                val effectiveDef = def.copy(numQuestions = picked.size)
                return@withContext listOf(
                    DailyChallengeInstance(
                        instanceId = instanceId,
                        definition = effectiveDef,
                        questions = picked,
                        date = today,
                        completed = saved?.optBoolean("completed", false) == true,
                        lastScore = saved?.optInt("score", 0) ?: 0
                    )
                )
            }
        }
        emptyList()
    }

    suspend fun recordCompletion(
        instance: DailyChallengeInstance,
        score: Int,
        picks: Map<String, Int>
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val picksJson = JSONObject()
                picks.forEach { (k, v) -> picksJson.put(k, v) }
                val epoch = instance.date.toEpochDay()
                instance.questions.forEach { q ->
                    val selected = picks[q.id] ?: -1
                    LocalBackend.recordDailyAnswer(
                        SessionManager.accessToken,
                        q.id,
                        q.tags.firstOrNull() ?: "Análise Dimensional",
                        q.options.getOrElse(selected) { "Não respondida" },
                        q.options.getOrElse(q.correctIndex) { "" },
                        selected == q.correctIndex,
                        instance.definition.difficulty.name,
                        0
                    )
                    LocalBackend.recordDailyQuestionUsage(SessionManager.accessToken, q.id, epoch)
                }
                LocalBackend.recordDailyChallengeInstance(
                    SessionManager.accessToken,
                    instance.instanceId,
                    score,
                    instance.questions.size,
                    picksJson
                )
            }
        }
    }

    suspend fun getPicks(instanceId: String): Map<String, Int> = withContext(Dispatchers.IO) {
        runCatching {
            val saved = LocalBackend.dailyChallengeInstance(SessionManager.accessToken, instanceId)
            val picks = saved.optJSONObject("picks") ?: JSONObject()
            buildMap {
                val it = picks.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    put(k, picks.optInt(k, -1))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private suspend fun recentUsageMap(): Map<String, Long> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = LocalBackend.dailyQuestionUsage(SessionManager.accessToken)
            val map = mutableMapOf<String, Long>()
            val it = raw.keys()
            while (it.hasNext()) {
                val k = it.next()
                map[k] = raw.optLong(k, 0L)
            }
            map
        }.getOrDefault(emptyMap())
    }

}
