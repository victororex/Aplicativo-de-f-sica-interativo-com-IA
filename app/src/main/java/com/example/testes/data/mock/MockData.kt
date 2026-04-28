package com.example.testes.data.mock

import com.example.testes.model.Lesson
import com.example.testes.model.User
import com.example.testes.model.Progress

object MockData {
    val user = User(
        id = "1",
        name = "Estudante de Física",
        email = "estudante@universidade.edu"
    )

    val lessons = listOf(
        Lesson("1", "Cinemática Escalar", "Introdução ao movimento.", "Conteúdo detalhado...", "Mecânica"),
        Lesson("2", "Leis de Newton", "As três leis fundamentais.", "Conteúdo detalhado...", "Mecânica"),
        Lesson("3", "Trabalho e Energia", "Conceitos de energia cinética e potencial.", "Conteúdo detalhado...", "Mecânica"),
        Lesson("4", "Termodinâmica", "Leis da termodinâmica.", "Conteúdo detalhado...", "Termologia")
    )

    val progress = Progress(
        userId = "1",
        completedLessons = listOf("1"),
        currentModule = "Mecânica",
        overallCompletion = 0.25f
    )
}