package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar

@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = "Configurações", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            SettingsItem("Alterar Nome", "Gustavo Silva", Icons.Default.Person)
            SettingsItem("Alterar E-mail", "gustavo@example.com", Icons.Default.Email)
            SettingsItem("Alterar Senha", "********", Icons.Default.Lock)
            SettingsItem("Alterar Telefone", "(11) 99999-9999", Icons.Default.Phone)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "Privacidade", style = MaterialTheme.typography.titleMedium)
            SettingsItem("Conta Privada", "Ativado", null, isSwitch = true)
            SettingsItem("Notificações", "Ativado", null, isSwitch = true)
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(
                onClick = { /* Delete Account Logic */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Excluir Conta")
            }
        }
    }
}

@Composable
fun SettingsItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, isSwitch: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                if (!isSwitch) {
                    Text(text = value, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                }
            }
        }
        if (isSwitch) {
            var checked by remember { mutableStateOf(true) }
            Switch(checked = checked, onCheckedChange = { checked = it })
        } else {
            TextButton(onClick = { /* Edit Logic */ }) {
                Text("Editar")
            }
        }
    }
}