package com.noto.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * High-performance, live Markdown visual transformation for Jetpack Compose.
 * Hides raw Markdown delimiter tokens (**, *, _, ~~, `, #) in the live editor,
 * styling content directly with bold, italic, strikethrough, headings, and code formatting.
 * Computes exact, monotonic OffsetMapping between raw and transformed text.
 */
class MarkdownVisualTransformation(
    private val syntaxColor: Color = Color(0x66888888),
    private val checkedBoxColor: Color = Color(0x88888888),
    private val codeBackground: Color = Color(0x1F888888),
    private val tagColor: Color = Color(0xAA64748B)
) : VisualTransformation {

    private var lastRaw: String? = null
    private var lastTransformed: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        // O(1) return when text hasn't changed (e.g., cursor blinking, selection change, or UI recomposition)
        if (raw == lastRaw && lastTransformed != null) {
            return lastTransformed!!
        }

        val builder = AnnotatedString.Builder()
        val N = raw.length
        val origToTrans = IntArray(N + 1)
        val transToOrig = ArrayList<Int>(N + 1)

        var currentTransformedIndex = 0

        fun appendChar(origIdx: Int, ch: Char) {
            origToTrans[origIdx] = currentTransformedIndex
            transToOrig.add(origIdx)
            builder.append(ch)
            currentTransformedIndex++
        }

        fun skipChar(origIdx: Int) {
            origToTrans[origIdx] = currentTransformedIndex
        }

        fun processInlineSegment(segment: String, segOffsetInRaw: Int) {
            var segIdx = 0
            for (match in MarkdownFormatter.InlineMarkdownRegex.findAll(segment)) {
                // Plain text before match
                for (i in segIdx until match.range.first) {
                    appendChar(segOffsetInRaw + i, segment[i])
                }

                val boldContent = match.groups[2]
                val italic1Content = match.groups[4]
                val italic2Content = match.groups[5]
                val strikeContent = match.groups[7]
                val codeContent = match.groups[9]
                val tagContent = match.groups[10]

                val matchedContent = boldContent ?: italic1Content ?: italic2Content ?: strikeContent ?: codeContent
                val matchedStyle = when {
                    boldContent != null -> SpanStyle(fontWeight = FontWeight.Bold)
                    italic1Content != null || italic2Content != null -> SpanStyle(fontStyle = FontStyle.Italic)
                    strikeContent != null -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    codeContent != null -> SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
                    else -> null
                }

                if (matchedContent != null && matchedStyle != null) {
                    // Skip opening delimiters
                    for (i in match.range.first until matchedContent.range.first) {
                        skipChar(segOffsetInRaw + i)
                    }

                    // Append inner content with style
                    val styleStart = currentTransformedIndex
                    for (i in matchedContent.range) {
                        appendChar(segOffsetInRaw + i, segment[i])
                    }
                    builder.addStyle(matchedStyle, styleStart, currentTransformedIndex)

                    // Skip closing delimiters
                    for (i in (matchedContent.range.last + 1)..match.range.last) {
                        skipChar(segOffsetInRaw + i)
                    }
                } else if (tagContent != null) {
                    val styleStart = currentTransformedIndex
                    for (i in match.range) {
                        appendChar(segOffsetInRaw + i, segment[i])
                    }
                    builder.addStyle(
                        SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold),
                        styleStart,
                        currentTransformedIndex
                    )
                } else {
                    for (i in match.range) {
                        appendChar(segOffsetInRaw + i, segment[i])
                    }
                }

                segIdx = match.range.last + 1
            }

            // Plain text after all matches in this segment
            for (i in segIdx until segment.length) {
                appendChar(segOffsetInRaw + i, segment[i])
            }
        }

        var lineStart = 0
        while (lineStart < N) {
            val lineEnd = raw.indexOf('\n', lineStart).let { if (it == -1) N else it }
            val line = raw.substring(lineStart, lineEnd)

            val headingMatch = HeadingRegex.find(line)
            val completedMatch = CompletedCheckRegex.find(line)
            val pendingMatch = PendingCheckRegex.find(line)

            when {
                headingMatch != null -> {
                    val hashGroup = headingMatch.groups[1]!!
                    val textGroup = headingMatch.groups[2]!!
                    val prefixEnd = lineStart + hashGroup.value.length + (line.length - hashGroup.value.length - textGroup.value.length)

                    // Skip the "# " prefix
                    for (i in lineStart until prefixEnd) {
                        skipChar(i)
                    }

                    val headingStartInTrans = currentTransformedIndex
                    processInlineSegment(textGroup.value, prefixEnd)
                    val headingEndInTrans = currentTransformedIndex

                    val size = when (hashGroup.value.length) {
                        1 -> 24.sp
                        2 -> 20.sp
                        else -> 18.sp
                    }
                    if (headingEndInTrans > headingStartInTrans) {
                        builder.addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold, fontSize = size),
                            headingStartInTrans,
                            headingEndInTrans
                        )
                    }
                }
                completedMatch != null -> {
                    val prefix = completedMatch.groups[1]!!
                    val textGroup = completedMatch.groups[2]!!

                    val prefixStart = currentTransformedIndex
                    for (i in prefix.range) {
                        appendChar(lineStart + i, line[i])
                    }
                    builder.addStyle(SpanStyle(color = checkedBoxColor), prefixStart, currentTransformedIndex)

                    val textStart = currentTransformedIndex
                    processInlineSegment(textGroup.value, lineStart + prefix.value.length)
                    val textEnd = currentTransformedIndex

                    if (textEnd > textStart) {
                        builder.addStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough, color = checkedBoxColor),
                            textStart,
                            textEnd
                        )
                    }
                }
                pendingMatch != null -> {
                    val prefix = pendingMatch.groups[1]!!
                    val prefixStart = currentTransformedIndex
                    for (i in prefix.range) {
                        appendChar(lineStart + i, line[i])
                    }
                    builder.addStyle(SpanStyle(color = syntaxColor, fontWeight = FontWeight.Bold), prefixStart, currentTransformedIndex)

                    val remainder = line.substring(prefix.value.length)
                    processInlineSegment(remainder, lineStart + prefix.value.length)
                }
                else -> {
                    processInlineSegment(line, lineStart)
                }
            }

            if (lineEnd < N) {
                // Preserved newline character
                appendChar(lineEnd, '\n')
            }

            lineStart = lineEnd + 1
        }

        origToTrans[N] = currentTransformedIndex
        transToOrig.add(N)

        val mapping = MarkdownOffsetMapping(
            origToTrans = origToTrans,
            transToOrig = transToOrig.toIntArray(),
            originalLength = N,
            transformedLength = currentTransformedIndex
        )

        val result = TransformedText(builder.toAnnotatedString(), mapping)
        lastRaw = raw
        lastTransformed = result
        return result
    }
}

private class MarkdownOffsetMapping(
    private val origToTrans: IntArray,
    private val transToOrig: IntArray,
    private val originalLength: Int,
    private val transformedLength: Int
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, originalLength)
        return origToTrans[clamped].coerceIn(0, transformedLength)
    }

    override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, transformedLength)
        return transToOrig[clamped].coerceIn(0, originalLength)
    }
}

// Pre-compiled regex patterns to eliminate repeated compilation during real-time typing
private val HeadingRegex = Regex("""^(#{1,3})\s+(.+)$""")
private val CompletedCheckRegex = Regex("""^(\s*(?:-\s+)?\[x\]\s+)(.*)$""")
private val PendingCheckRegex = Regex("""^(\s*(?:-\s+)?\[\s\]\s+)""")

@Composable
fun rememberMarkdownVisualTransformation(): MarkdownVisualTransformation {
    return remember { MarkdownVisualTransformation() }
}
