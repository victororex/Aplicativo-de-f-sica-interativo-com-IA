package com.example.testes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar

@Composable
fun SupportScreen(onBackClick: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Suporte", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SupportCard(
                    icon = Icons.Default.LockReset,
                    title = "Recuperar senha",
                    text = "Nesta demo local, crie uma nova conta ou use o acesso de teste: aluno@demo.com com senha 123456."
                )
                Spacer(modifier = Modifier.height(12.dp))
                SupportCard(
                    icon = Icons.Default.Mail,
                    title = "Falar com o suporte",
                    text = "Para apresentar o projeto, informe o e-mail usado e descreva o problema ao responsavel pela avaliacao."
                )
                Spacer(modifier = Modifier.height(12.dp))
                SupportCard(
                    icon = Icons.Default.Info,
                    title = "Sobre a demo",
                    text = "O app roda sem servidor externo. Aulas, campanha, desafios, chat e progresso ficam salvos no aparelho."
                )
            }
        }
    }
}

@Composable
private fun SupportCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
