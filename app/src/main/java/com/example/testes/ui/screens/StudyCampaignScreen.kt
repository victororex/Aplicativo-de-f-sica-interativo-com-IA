package com.example.testes.ui.screens

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testes.ui.components.AppTopBar

@Composable
fun StudyCampaignScreen(onBackClick: () -> Unit) {
    val scrollState = rememberScrollState()
    var avatarTargetOffset by remember { mutableStateOf(Offset.Zero) }
    val animatedAvatarOffset by animateOffsetAsState(
        targetValue = avatarTargetOffset,
        animationSpec = tween(durationMillis = 1000),
        label = "avatarAnimation"
    )

    // Positions for drawing the path
    val nodePositions = remember { mutableStateMapOf<Int, Offset>() }

    Scaffold(
        topBar = { AppTopBar(title = "Campanha de Estudo", onBackClick = onBackClick) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Path Drawing
            Canvas(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                val sortedPoints = nodePositions.keys.sorted().mapNotNull { nodePositions[it] }
                for (i in 0 until sortedPoints.size - 1) {
                    drawLine(
                        color = Color.Gray,
                        start = sortedPoints[i],
                        end = sortedPoints[i + 1],
                        strokeWidth = 8f
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                val levels = listOf("Ótica", "Eletricidade", "Termologia", "Mecânica", "Introdução")
                levels.forEachIndexed { index, name ->
                    val xOff = if (index % 2 == 0) 60.dp else (-60).dp
                    
                    Box(
                        modifier = Modifier
                            .offset(x = xOff)
                            .onGloballyPositioned { layoutCoordinates ->
                                val pos = layoutCoordinates.positionInParent()
                                // Adjusted coordinates for more precise path
                                nodePositions[index] = Offset(pos.x + 30.dp.value * 3, pos.y + 30.dp.value * 3)
                                if (index == 4 && avatarTargetOffset == Offset.Zero) {
                                    avatarTargetOffset = nodePositions[index]!!
                                }
                            }
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp), // Increased size
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer, // More visible color
                            tonalElevation = 4.dp,
                            onClick = {
                                nodePositions[index]?.let { avatarTargetOffset = it }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (levels.size - index).toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(x = xOff).padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(120.dp))
                }
                Spacer(modifier = Modifier.height(100.dp))
            }

            // The Moving Avatar
            if (avatarTargetOffset != Offset.Zero) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .offset(
                            x = animatedAvatarOffset.x.dp - 28.dp,
                            y = animatedAvatarOffset.y.dp - 28.dp - scrollState.value.dp
                        ),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}