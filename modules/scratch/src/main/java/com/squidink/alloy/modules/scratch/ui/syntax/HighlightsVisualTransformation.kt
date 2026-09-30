package com.squidink.alloy.modules.scratch.ui.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight

/**
 * VisualTransformation that decorates code with syntax coloring and font weights
 * produced by the SnipMe Highlights engine.
 *
 * Uses [OffsetMapping.Identity] so cursor and selection indices remain completely
 * unchanged and natural during editing.
 */
class HighlightsVisualTransformation(
    private val highlights: Highlights?
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (highlights == null) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val codeHighlights = highlights.getHighlights()
        val builder = AnnotatedString.Builder(text.text)

        for (highlight in codeHighlights) {
            val start = highlight.location.start.coerceIn(0, text.length)
            val end = highlight.location.end.coerceIn(0, text.length)
            if (start < end) {
                when (highlight) {
                    is ColorHighlight -> {
                        // SnipMe Highlights RGB colors are 24-bit (0x00RRGGBB). Mask with 0xFF alpha (0xFF000000)
                        // so Compose Color does not treat them as 0.0f alpha (transparent).
                        val opaqueColor = Color((highlight.rgb and 0x00FFFFFF) or 0xFF000000.toInt())
                        builder.addStyle(
                            SpanStyle(color = opaqueColor),
                            start,
                            end
                        )
                    }
                    is BoldHighlight -> {
                        builder.addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            start,
                            end
                        )
                    }
                }
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
