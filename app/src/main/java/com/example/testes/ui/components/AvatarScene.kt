package com.example.testes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.Radius

/**
 * Lightweight avatar presentation.
 *
 * The previous Filament scene kept a native 3D engine, model and environment in
 * memory for a decorative chat header. On constrained devices this pushed the
 * process above 200 MB and allowed the low-memory killer to terminate the app.
 */
@Composable
fun AvatarScene(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.lg)
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f), shape)
            .border(1.dp, CardBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(primary.copy(alpha = 0.08f), size.minDimension * 0.38f, center)
            drawCircle(
                Color.White.copy(alpha = 0.10f),
                size.minDimension * 0.29f,
                center,
                style = Stroke(width = 1.4.dp.toPx())
            )
            drawLine(
                color = primary.copy(alpha = 0.35f),
                start = Offset(size.width * 0.18f, size.height * 0.76f),
                end = Offset(size.width * 0.82f, size.height * 0.24f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), CircleShape)
                .border(1.dp, primary.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "Renato",
                modifier = Modifier.size(58.dp),
                tint = primary
            )
        }
    }
}
