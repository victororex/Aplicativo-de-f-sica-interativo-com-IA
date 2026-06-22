package com.example.testes.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMarkdownTest {
    @Test
    fun normalizesMalformedHeadingsAndOrphanMarkers() {
        val normalized = normalizeAiMarkdown("######**Resumo**\n\n__\nTexto")

        assertEquals("###### **Resumo**\n\nTexto", normalized)
        assertFalse(normalized.lines().any { it.trim() == "__" })
    }

    @Test
    fun keepsValidNestedHeadingWithoutSplittingItsMarkers() {
        assertEquals("## Explicação", normalizeAiMarkdown("## Explicação"))
        assertEquals(
            AiContentBlock.Heading(2, "Explicação"),
            splitAiContent(normalizeAiMarkdown("## Explicação")).single()
        )
    }

    @Test
    fun extractsDisplayFormulas() {
        val blocks = splitAiContent(
            """
            # Velocidade
            Explicação curta.

            ${'$'}${'$'}
            v = Δs / Δt
            [v] = [L][T]^-1
            ${'$'}${'$'}

            ## Resumo
            A velocidade relaciona espaço e tempo.
            """.trimIndent()
        )

        assertEquals(5, blocks.size)
        assertEquals(AiContentBlock.Heading(1, "Velocidade"), blocks[0])
        assertTrue(blocks[1] is AiContentBlock.MarkdownText)
        assertEquals(
            "v = Δs / Δt\n[v] = [L][T]^-1",
            (blocks[2] as AiContentBlock.Formula).content
        )
        assertEquals(AiContentBlock.Heading(2, "Resumo"), blocks[3])
        assertTrue(blocks[4] is AiContentBlock.MarkdownText)
    }

    @Test
    fun preservesNumberedListsAsMarkdown() {
        val blocks = splitAiContent("1. Identifique os dados\n2. Aplique a fórmula")

        assertEquals(
            listOf(AiContentBlock.MarkdownText("1. Identifique os dados\n2. Aplique a fórmula")),
            blocks
        )
    }

    @Test
    fun removesMarkdownBeforeSpeech() {
        val spoken = aiTextForSpeech(
            "# Velocidade\n\n- Use **distância**.\n\n[Saiba mais](https://example.com)"
        )

        assertEquals("Velocidade Use distância. Saiba mais", spoken)
    }
}
