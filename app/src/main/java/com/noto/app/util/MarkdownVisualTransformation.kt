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
 * High-performance, zero-lag visual transformation for live Markdown styling in Jetpack Compose.
 * Uses OffsetMapping.Identity to guarantee 1:1 cursor alignment without jumping or desync.
 * Caches transformed text to eliminate repetitive regex execution during cursor moves and idle recompositions.
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

        val builder = AnnotatedString.Builder(raw)
        val len = raw.length

        fun addSpan(style: SpanStyle, start: Int, end: Int) {
            if (start in 0 until len && end in (start + 1)..len) {
                builder.addStyle(style, start, end)
            }
        }

        // 1. Headings (# Heading or ## Heading or ### Heading)
        for (match in HeadingRegex.findAll(raw)) {
            val hashGroup = match.groups[1] ?: continue
            val textGroup = match.groups[2] ?: continue
            addSpan(SpanStyle(color = syntaxColor), hashGroup.range.first, hashGroup.range.last + 1)
            val size = when (hashGroup.value.length) {
                1 -> 24.sp
                2 -> 20.sp
                else -> 18.sp
            }
            addSpan(
                SpanStyle(fontWeight = FontWeight.Bold, fontSize = size),
                textGroup.range.first,
                textGroup.range.last + 1
            )
        }

        // 2. Bold (**text**)
        for (match in BoldRegex.findAll(raw)) {
            val startToken = match.groups[1] ?: continue
            val content = match.groups[2] ?: continue
            val endToken = match.groups[3] ?: continue

            addSpan(SpanStyle(color = syntaxColor), startToken.range.first, startToken.range.last + 1)
            addSpan(SpanStyle(fontWeight = FontWeight.Bold), content.range.first, content.range.last + 1)
            addSpan(SpanStyle(color = syntaxColor), endToken.range.first, endToken.range.last + 1)
        }

        // 3. Italic (*text* or _text_) - excluding **
        for (match in ItalicRegex.findAll(raw)) {
            val range = match.range
            if (range.last > range.first + 1) {
                addSpan(SpanStyle(color = syntaxColor), range.first, range.first + 1)
                addSpan(SpanStyle(fontStyle = FontStyle.Italic), range.first + 1, range.last)
                addSpan(SpanStyle(color = syntaxColor), range.last, range.last + 1)
            }
        }

        // 4. Strikethrough (~~text~~)
        for (match in StrikeRegex.findAll(raw)) {
            val startToken = match.groups[1] ?: continue
            val content = match.groups[2] ?: continue
            val endToken = match.groups[3] ?: continue

            addSpan(SpanStyle(color = syntaxColor), startToken.range.first, startToken.range.last + 1)
            addSpan(SpanStyle(textDecoration = TextDecoration.LineThrough), content.range.first, content.range.last + 1)
            addSpan(SpanStyle(color = syntaxColor), endToken.range.first, endToken.range.last + 1)
        }

        // 5. Inline Code (`code`)
        for (match in CodeRegex.findAll(raw)) {
            val startToken = match.groups[1] ?: continue
            val content = match.groups[2] ?: continue
            val endToken = match.groups[3] ?: continue

            addSpan(SpanStyle(color = syntaxColor), startToken.range.first, startToken.range.last + 1)
            addSpan(
                SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
                content.range.first,
                content.range.last + 1
            )
            addSpan(SpanStyle(color = syntaxColor), endToken.range.first, endToken.range.last + 1)
        }

        // 6. Checkboxes: - [x] or [x] completed items
        for (match in CompletedCheckRegex.findAll(raw)) {
            val prefix = match.groups[1] ?: continue
            val textGroup = match.groups[2] ?: continue
            addSpan(SpanStyle(color = checkedBoxColor), prefix.range.first, prefix.range.last + 1)
            addSpan(
                SpanStyle(textDecoration = TextDecoration.LineThrough, color = checkedBoxColor),
                textGroup.range.first,
                textGroup.range.last + 1
            )
        }

        // Checkboxes: - [ ] or [ ] uncompleted items
        for (match in PendingCheckRegex.findAll(raw)) {
            val prefix = match.groups[1] ?: continue
            addSpan(SpanStyle(color = syntaxColor, fontWeight = FontWeight.Bold), prefix.range.first, prefix.range.last + 1)
        }

        // 7. Bullet list prefixes (• or - )
        for (match in BulletRegex.findAll(raw)) {
            val prefix = match.groups[1] ?: continue
            addSpan(SpanStyle(color = syntaxColor, fontWeight = FontWeight.Bold), prefix.range.first, prefix.range.last + 1)
        }

        // 8. Hashtags (#tag or #বাংলা)
        for (match in HashtagRegex.findAll(raw)) {
            addSpan(
                SpanStyle(color = tagColor, fontWeight = FontWeight.Medium),
                match.range.first,
                match.range.last + 1
            )
        }

        val result = TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        lastRaw = raw
        lastTransformed = result
        return result
    }
}

// Pre-compiled regex patterns to eliminate repeated compilation during real-time typing
private val HeadingRegex = Regex("""^(#{1,3})\s+(.*)$""", RegexOption.MULTILINE)
private val BoldRegex = Regex("""(\*\*)([^\*\n]+)(\*\*)""")
private val ItalicRegex = Regex("""(?<!\*)\*([^\*\n]+)\*(?!\*)|(?<!_)_([^_\n]+)_(?!_)""")
private val StrikeRegex = Regex("""(~~)([^~\n]+)(~~)""")
private val CodeRegex = Regex("""(`)([^`\n]+)(`)""")
private val CompletedCheckRegex = Regex("""^(\s*(?:-\s+)?\[x\]\s+)(.*)$""", RegexOption.MULTILINE)
private val PendingCheckRegex = Regex("""^(\s*(?:-\s+)?\[\s\]\s+)""", RegexOption.MULTILINE)
private val BulletRegex = Regex("""^(\s*(?:[•\-*])\s+)""", RegexOption.MULTILINE)
private val HashtagRegex = Regex("""(?<=^|\s)(#[\p{L}\p{N}_]+)""")

@Composable
fun rememberMarkdownVisualTransformation(): MarkdownVisualTransformation {
    return remember { MarkdownVisualTransformation() }
}
