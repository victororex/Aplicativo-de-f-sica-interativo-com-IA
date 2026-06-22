package com.example.testes

import com.example.testes.data.analytics.FuzzyLearningEngine
import com.example.testes.data.analytics.FuzzyLearningInputs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyLearningEngineTest {
    @Test
    fun lowSignalsProduceBeginnerLevelWithoutAbruptThresholds() {
        val low = FuzzyLearningEngine.infer(FuzzyLearningInputs(25.0, 20.0, 15.0, 10.0, 20.0))
        val slightlyHigher = FuzzyLearningEngine.infer(FuzzyLearningInputs(30.0, 25.0, 20.0, 15.0, 25.0))

        assertTrue(low.level in setOf("Muito Fácil", "Fácil"))
        assertTrue(slightlyHigher.score >= low.score)
        assertTrue(slightlyHigher.score - low.score < 20)
    }

    @Test
    fun consistentlyStrongSignalsReachVeryAdvanced() {
        val result = FuzzyLearningEngine.infer(FuzzyLearningInputs(96.0, 94.0, 90.0, 92.0, 95.0))

        assertEquals("Muito Avançado", result.level)
        assertTrue(result.score >= 80)
    }
}
