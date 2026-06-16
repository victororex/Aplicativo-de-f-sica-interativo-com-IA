package com.example.testes.model

data class MissionTrack(
    val subjectId: String,
    val name: String,
    val missions: List<MissionDetail>
)

data class MissionDetail(
    val index: Int,
    val id: String,
    val title: String,
    val subtitle: String,
    val requirements: List<String>,
    val objectives: List<String>,
    val contentBlocks: List<MissionContentBlock>,
    val questions: List<MissionQuestion>
)

sealed class MissionContentBlock {
    abstract val id: String
    abstract val title: String

    data class Text(
        override val id: String,
        override val title: String,
        val body: String
    ) : MissionContentBlock()

    data class BulletList(
        override val id: String,
        override val title: String,
        val items: List<String>
    ) : MissionContentBlock()
}

sealed class MissionQuestion {
    abstract val id: String
    abstract val statement: String
    abstract val explanation: String

    data class MultipleChoice(
        override val id: String,
        override val statement: String,
        val options: List<String>,
        val correctIndex: Int,
        override val explanation: String
    ) : MissionQuestion()

    data class TrueFalse(
        override val id: String,
        override val statement: String,
        val correct: Boolean,
        override val explanation: String
    ) : MissionQuestion()

    data class Numeric(
        override val id: String,
        override val statement: String,
        val answer: Double,
        val tolerance: Double,
        val unit: String,
        override val explanation: String
    ) : MissionQuestion()

    data class Open(
        override val id: String,
        override val statement: String,
        val placeholder: String,
        override val explanation: String
    ) : MissionQuestion()

    data class MultiStep(
        override val id: String,
        override val statement: String,
        val steps: List<String>,
        override val explanation: String
    ) : MissionQuestion()
}
