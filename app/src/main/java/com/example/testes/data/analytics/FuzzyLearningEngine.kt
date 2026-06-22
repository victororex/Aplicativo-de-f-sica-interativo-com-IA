package com.example.testes.data.analytics

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class FuzzyLearningInputs(
    val accuracy: Double,
    val responseSpeed: Double,
    val studyFrequency: Double,
    val missionProgress: Double,
    val lessonProgress: Double
)

data class FuzzyLearningResult(
    val score: Int,
    val level: String,
    val memberships: Map<String, Double>
)

/**
 * Mamdani fuzzy inference with centroid defuzzification.
 *
 * Every input is continuous in [0, 100]. Overlapping membership functions keep
 * small metric changes from causing abrupt difficulty jumps.
 */
object FuzzyLearningEngine {
    private val outputSets = linkedMapOf(
        "Muito Fácil" to Triple(0.0, 10.0, 30.0),
        "Fácil" to Triple(10.0, 30.0, 50.0),
        "Intermediário" to Triple(30.0, 50.0, 70.0),
        "Avançado" to Triple(50.0, 70.0, 90.0),
        "Muito Avançado" to Triple(70.0, 90.0, 100.0)
    )

    fun infer(inputs: FuzzyLearningInputs): FuzzyLearningResult {
        val accuracy = memberships(inputs.accuracy)
        val speed = memberships(inputs.responseSpeed)
        val frequency = memberships(inputs.studyFrequency)
        val missions = memberships(inputs.missionProgress)
        val lessons = memberships(inputs.lessonProgress)

        val strengths = linkedMapOf(
            "Muito Fácil" to maxOf(
                min(accuracy.low, speed.low),
                min(accuracy.low, lessons.low),
                min(frequency.low, missions.low)
            ),
            "Fácil" to maxOf(
                min(accuracy.low, speed.medium),
                min(accuracy.medium, frequency.low),
                min(missions.low, lessons.medium)
            ),
            "Intermediário" to maxOf(
                minOf(accuracy.medium, speed.medium),
                minOf(frequency.medium, lessons.medium),
                minOf(accuracy.medium, missions.medium)
            ),
            "Avançado" to maxOf(
                minOf(accuracy.high, speed.medium, frequency.medium),
                minOf(accuracy.medium, speed.high, lessons.high),
                minOf(accuracy.high, missions.high, lessons.medium)
            ),
            "Muito Avançado" to maxOf(
                minOf(accuracy.high, speed.high, frequency.high),
                minOf(accuracy.high, missions.high, lessons.high)
            )
        )

        var numerator = 0.0
        var denominator = 0.0
        for (x in 0..100) {
            val aggregated = outputSets.maxOf { (label, triangle) ->
                min(strengths.getValue(label), triangular(x.toDouble(), triangle.first, triangle.second, triangle.third))
            }
            numerator += x * aggregated
            denominator += aggregated
        }
        val score = if (denominator == 0.0) 50 else (numerator / denominator).roundToInt().coerceIn(0, 100)
        val level = outputSets.keys.maxByOrNull { label ->
            val triangle = outputSets.getValue(label)
            min(strengths.getValue(label), triangular(score.toDouble(), triangle.first, triangle.second, triangle.third))
        } ?: "Intermediário"

        return FuzzyLearningResult(score, level, strengths)
    }

    fun responseSpeedFromSeconds(seconds: Int): Double = when {
        seconds <= 0 -> 50.0
        else -> (100.0 - (seconds.coerceIn(10, 180) - 10) * 100.0 / 170.0).coerceIn(0.0, 100.0)
    }

    private data class Memberships(val low: Double, val medium: Double, val high: Double)

    private fun memberships(raw: Double): Memberships {
        val value = raw.coerceIn(0.0, 100.0)
        return Memberships(
            low = leftShoulder(value, 25.0, 55.0),
            medium = triangular(value, 25.0, 55.0, 80.0),
            high = rightShoulder(value, 55.0, 85.0)
        )
    }

    private fun leftShoulder(x: Double, fullUntil: Double, zeroAt: Double): Double = when {
        x <= fullUntil -> 1.0
        x >= zeroAt -> 0.0
        else -> (zeroAt - x) / (zeroAt - fullUntil)
    }

    private fun rightShoulder(x: Double, zeroUntil: Double, fullAt: Double): Double = when {
        x <= zeroUntil -> 0.0
        x >= fullAt -> 1.0
        else -> (x - zeroUntil) / (fullAt - zeroUntil)
    }

    private fun triangular(x: Double, left: Double, center: Double, right: Double): Double {
        if (x <= left || x >= right) return if (x == center) 1.0 else 0.0
        if (x == center) return 1.0
        return if (x < center) (x - left) / (center - left) else (right - x) / (right - center)
    }
}
