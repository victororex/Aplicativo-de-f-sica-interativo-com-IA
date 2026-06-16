package com.example.testes.model

import androidx.compose.ui.graphics.Color

data class SubjectPlanet(
    val id: String,
    val name: String,
    val color: Color,
    val progress: Float,
    val missions: List<MissionNode>
)

data class MissionNode(
    val id: String,
    val title: String,
    val description: String,
    val status: MissionStatus,
    val order: Int,
    val subjectId: String = "",
    val stageLabel: String = ""
)

enum class MissionStatus { LOCKED, CURRENT, COMPLETED }
