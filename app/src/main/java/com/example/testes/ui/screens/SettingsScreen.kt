package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.ContentApiClient
import com.example.testes.data.api.SessionManager
import com.example.testes.model.User
import com.example.testes.ui.components.AppHeroPanel
import com.example.testes.ui.components.AppScreenBackground
import com.example.testes.ui.components.AppTopBar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    contentApiClient: ContentApiClient = ContentApiClient()
) {
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var editField by remember { mutableStateOf<EditField?>(null) }
    val scope = rememberCoroutineScope()

    fun save(updated: User) {
        scope.launch {
            isSaving = true
            errorMessage = null
            successMessage = null
            contentApiClient.updateCurrentUser(
                name = updated.name,
                email = updated.email,
                phone = updated.phone,
                privateAccount = updated.privateAccount,
                notificationsEnabled = updated.notificationsEnabled
            ).onSuccess {
                user = it
                SessionManager.user?.let { currentSession ->
                    SessionManager.saveSession(
                        com.example.testes.data.api.AuthResponse(
                            accessToken = SessionManager.accessToken.orEmpty(),
                            tokenType = "bearer",
                            user = currentSession.copy(
                                name = it.name,
                                email = it.email,
                                phone = it.phone,
                                privateAccount = it.privateAccount,
                                notificationsEnabled = it.notificationsEnabled
                            )
                        )
                    )
                }
                successMessage = "Tudo salvo."
            }.onFailure {
                errorMessage = "Nao consegui salvar agora."
            }
            isSaving = false
        }
    }

    LaunchedEffect(Unit) {
        contentApiClient.getCurrentUser()
            .onSuccess {
                user = it
                errorMessage = null
            }
            .onFailure { errorMessage = "Nao consegui mostrar suas configuracoes agora." }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Configuracoes", onBackClick = onBackClick) }
    ) { padding ->
        AppScreenBackground(modifier = Modifier.padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                user == null -> Text(errorMessage ?: "Nao foi possivel carregar suas configuracoes.")
                else -> {
                    val currentUser = user!!
                    AppHeroPanel(
                        title = "Sua conta",
                        subtitle = "Ajuste seus dados e escolha como quer usar o aplicativo."
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsGroup(title = "Dados pessoais") {
                        SettingsItem("Nome", currentUser.name, Icons.Default.Person) {
                            editField = EditField("Nome", currentUser.name, false) { value ->
                                save(currentUser.copy(name = value.trim()))
                            }
                        }
                        SettingsItem("E-mail", currentUser.email, Icons.Default.Email) {
                            editField = EditField("E-mail", currentUser.email, false) { value ->
                                save(currentUser.copy(email = value.trim()))
                            }
                        }
                        SettingsItem("Telefone", currentUser.phone ?: "Nao informado", Icons.Default.Phone) {
                            editField = EditField("Telefone", currentUser.phone.orEmpty(), true) { value ->
                                save(currentUser.copy(phone = value.trim().ifBlank { null }))
                            }
                        }
                        PasswordItem()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsGroup(title = "Preferencias") {
                        SettingsSwitchItem(
                            label = "Conta privada",
                            description = "Oculta dados publicos do perfil.",
                            checked = currentUser.privateAccount,
                            enabled = !isSaving,
                            onCheckedChange = { save(currentUser.copy(privateAccount = it)) }
                        )
                        SettingsSwitchItem(
                            label = "Notificacoes",
                            description = "Receba lembretes e avisos de estudo.",
                            checked = currentUser.notificationsEnabled,
                            enabled = !isSaving,
                            onCheckedChange = { save(currentUser.copy(notificationsEnabled = it)) }
                        )
                    }

                    if (isSaving) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    successMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TextButton(
                        onClick = { errorMessage = "Essa opcao estara disponivel em breve." },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir Conta")
                    }
                }
            }
        }
        }
    }

    editField?.let { field ->
        EditSettingsDialog(
            field = field,
            onDismiss = { editField = null },
            onConfirm = {
                field.onConfirm(it)
                editField = null
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

private data class EditField(
    val label: String,
    val initialValue: String,
    val optional: Boolean,
    val onConfirm: (String) -> Unit
)

@Composable
private fun SettingsItem(label: String, value: String, icon: ImageVector, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.titleMedium)
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onEdit) {
            Text("Editar")
        }
    }
}

@Composable
private fun PasswordItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Senha", style = MaterialTheme.typography.titleMedium)
                Text(text = "Alteracao disponivel em breve", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EditSettingsDialog(field: EditField, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(field) { mutableStateOf(field.initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${field.label}") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = field.optional || value.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
