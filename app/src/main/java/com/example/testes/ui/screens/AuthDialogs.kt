package com.example.testes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.AuthApiClient
import kotlinx.coroutines.launch

@Composable
fun ContactSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suporte") },
        text = {
            Column {
                Text("Física Interativa IA")
                Spacer(modifier = Modifier.height(10.dp))
                Text("E-mail: suporte@fisicainterativa.app")
                Text("WhatsApp: (11) 99999-0000")
                Text("Horário: segunda a sexta, 9h às 18h")
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Informe seu e-mail de cadastro e descreva o problema para receber ajuda.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun ResetPasswordDialog(
    initialEmail: String,
    authApiClient: AuthApiClient,
    onDismiss: () -> Unit
) {
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Trocar senha") },
        text = {
            Column {
                Text("Digite seu e-mail e escolha uma nova senha local para este aparelho.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nova senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                message?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val cleanEmail = email.trim()
                    val cleanPassword = password.trim()
                    when {
                        cleanEmail.isBlank() -> {
                            isSuccess = false
                            message = "Informe seu e-mail."
                        }
                        cleanPassword.length < 6 -> {
                            isSuccess = false
                            message = "Use uma senha com pelo menos 6 caracteres."
                        }
                        cleanPassword != confirmPassword.trim() -> {
                            isSuccess = false
                            message = "As senhas não conferem."
                        }
                        else -> {
                            scope.launch {
                                isSaving = true
                                authApiClient.resetPassword(cleanEmail, cleanPassword)
                                    .onSuccess {
                                        isSuccess = true
                                        message = "Senha atualizada. Você já pode entrar."
                                    }
                                    .onFailure {
                                        isSuccess = false
                                        message = it.message ?: "Não consegui trocar a senha agora."
                                    }
                                isSaving = false
                            }
                        }
                    }
                }
            ) {
                Text(if (isSaving) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text(if (isSuccess) "Concluir" else "Cancelar")
            }
        }
    )
}
