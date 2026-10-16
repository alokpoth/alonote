package com.noto.app.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Intelligent Markdown list and checklist auto-continuation.
 * When pressing Enter on a checklist or bullet item, automatically continues the prefix on the new line.
 * If Enter is pressed on an empty prefix, cleanly cancels the list item.
 */
object SmartListContinuation {

    fun processChange(
        newValue: TextFieldValue,
        oldValue: TextFieldValue
    ): TextFieldValue {
        val newText = newValue.text
        val oldText = oldValue.text

        // Only trigger when exactly 1 newline character is inserted with a collapsed cursor
        if (newText.length == oldText.length + 1 && newValue.selection.collapsed) {
            val cursor = newValue.selection.start
            if (cursor > 0 && newText[cursor - 1] == '\n') {
                val prevLineEnd = cursor - 1
                val prevLineStart = newText.lastIndexOf('\n', prevLineEnd - 1) + 1
                val prevLine = newText.substring(prevLineStart, prevLineEnd)

                val prefix = when {
                    prevLine.startsWith("- [ ] ") -> "- [ ] "
                    prevLine.startsWith("- [x] ") -> "- [ ] "
                    prevLine.startsWith("[ ] ") -> "[ ] "
                    prevLine.startsWith("[x] ") -> "[ ] "
                    prevLine.startsWith("- ") -> "- "
                    prevLine.startsWith("* ") -> "* "
                    prevLine.startsWith("• ") -> "• "
                    else -> null
                }

                if (prefix != null) {
                    // Empty list item: cancel prefix and insert blank line
                    if (prevLine.trim() == prefix.trim()) {
                        val updated = newText.substring(0, prevLineStart) + newText.substring(cursor)
                        return TextFieldValue(updated, TextRange(prevLineStart))
                    } else {
                        // Continue prefix
                        val updated = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                        val newCursor = cursor + prefix.length
                        return TextFieldValue(updated, TextRange(newCursor))
                    }
                }
            }
        }

        return newValue
    }
}
