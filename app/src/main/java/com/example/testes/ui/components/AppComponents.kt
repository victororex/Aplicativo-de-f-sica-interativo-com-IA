package com.example.testes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.testes.model.Lesson
import com.example.testes.ui.theme.CardBorder
import com.example.testes.ui.theme.Radius
import com.example.testes.ui.theme.SpaceCyanSubtle
import com.example.testes.ui.theme.Spacing

/* ---------------- Layout & background ---------------- */

@Composable
fun AppScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StarfieldBackground()
        content()
    }
}

@Composable
fun StarfieldBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        List(34) { index ->
            val x = ((index * 37 + 11) % 101) / 101f
            val y = ((index * 61 + 17) % 103) / 103f
            val r = if (index % 4 == 3) 1.4f else if (index % 4 == 2) 1.0f else 0.6f
            Triple(x, y, r)
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { (xF, yF, r) ->
            drawCircle(
                color = Color(0xFFEAF1FA).copy(alpha = 0.055f),
                radius = r.dp.toPx(),
                center = Offset(size.width * xF, size.height * yF)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBackClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .statusBarsPadding()
                .height(46.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .height(64.dp)
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        val items = listOf(
            Triple("Início", "home", Icons.Default.Home),
            Triple("Missão", "study_campaign", Icons.Default.Science),
            Triple("Renato", "chat", Icons.Default.Psychology),
            Triple("Perfil", "profile", Icons.Default.Person)
        )
        items.forEach { (label, route, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/* ---------------- Cards ---------------- */

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Radius.lg)
    val base = modifier
        .fillMaxWidth()
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Surface(
        modifier = clickable,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, CardBorder, shape)
                .padding(Spacing.md),
            content = content
        )
    }
}

/* ---------------- Buttons ---------------- */

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(Radius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}

/* ---------------- Section headers / chips / empty states ---------------- */

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class StatusTone { Success, Warning, Error, Neutral }

@Composable
fun StatusChip(label: String, tone: StatusTone = StatusTone.Neutral, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        StatusTone.Success -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f) to MaterialTheme.colorScheme.tertiary
        StatusTone.Warning -> Color(0xFFF59E0B).copy(alpha = 0.16f) to Color(0xFFF59E0B)
        StatusTone.Error   -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f) to MaterialTheme.colorScheme.error
        StatusTone.Neutral -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.secondary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(Spacing.md))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/* ---------------- Compatibility shims (used by existing screens) ---------------- */

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun AppHeroPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    GlassCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Spacing.xs))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) {
            Spacer(Modifier.height(Spacing.md))
            action()
        }
    }
}

@Composable
fun ProgressCard(title: String, progress: Float) {
    GlassCard(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "${(progress * 100).toInt()}% concluído",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun LessonCard(lesson: Lesson, onClick: () -> Unit) {
    GlassCard(onClick = onClick, modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniPhysicsMark()
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(lesson.module, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    lesson.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun HomeMenuButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = accent)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/* Legacy bubble (kept for binary compatibility com módulos antigos). */
@Composable
fun ChatMessageBubble(
    message: String,
    isFromUser: Boolean,
    onCopy: ((String) -> Unit)? = null
) {
    RenatoMessageBubble(
        message = com.example.testes.model.ChatMessage(
            id = message.hashCode().toString(),
            text = message,
            isFromUser = isFromUser
        ),
        onCopy = onCopy ?: {},
        onShare = {},
        onSave = {},
        onOpenLesson = {}
    )
}

@Composable
private fun FormattedChatText(message: String, color: Color) {
    val lines = message.lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                Spacer(Modifier.height(2.dp))
            } else if (line.looksLikeFormula()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ) {
                    Text(
                        text = line,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(text = line, color = color, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun String.looksLikeFormula(): Boolean {
    val text = trim()
    return text.contains("=") ||
        text.contains("[M]") ||
        text.contains("[L]") ||
        text.contains("[T]") ||
        text.startsWith("v =") ||
        text.startsWith("[v]")
}

@Composable
fun VoiceButton(onClick: () -> Unit, isListening: Boolean = false) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(if (isListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Falar")
    }
}

@Composable
fun HelpSignInButton(onClick: () -> Unit) {
    GhostButton(label = "Preciso de ajuda para entrar", onClick = onClick)
}

@Composable
fun AvatarBox(modifier: Modifier = Modifier, initial: String = "U") {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(1).uppercase(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MiniPhysicsMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            drawCircle(
                color = primary,
                radius = size.minDimension * 0.18f,
                center = center
            )
            drawLine(
                color = SpaceCyanSubtle.copy(alpha = 0.82f),
                start = Offset(size.width * 0.1f, size.height * 0.78f),
                end = Offset(size.width * 0.9f, size.height * 0.22f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }
    }
}

/* ---------------- Renato Chat Bubble (v4) ---------------- */

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RenatoMessageBubble(
    message: com.example.testes.model.ChatMessage,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onSave: (String) -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val isUser = message.isFromUser
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }
    val containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuOpen = true }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!message.imageUri.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = message.imageUri,
                        contentDescription = "Anexo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    if (message.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (message.isAnalyzing) {
                    AnimatedDots(color = MaterialTheme.colorScheme.primary)
                } else if (message.sections.isNotEmpty()) {
                    SectionsContent(message.sections)
                } else if (message.text.isNotBlank()) {
                    FormattedChatText(message = message.text, color = contentColor)
                }
                message.relatedLesson?.let { lesson ->
                    Spacer(Modifier.height(10.dp))
                    Divider(color = CardBorder)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📖", modifier = Modifier.padding(end = 8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Aula recomendada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                lesson.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor
                            )
                            Text(
                                lesson.module,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onOpenLesson(lesson.lessonId) }) {
                            Text("Abrir", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        androidx.compose.material3.DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Copiar") },
                onClick = { onCopy(message.text); menuOpen = false }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Compartilhar") },
                onClick = { onShare(message.text); menuOpen = false }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Salvar") },
                onClick = { onSave(message.text); menuOpen = false }
            )
        }
    }
}

@Composable
private fun SectionsContent(sections: List<com.example.testes.model.MessageSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEachIndexed { index, section ->
            if (index > 0) {
                Divider(color = CardBorder)
            }
            Row(verticalAlignment = Alignment.Top) {
                Text(section.icon, modifier = Modifier.padding(end = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    when (section.style) {
                        com.example.testes.model.SectionStyle.Formula -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    section.body,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                )
                            }
                        }
                        com.example.testes.model.SectionStyle.Tip -> {
                            Text(
                                section.body,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        com.example.testes.model.SectionStyle.Plain -> {
                            Text(
                                section.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedDots(color: Color = MaterialTheme.colorScheme.primary) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "dots")
    val a by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "a"
    )
    val b by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, delayMillis = 150),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "b"
    )
    val c by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, delayMillis = 300),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "c"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, a)
        Spacer(Modifier.width(4.dp))
        Dot(color, b)
        Spacer(Modifier.width(4.dp))
        Dot(color, c)
    }
}

@Composable
private fun Dot(color: Color, alpha: Float) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

/* ---------------- Compatibility shims for ongoing migration ---------------- */

@Composable
fun AssistantAvatar(
    modifier: Modifier = Modifier,
    label: String = "Renato"
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AvatarBox(initial = label.firstOrNull()?.toString() ?: "R", modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}
