package com.noto.app.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Line-level transformation actions for Markdown formatting.
 */
enum class LineFormatAction {
    BULLET_LIST,
    CHECKLIST,
    HEADING_CYCLE
}

/**
 * Utility functions for applying markdown formatting to Compose TextFieldValue.
 */
object MarkdownFormatter {

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
