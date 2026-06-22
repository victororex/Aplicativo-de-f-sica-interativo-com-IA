package com.example.testes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown

internal sealed interface AiContentBlock {
    data class Heading(val level: Int, val content: String) : AiContentBlock
    data class MarkdownText(val content: String) : AiContentBlock
    data class Formula(val content: String) : AiContentBlock
}

@Composable
internal fun AiMarkdownMessage(message: String, modifier: Modifier = Modifier) {
    val blocks = remember(message) { splitAiContent(normalizeAiMarkdown(message)) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is AiContentBlock.Heading -> Text(
                    text = block.content
                        .replace(Regex("""[*_~`]"""), "")
                        .trim(),
                    modifier = Modifier.fillMaxWidth(),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                is AiContentBlock.MarkdownText -> Markdown(
                    content = block.content,
                    modifier = Modifier.fillMaxWidth()
                )
                is AiContentBlock.Formula -> FormulaCard(block.content)
            }
        }
    }
}

@Composable
private fun FormulaCard(formula: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Fórmula",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = formula,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

internal fun normalizeAiMarkdown(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()
        .mapNotNull { original ->
            val line = original.trimEnd()
            if (line.trim().matches(Regex("""^[#*_~`>-]+$"""))) {
                null
            } else {
                line.replace(
                    Regex("""^(\s*)(#{1,6})([^#\s].*)$"""),
                    "$1$2 $3"
                )
                    .replace(Regex("""^(\s*)#{7,}\s*"""), "$1###### ")
            }
        }
        .joinToString("\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

internal fun aiTextForSpeech(raw: String): String {
    return normalizeAiMarkdown(raw)
        .replace(Regex("""!\[([^\]]*)]\([^)]+\)"""), "$1")
        .replace(Regex("""\[([^\]]+)]\([^)]+\)"""), "$1")
        .replace(Regex("""(?m)^\s{0,3}#{1,6}\s*"""), "")
        .replace(Regex("""(?m)^\s*>\s?"""), "Observação: ")
        .replace(Regex("""(?m)^\s*[-+*]\s+"""), "")
        .replace(Regex("""(?m)^\s*\d+[.)]\s+"""), "")
        .replace("$$", "")
        .replace("\\[", "")
        .replace("\\]", "")
        .replace(Regex("""[*_~`]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

internal fun splitAiContent(markdown: String): List<AiContentBlock> {
    if (markdown.isBlank()) return emptyList()

    val result = mutableListOf<AiContentBlock>()
    val text = mutableListOf<String>()
    val formula = mutableListOf<String>()
    var formulaDelimiter: String? = null

    fun flushText() {
        text.joinToString("\n").trim().takeIf(String::isNotBlank)?.let {
            result += AiContentBlock.MarkdownText(it)
        }
        text.clear()
    }

    fun flushFormula() {
        formula.joinToString("\n").trim().takeIf(String::isNotBlank)?.let {
            result += AiContentBlock.Formula(it)
        }
        formula.clear()
    }

    markdown.lines().forEach { line ->
        val trimmed = line.trim()
        val heading = Regex("""^(#{1,6})\s+(.+)$""").matchEntire(trimmed)
        when {
            formulaDelimiter != null -> {
                if (trimmed == formulaDelimiter) {
                    flushFormula()
                    formulaDelimiter = null
                } else {
                    formula += line
                }
            }
            trimmed == "$$" || trimmed == "\\[" -> {
                flushText()
                formulaDelimiter = if (trimmed == "$$") "$$" else "\\]"
            }
            trimmed.startsWith("```") &&
                trimmed.removePrefix("```").trim().lowercase() in setOf("math", "latex", "formula") -> {
                flushText()
                formulaDelimiter = "```"
            }
            heading != null -> {
                flushText()
                result += AiContentBlock.Heading(
                    level = heading.groupValues[1].length,
                    content = heading.groupValues[2]
                )
            }
            line.isStandaloneFormula() -> {
                flushText()
                formula += trimmed.removeSurrounding("$").trim()
                flushFormula()
            }
            else -> text += line
        }
    }

    if (formulaDelimiter != null) {
        text += formula.map { "    $it" }
        formula.clear()
    }
    flushText()
    flushFormula()
    return result
}

private fun String.isStandaloneFormula(): Boolean {
    val value = trim()
    if (value.isBlank() || value.startsWith("#") || value.startsWith(">")) return false
    if (value.matches(Regex("""^([-*+]|\d+[.)])\s+.*"""))) return false
    if (value.startsWith("$") && value.endsWith("$") && value.length > 2) return true

    val hasEquation = value.contains("=") || value.contains("→") || value.contains("\\frac")
    val hasPhysicsNotation = value.contains(Regex("""\[[MLTΘNIJvFaEp]\]""")) ||
        value.contains(Regex("""[A-Za-z]\s*[=≈]\s*"""))
    val proseWords = value.split(Regex("""\s+""")).count {
        it.matches(Regex("""[\p{L}À-ÿ]{4,}"""))
    }
    return hasEquation && hasPhysicsNotation && proseWords <= 4
}
