package com.example.testes.model

data class FormulaStep(
    val title: String,
    val explanation: String,
    val latex: String?
)

data class GraphPoint(
    val x: Float,
    val y: Float
)

data class FormulaGraph(
    val expression: String,
    val label: String,
    val xMin: Float,
    val xMax: Float,
    val points: List<GraphPoint>
)

data class FormulaAnalysis(
    val contentType: String,
    val visualDescription: String,
    val structuredData: List<String>,
    val ocrText: String,
    val latex: String,
    val problemStatement: String,
    val steps: List<FormulaStep>,
    val finalAnswer: String,
    val graph: FormulaGraph?,
    val narrationText: String,
    val warnings: List<String>
)
