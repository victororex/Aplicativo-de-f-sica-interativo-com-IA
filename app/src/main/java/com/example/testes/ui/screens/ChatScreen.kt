package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.ChatMessageBubble
import com.example.testes.ui.components.VoiceButton
import com.example.testes.viewmodel.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel, onBackClick: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    var textState by remember { mutableStateOf("") }

    Scaffold(
        topBar = { AppTopBar(title = "Assistente de Física", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    ChatMessageBubble(
                        message = message.text,
                        isFromUser = message.isFromUser
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoiceButton(onClick = { /* Future Voice Logic */ })
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Pergunte algo...") },
                    shape = MaterialTheme.shapes.medium
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = {
                    viewModel.sendMessage(textState)
                    textState = ""
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}