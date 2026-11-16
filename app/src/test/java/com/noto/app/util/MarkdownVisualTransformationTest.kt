package com.noto.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MarkdownVisualTransformationTest {

    private val transformation = MarkdownVisualTransformation()

    @Test
    fun boldText_stripsDelimitersAndStylesWithBoldFontWeight() {
        val input = AnnotatedString("Hello **world** today")
        val transformed = transformation.filter(input)

        assertEquals("Hello world today", transformed.text.text)
        val spanStyles = transformed.text.spanStyles

        // Must contain bold span
        val boldSpan = spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Bold span style must be present", boldSpan)
        assertEquals(6, boldSpan?.start) // index of "world" in transformed text
        assertEquals(11, boldSpan?.end)

        // Offset mapping verification
        val mapping = transformed.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0)) // 'H'
        assertEquals(5, mapping.originalToTransformed(5)) // ' '
        assertEquals(6, mapping.originalToTransformed(6)) // start '*'
        assertEquals(6, mapping.originalToTransformed(7)) // start '*'
        assertEquals(6, mapping.originalToTransformed(8)) // 'w'
        assertEquals(11, mapping.originalToTransformed(13)) // end '*'
        assertEquals(12, mapping.originalToTransformed(16)) // 't'

        assertEquals(0, mapping.transformedToOriginal(0)) // 'H'
        assertEquals(8, mapping.transformedToOriginal(6)) // 'w'
        assertEquals(15, mapping.transformedToOriginal(11)) // ' '
        assertEquals(16, mapping.transformedToOriginal(12)) // 't'
    }

    @Test
    fun italicText_stripsDelimitersAndStylesWithItalicFontStyle() {
        val input = AnnotatedString("An *important* note")
        val transformed = transformation.filter(input)

        assertEquals("An important note", transformed.text.text)
        val italicSpan = transformed.text.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
        assertNotNull("Italic span style must be present", italicSpan)
        assertEquals(3, italicSpan?.start)
        assertEquals(12, italicSpan?.end)
    }

    @Test
    fun strikethroughText_stripsDelimitersAndStylesWithLineThrough() {
        val input = AnnotatedString("This is ~~cancelled~~ now")
        val transformed = transformation.filter(input)

        assertEquals("This is cancelled now", transformed.text.text)
        val strikeSpan = transformed.text.spanStyles.find { it.item.textDecoration == TextDecoration.LineThrough }
        assertNotNull("Strikethrough span style must be present", strikeSpan)
        assertEquals(8, strikeSpan?.start)
        assertEquals(17, strikeSpan?.end)
    }

    @Test
    fun headingText_stripsHashAndStylesWithLargerFontSizeAndBoldWeight() {
        val input = AnnotatedString("# Main Title\nNormal text")
        val transformed = transformation.filter(input)

        assertEquals("Main Title\nNormal text", transformed.text.text)
        val headingSpan = transformed.text.spanStyles.find { it.item.fontWeight == FontWeight.Bold && it.item.fontSize.value > 20f }
        assertNotNull("Heading span style must be present", headingSpan)
        assertEquals(0, headingSpan?.start)
        assertEquals(10, headingSpan?.end)
    }

    @Test
    fun completedCheckbox_stylesWithLineThrough() {
        val input = AnnotatedString("[x] Done task\n[ ] Pending task")
        val transformed = transformation.filter(input)

        val completedSpan = transformed.text.spanStyles.find { it.item.textDecoration == TextDecoration.LineThrough }
        assertNotNull("Completed checkbox task must have line-through", completedSpan)
    }
}
