package com.squidink.alloy.modules.scratch.ui.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightsVisualTransformationTest {

    @Test
    fun `filter with null highlights returns identity text without spans`() {
        val transformation = HighlightsVisualTransformation(null)
        val input = AnnotatedString("val x = 10")
        val result = transformation.filter(input)

        assertEquals("val x = 10", result.text.text)
        assertTrue(result.text.spanStyles.isEmpty())
    }

    @Test
    fun `filter with highlights produces opaque color spans with alpha 1`() {
        val code = "fun testFunction() = 42"
        val highlights = Highlights.Builder()
            .code(code)
            .language(SyntaxLanguage.KOTLIN)
            .theme(SyntaxThemes.monokai())
            .build()

        val transformation = HighlightsVisualTransformation(highlights)
        val result = transformation.filter(AnnotatedString(code))

        assertEquals(code, result.text.text)
        assertFalse("Expected syntax spans to be generated", result.text.spanStyles.isEmpty())

        // Verify that all color spans have alpha == 1.0f (opaque), fixing the invisible keywords bug
        val colorSpans = result.text.spanStyles.filter { it.item.color != Color.Unspecified }
        assertTrue("Expected at least one color span", colorSpans.isNotEmpty())
        for (span in colorSpans) {
            assertEquals(
                "Color span must be fully opaque (alpha = 1.0f)",
                1.0f,
                span.item.color.alpha,
                0.001f
            )
        }
    }

    @Test
    fun `filter on light theme produces opaque color spans`() {
        val code = "import kotlin.math.max"
        val highlights = Highlights.Builder()
            .code(code)
            .language(SyntaxLanguage.KOTLIN)
            .theme(SyntaxThemes.atom(false))
            .build()

        val transformation = HighlightsVisualTransformation(highlights)
        val result = transformation.filter(AnnotatedString(code))

        val colorSpans = result.text.spanStyles.filter { it.item.color != Color.Unspecified }
        assertTrue("Expected color spans in light theme", colorSpans.isNotEmpty())
        for (span in colorSpans) {
            assertEquals(
                "Color span in light theme must be fully opaque",
                1.0f,
                span.item.color.alpha,
                0.001f
            )
        }
    }
}
