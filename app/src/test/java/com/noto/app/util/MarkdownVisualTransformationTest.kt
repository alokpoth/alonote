package com.noto.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownVisualTransformationTest {

    private val transformation = MarkdownVisualTransformation()

    @Test
    fun boldText_stylesContentWithBoldFontWeight() {
        val input = AnnotatedString("Hello **world** today")
        val transformed = transformation.filter(input)

        assertEquals("Hello **world** today", transformed.text.text)
        val spanStyles = transformed.text.spanStyles

        // Must contain bold span
        val boldSpan = spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Bold span style must be present", boldSpan)
        assertEquals(8, boldSpan?.start) // index of "world"
        assertEquals(13, boldSpan?.end)
    }

    @Test
    fun italicText_stylesContentWithItalicFontStyle() {
        val input = AnnotatedString("An *important* note")
        val transformed = transformation.filter(input)

        val italicSpan = transformed.text.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
        assertNotNull("Italic span style must be present", italicSpan)
    }

    @Test
    fun strikethroughText_stylesContentWithLineThrough() {
        val input = AnnotatedString("This is ~~cancelled~~ now")
        val transformed = transformation.filter(input)

        val strikeSpan = transformed.text.spanStyles.find { it.item.textDecoration == TextDecoration.LineThrough }
        assertNotNull("Strikethrough span style must be present", strikeSpan)
    }

    @Test
    fun headingText_stylesWithLargerFontSizeAndBoldWeight() {
        val input = AnnotatedString("# Main Title\nNormal text")
        val transformed = transformation.filter(input)

        val headingSpan = transformed.text.spanStyles.find { it.item.fontWeight == FontWeight.Bold && it.item.fontSize.value > 20f }
        assertNotNull("Heading span style must be present", headingSpan)
    }

    @Test
    fun completedCheckbox_stylesWithLineThrough() {
        val input = AnnotatedString("[x] Done task\n[ ] Pending task")
        val transformed = transformation.filter(input)

        val completedSpan = transformed.text.spanStyles.find { it.item.textDecoration == TextDecoration.LineThrough }
        assertNotNull("Completed checkbox task must have line-through", completedSpan)
    }
}
