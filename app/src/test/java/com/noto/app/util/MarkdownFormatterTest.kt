package com.noto.app.util

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MarkdownFormatterTest {

    @Test
    fun parseInlineMarkdown_boldDelimitersAreStrippedAndStyled() {
        val input = "This is **example** text"
        val result = MarkdownFormatter.parseInlineMarkdown(input)

        assertEquals("This is example text", result.text)
        val boldSpan = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Bold span must be present", boldSpan)
        assertEquals(8, boldSpan?.start) // index of "example"
        assertEquals(15, boldSpan?.end)
    }

    @Test
    fun parseInlineMarkdown_italicAndStrikeAndCodeAreStrippedAndStyled() {
        val input = "*italic* and ~~strike~~ and `code`"
        val result = MarkdownFormatter.parseInlineMarkdown(input)

        assertEquals("italic and strike and code", result.text)
        val italicSpan = result.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
        val strikeSpan = result.spanStyles.find { it.item.textDecoration == TextDecoration.LineThrough }
        val codeSpan = result.spanStyles.find { it.item.fontFamily != null }

        assertNotNull(italicSpan)
        assertNotNull(strikeSpan)
        assertNotNull(codeSpan)

        assertEquals(0, italicSpan?.start)
        assertEquals(6, italicSpan?.end)
    }

    @Test
    fun stripMarkdown_removesAllFormattingTokens() {
        assertEquals("example", MarkdownFormatter.stripMarkdown("**example**"))
        assertEquals("italic", MarkdownFormatter.stripMarkdown("*italic*"))
        assertEquals("strike", MarkdownFormatter.stripMarkdown("~~strike~~"))
        assertEquals("code", MarkdownFormatter.stripMarkdown("`code`"))
        assertEquals("My Heading", MarkdownFormatter.stripMarkdown("# My Heading"))
        assertEquals("Buy Milk", MarkdownFormatter.stripMarkdown("- [ ] Buy Milk"))
        assertEquals("Done Task", MarkdownFormatter.stripMarkdown("- [x] Done Task"))
        assertEquals("Bullet Item", MarkdownFormatter.stripMarkdown("• Bullet Item"))
    }
}
