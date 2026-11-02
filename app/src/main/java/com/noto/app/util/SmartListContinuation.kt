package com.noto.app.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Intelligent Markdown list, checklist, and numbered list auto-continuation.
 * When pressing Enter on a checklist or bullet item, automatically continues the prefix on the new line.
 * If Enter is pressed on an empty prefix, cleanly cancels the list item.
 */
object SmartListContinuation {

    private val NumberedListRegex = Regex("""^(\d+)\.\s""")

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

                val numMatch = NumberedListRegex.find(prevLine)
                val (prefix, isEmpty) = when {
                    prevLine.startsWith("- [ ] ") -> Pair("- [ ] ", prevLine.trim() == "- [ ]")
                    prevLine.startsWith("- [x] ") -> Pair("- [ ] ", prevLine.trim() == "- [x]")
                    prevLine.startsWith("[ ] ") -> Pair("[ ] ", prevLine.trim() == "[ ]")
                    prevLine.startsWith("[x] ") -> Pair("[ ] ", prevLine.trim() == "[ ]")
                    prevLine.startsWith("- ") -> Pair("- ", prevLine.trim() == "-")
                    prevLine.startsWith("* ") -> Pair("* ", prevLine.trim() == "*")
                    prevLine.startsWith("• ") -> Pair("• ", prevLine.trim() == "•")
                    numMatch != null -> {
                        val num = numMatch.groupValues[1].toIntOrNull()
                        if (num != null) {
                            Pair("${num + 1}. ", prevLine.trim() == "${num}.")
                        } else null to false
                    }
                    else -> null to false
                }

                if (prefix != null) {
                    if (isEmpty) {
                        // Empty list item: cancel prefix and insert blank line
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
