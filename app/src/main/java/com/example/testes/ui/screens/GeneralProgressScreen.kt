package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.ProgressCard

@Composable
fun GeneralProgressScreen(
    onBackClick: () -> Unit,
    onSubjectClick: (String) -> Unit
) {
    val subjects = listOf(
        "Física 1" to 0.8f,
        "Física 2" to 0.5f,
        "Física 3" to 0.2f
    )

    Scaffold(
        topBar = { AppTopBar(title = "Progresso Geral", onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(subjects) { (name, progress) ->
                Surface(
                    onClick = { onSubjectClick(name) },
                    modifier = Modifier.padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    ProgressCard(title = name, progress = progress)
                }
            }
        }
    }
}