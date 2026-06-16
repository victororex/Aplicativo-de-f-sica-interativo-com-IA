package com.example.testes.data.local

import com.example.testes.model.DailyChallengeDefinition
import com.example.testes.model.DailyDimensionalQuestion
import com.example.testes.model.Difficulty
import com.example.testes.model.dimensionalDailyChallenges
import java.time.LocalDate
import kotlin.math.min
import kotlin.random.Random

/** Janela mínima de não repetição de uma mesma pergunta no diário. */
const val NO_REPEAT_DAYS_WINDOW: Long = 90L

/** Quantas perguntas o desafio diário traz. Pode ser 1 ou 2; usamos 2 quando há pool. */
const val DAILY_QUESTIONS_PER_DAY: Int = 2

fun unlockedDifficulties(highestMissionUnlocked: Int): Set<Difficulty> = when {
    highestMissionUnlocked <= 0 -> emptySet()
    highestMissionUnlocked in 1..2 -> setOf(Difficulty.EASY)
    highestMissionUnlocked in 3..5 -> setOf(Difficulty.EASY, Difficulty.MEDIUM)
    highestMissionUnlocked in 6..7 -> setOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)
    else -> setOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.MASTER)
}

fun filterQuestionsForChallenge(
    allQuestions: List<DailyDimensionalQuestion>,
    definition: DailyChallengeDefinition,
    highestMissionUnlocked: Int
): List<DailyDimensionalQuestion> {
    val ceiling = min(highestMissionUnlocked, definition.maxMissionIndex)
    return allQuestions.filter { q ->
        if (q.difficulty != definition.difficulty) return@filter false
        val rangeMin = q.missionIndexRange.getOrNull(0) ?: return@filter false
        val rangeMax = q.missionIndexRange.getOrNull(1) ?: rangeMin
        rangeMin <= ceiling && rangeMax >= definition.minMissionIndex
    }
}

fun pickQuestionsForToday(
    eligibleQuestions: List<DailyDimensionalQuestion>,
    definition: DailyChallengeDefinition,
    today: LocalDate
): List<DailyDimensionalQuestion> {
    if (eligibleQuestions.size <= definition.numQuestions) return eligibleQuestions
    val seed = today.toEpochDay() xor definition.id.hashCode().toLong()
    return eligibleQuestions.shuffled(Random(seed)).take(definition.numQuestions)
}

/**
 * Sorteia a dificuldade do dia entre as liberadas, determinístico por dia mas variando dia a dia.
 * Estratégia: cria uma ordem aleatória das dificuldades liberadas e retorna a primeira; o caller
 * pode iterar a sequência completa se a primeira não tiver perguntas elegíveis.
 */
fun difficultyOrderForToday(
    unlocked: Set<Difficulty>,
    today: LocalDate
): List<Difficulty> {
    if (unlocked.isEmpty()) return emptyList()
    val seed = today.toEpochDay() xor 0x9E3779B97F4A7C15UL.toLong()
    return unlocked.toList().shuffled(Random(seed))
}

/**
 * Sorteia até [count] perguntas do dia entre [eligible], com seed determinístico por dia.
 * Aplica o filtro de "não repete em [NO_REPEAT_DAYS_WINDOW] dias" a partir do mapa de uso.
 */
fun pickDailyQuestions(
    eligible: List<DailyDimensionalQuestion>,
    today: LocalDate,
    usageEpochDayById: Map<String, Long>,
    count: Int = DAILY_QUESTIONS_PER_DAY
): List<DailyDimensionalQuestion> {
    val todayEpoch = today.toEpochDay()
    val fresh = eligible.filter { q ->
        val lastUsed = usageEpochDayById[q.id] ?: return@filter true
        (todayEpoch - lastUsed) >= NO_REPEAT_DAYS_WINDOW
    }
    val pool = if (fresh.isEmpty()) eligible else fresh
    if (pool.isEmpty()) return emptyList()
    if (pool.size <= count) return pool
    val seed = todayEpoch xor 0xC2B2AE3D27D4EB4FUL.toLong()
    return pool.shuffled(Random(seed)).take(count)
}

/**
 * Resolve a [DailyChallengeDefinition] correspondente à dificuldade (uma por difficulty).
 * Fallback no primeiro encontrado.
 */
fun definitionForDifficulty(difficulty: Difficulty): DailyChallengeDefinition =
    dimensionalDailyChallenges.firstOrNull { it.difficulty == difficulty }
        ?: dimensionalDailyChallenges.first()
