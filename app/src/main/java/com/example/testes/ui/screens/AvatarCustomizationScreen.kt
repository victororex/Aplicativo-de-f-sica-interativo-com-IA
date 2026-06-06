package com.example.testes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testes.data.api.LearningApiClient
import com.example.testes.model.AvatarItem
import com.example.testes.ui.components.AppTopBar
import com.example.testes.ui.components.AvatarBox

@Composable
fun AvatarCustomizationScreen(
    onBackClick: () -> Unit,
    learningApiClient: LearningApiClient = LearningApiClient()
) {
    var items by remember { mutableStateOf<List<AvatarItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        learningApiClient.getAvatarItems()
            .onSuccess {
                items = it
                selectedCategory = it.firstOrNull()?.category.orEmpty()
                selectedItemId = it.firstOrNull()?.id
                errorMessage = null
            }
            .onFailure { errorMessage = "Nao consegui mostrar os visuais agora." }
        isLoading = false
    }

    val categories = remember(items) { items.map { it.category }.distinct() }
    val visibleItems = items.filter { it.category == selectedCategory }

    Scaffold(
        topBar = { AppTopBar(title = "Personalizar Visual", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AvatarBox()
            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> Text(errorMessage ?: "Nao foi possivel carregar itens.")
                categories.isEmpty() -> Text("Nenhum visual disponivel.")
                else -> {
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        categories.forEach { category ->
                            Tab(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    selectedItemId = items.firstOrNull { it.category == category }?.id
                                },
                                text = { Text(category) }
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(visibleItems) { item ->
                            InventorySlot(
                                item = item,
                                isSelected = item.id == selectedItemId,
                                onClick = { selectedItemId = item.id }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Salvar Visual")
            }
        }
    }
}

@Composable
fun InventorySlot(item: AvatarItem, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.name, style = MaterialTheme.typography.labelSmall)
        }
    }
}
