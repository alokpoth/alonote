package com.noto.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration

/**
 * Line-level transformation actions for Markdown formatting.
 */
enum class LineFormatAction {
    BULLET_LIST,
    CHECKLIST,
    HEADING_CYCLE
}

/**
 * Utility functions for applying markdown formatting to Compose TextFieldValue,
 * live parsing inline Markdown, and stripping Markdown symbols.
 */
object MarkdownFormatter {

    /**
     * Unified inline markdown pattern for Bold (**), Italic (* or _), Strike (~~), Code (`), and Tag (#)
     */
    val InlineMarkdownRegex = Regex(
        """(\*\*([^*\n]+)\*\*)|""" +
        """((?<!\*)\*([^*\n]+)\*(?!\*)|(?<!_)_([^_\n]+)_(?!_))|""" +
        """(~~([^~\n]+)~~)|""" +
        """(`([^`\n]+)`)|""" +
        """((?<=^|\s)#[\p{L}\p{N}_]+)"""
    )

    private val HeadingStripPattern = Regex("""^#{1,6}\s+""", RegexOption.MULTILINE)
    private val ChecklistPrefixPattern = Regex("""^\s*(?:-\s+)?\[[ xX]\]\s+""", RegexOption.MULTILINE)
    private val BulletPrefixPattern = Regex("""^\s*[-*•]\s+""", RegexOption.MULTILINE)

    /**
     * Parses inline Markdown, stripping delimiter syntax symbols (**, *, _, ~~, `)
     * and applying styled SpanStyles for bold, italic, strikethrough, monospace code, and hashtags.
     */
    fun parseInlineMarkdown(
        text: String,
        codeBackground: Color = Color(0x1F888888),
        tagColor: Color = Color(0xFF64748B)
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val builder = AnnotatedString.Builder()
        var lastIndex = 0

        for (match in InlineMarkdownRegex.findAll(text)) {
            if (match.range.first > lastIndex) {
                builder.append(text.substring(lastIndex, match.range.first))
            }

            val boldContent = match.groups[2]?.value
            val italic1Content = match.groups[4]?.value
            val italic2Content = match.groups[5]?.value
            val strikeContent = match.groups[7]?.value
            val codeContent = match.groups[9]?.value
            val tagContent = match.groups[10]?.value

            when {
                boldContent != null -> {
                    val start = builder.length
                    builder.append(boldContent)
                    builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, builder.length)
                }
                italic1Content != null -> {
                    val start = builder.length
                    builder.append(italic1Content)
                    builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, builder.length)
                }
                italic2Content != null -> {
                    val start = builder.length
                    builder.append(italic2Content)
                    builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, builder.length)
                }
                strikeContent != null -> {
                    val start = builder.length
                    builder.append(strikeContent)
                    builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, builder.length)
                }
                codeContent != null -> {
                    val start = builder.length
                    builder.append(codeContent)
                    builder.addStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
                        start,
                        builder.length
                    )
                }
                tagContent != null -> {
                    val start = builder.length
                    builder.append(tagContent)
                    builder.addStyle(
                        SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold),
                        start,
                        builder.length
                    )
                }
                else -> {
                    builder.append(match.value)
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }

        return builder.toAnnotatedString()
    }

    /**
     * Strips all Markdown formatting delimiters (bold, italic, strikethrough, code, headings, checkboxes)
     * leaving clean plain text for titles, list previews, search, and PDF exports.
     */
    fun stripMarkdown(text: String): String {
        if (text.isEmpty()) return ""
        return text
            .replace(HeadingStripPattern, "")
            .replace(ChecklistPrefixPattern, "")
            .replace(BulletPrefixPattern, "")
            .replace(InlineMarkdownRegex) { matchResult ->
                matchResult.groups[2]?.value
                    ?: matchResult.groups[4]?.value
                    ?: matchResult.groups[5]?.value
                    ?: matchResult.groups[7]?.value
                    ?: matchResult.groups[9]?.value
                    ?: matchResult.groups[10]?.value
                    ?: matchResult.value
            }
            .trim()
    }

    /**
     * Inlines markdown delimiters (e.g. Bold "**", Italic "*", Strikethrough "~~").
     */
    fun applyInlineFormat(
        current: TextFieldValue,
        prefix: String,
        suffix: String = prefix
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)

        // Case 1: Cursor collapsed (no text selected) -> insert delimiters and place cursor inside
        if (selection.collapsed) {
            val newText = text.substring(0, start) + prefix + suffix + text.substring(end)
            return TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length)
            )
        }

        // Case 2: Text selected -> check toggle or wrap
        val selectedText = text.substring(start, end)
        val isWrapped = selectedText.startsWith(prefix) && selectedText.endsWith(suffix) &&
            selectedText.length >= prefix.length + suffix.length

        return if (isWrapped) {
            // Unwrap
            val unwrapped = selectedText.substring(prefix.length, selectedText.length - suffix.length)
            val newText = text.substring(0, start) + unwrapped + text.substring(end)
            TextFieldValue(
                text = newText,
                selection = TextRange(start, start + unwrapped.length)
            )
        } else {
            // Wrap and preserve inner selection
            val wrapped = "$prefix$selectedText$suffix"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length, end + prefix.length)
            )
        }
    }

    /**
     * Line-level transformations: Heading, Bullet list, Todo checklist.
     */
    fun applyLineFormat(
        current: TextFieldValue,
        action: LineFormatAction
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selStart = selection.min.coerceIn(0, text.length)
        val selEnd = selection.max.coerceIn(0, text.length)

        // Find enclosing line boundaries
        val blockStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val blockEnd = text.indexOf('\n', selEnd).let {
            if (it == -1) text.length else it
        }

        val lines = text.substring(blockStart, blockEnd).split("\n")
        val transformedLines = lines.map { line ->
            when (action) {
                LineFormatAction.BULLET_LIST -> toggleBullet(line)
                LineFormatAction.CHECKLIST -> toggleChecklist(line)
                LineFormatAction.HEADING_CYCLE -> cycleHeading(line)
            }
        }

        val replacement = transformedLines.joinToString("\n")
        val newText = text.substring(0, blockStart) + replacement + text.substring(blockEnd)

        // Keep selection encompassing the modified text
        val delta = replacement.length - (blockEnd - blockStart)
        val newSelStart = if (selection.collapsed) {
            (selStart + (transformedLines.first().length - lines.first().length)).coerceIn(0, newText.length)
        } else {
            selStart
        }
        val newSelEnd = (selEnd + delta).coerceIn(newSelStart, newText.length)

        return TextFieldValue(
            text = newText,
            selection = TextRange(newSelStart, newSelEnd)
        )
    }

    private fun toggleBullet(line: String): String {
        return when {
            line.startsWith("• ") -> line.removePrefix("• ")
            line.startsWith("- ") -> line.removePrefix("- ")
            else -> "• $line"
        }
    }

    private fun toggleChecklist(line: String): String {
        return when {
            line.startsWith("[ ] ") -> "[x] " + line.removePrefix("[ ] ")
            line.startsWith("[x] ") -> line.removePrefix("[x] ")
            line.startsWith("- [ ] ") -> "- [x] " + line.removePrefix("- [ ] ")
            line.startsWith("- [x] ") -> line.removePrefix("- [x] ")
            else -> "[ ] $line"
        }
    }

    private fun cycleHeading(line: String): String {
        return when {
            line.startsWith("### ") -> line.removePrefix("### ")
            line.startsWith("## ") -> "### " + line.removePrefix("## ")
            line.startsWith("# ") -> "## " + line.removePrefix("# ")
            else -> "# $line"
        }
    }

    /**
     * Handles automatic bullet and checklist continuation upon pressing Enter.
     */
    fun handleEnterKey(current: TextFieldValue): TextFieldValue? {
        val text = current.text
        val selection = current.selection
        if (!selection.collapsed) return null

        val cursor = selection.start
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val currentLine = text.substring(lineStart, cursor)

        // If line is empty checklist item "[ ] " -> clear prefix on Enter
        if (currentLine == "[ ] " || currentLine == "- [ ] " || currentLine == "• " || currentLine == "- ") {
            val newText = text.substring(0, lineStart) + text.substring(cursor)
            return TextFieldValue(text = newText, selection = TextRange(lineStart))
        }

        // If line starts with checklist, continue it
        if (currentLine.startsWith("[ ] ") || currentLine.startsWith("[x] ")) {
            val prefix = "\n[ ] "
            val newText = text.substring(0, cursor) + prefix + text.substring(cursor)
            return TextFieldValue(text = newText, selection = TextRange(cursor + prefix.length))
        }

        // If line starts with bullet, continue it
        if (currentLine.startsWith("• ")) {
            val prefix = "\n• "
            val newText = text.substring(0, cursor) + prefix + text.substring(cursor)
            return TextFieldValue(text = newText, selection = TextRange(cursor + prefix.length))
        }

        return null
    }
}
