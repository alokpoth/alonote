package com.noto.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Distraction-free, rendered Markdown Reader / Focus Mode.
 * Renders headings, interactive to-do checkboxes that can be tapped to toggle in real-time,
 * code blocks with 1-tap copy, bullet lists, and tags.
 */
@Composable
fun MarkdownReaderView(
    title: String,
    body: String,
    onToggleTaskAtLine: (Int) -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Render Title
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (body.isBlank()) {
            Text(
                text = "No content written yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            return@Column
        }

        // Render Body Line-by-Line with Smart Grouping
        val lines = body.lines()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            when {
                // Code block start
                trimmed.startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    val lang = trimmed.removePrefix("```").trim()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        codeLines.add(lines[index])
                        index++
                    }
                    val codeContent = codeLines.joinToString("\n")
                    CodeBlockCard(code = codeContent, language = lang)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Headings
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ").trim()),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ").trim()),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 3.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ").trim()),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                // Interactive Checklist item: - [ ] or - [x]
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("[ ] ") -> {
                    val taskText = if (trimmed.startsWith("- [ ] ")) trimmed.removePrefix("- [ ] ") else trimmed.removePrefix("[ ] ")
                    val lineIdx = index
                    InteractiveTaskRow(
                        checked = false,
                        text = parseInlineMarkdown(taskText),
                        onToggle = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleTaskAtLine(lineIdx)
                        }
                    )
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("[x] ") -> {
                    val taskText = if (trimmed.startsWith("- [x] ")) trimmed.removePrefix("- [x] ") else trimmed.removePrefix("[x] ")
                    val lineIdx = index
                    InteractiveTaskRow(
                        checked = true,
                        text = parseInlineMarkdown(taskText),
                        onToggle = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleTaskAtLine(lineIdx)
                        }
                    )
                }

                // Bullet points
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val bulletText = trimmed.substring(2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp, start = 4.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Blank line
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Regular Paragraph
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            index++
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun InteractiveTaskRow(
    checked: Boolean,
    text: AnnotatedString,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = if (checked) "Completed task" else "Pending task",
            tint = if (checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 22.sp,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CodeBlockCard(
    code: String,
    language: String
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language.isNotBlank()) language.uppercase() else "CODE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = code,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Pre-compiled regex and inline Markdown parser for Bold (**), Italic (*), Strike (~~), Code (`), and Tags (#)
 */
private val BoldPattern = Regex("""\*\*([^*]+)\*\*""")
private val ItalicPattern = Regex("""(?<!\*)\*([^*]+)\*(?!\*)""")
private val StrikePattern = Regex("""~~([^~]+)~~""")
private val CodePattern = Regex("""`([^`]+)`""")
private val TagPattern = Regex("""(?<=^|\s)(#[\p{L}\p{N}_]+)""")

private fun parseInlineMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)

    for (match in BoldPattern.findAll(text)) {
        val range = match.groups[1]!!.range
        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first, range.last + 1)
    }

    for (match in ItalicPattern.findAll(text)) {
        val range = match.groups[1]!!.range
        builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), range.first, range.last + 1)
    }

    for (match in StrikePattern.findAll(text)) {
        val range = match.groups[1]!!.range
        builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), range.first, range.last + 1)
    }

    for (match in CodePattern.findAll(text)) {
        val range = match.groups[1]!!.range
        builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), range.first, range.last + 1)
    }

    for (match in TagPattern.findAll(text)) {
        builder.addStyle(
            SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF64748B), fontWeight = FontWeight.SemiBold),
            match.range.first,
            match.range.last + 1
        )
    }

    return builder.toAnnotatedString()
}
