package com.noto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.util.DateFormatter

/**
 * Apple-style detailed statistics sheet displaying words, characters, reading time,
 * speaking time, checklist progress, and exact creation/modification timestamps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteStatsBottomSheet(
    title: String,
    body: String,
    createdAt: Long,
    updatedAt: Long,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val fullText = "$title $body".trim()
    val words = if (fullText.isEmpty()) 0 else fullText.split("\\s+".toRegex()).count { it.isNotBlank() }
    val charsWithSpaces = fullText.length
    val charsNoSpaces = fullText.count { !it.isWhitespace() }
    val lines = if (body.isEmpty()) (if (title.isEmpty()) 0 else 1) else body.lines().size
    val paragraphs = if (body.isEmpty()) 0 else body.split("\n\n+".toRegex()).count { it.isNotBlank() }

    // Reading & Speaking speed estimates
    val readingTimeMinutes = maxOf(1, kotlin.math.ceil(words / 200.0).toInt())
    val speakingTimeMinutes = maxOf(1, kotlin.math.ceil(words / 130.0).toInt())

    // Checklist stats
    val totalTasks = body.lines().count { it.contains("[ ]") || it.contains("[x]") }
    val completedTasks = body.lines().count { it.contains("[x]") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Note Statistics",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Stats Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                StatItem(
                    icon = Icons.Filled.TextFields,
                    label = "Words",
                    value = words.toString()
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

                StatItem(
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    label = "Characters (all / no spaces)",
                    value = "$charsWithSpaces / $charsNoSpaces"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

                StatItem(
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    label = "Lines / Paragraphs",
                    value = "$lines lines • $paragraphs paras"
                )

                if (totalTasks > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                    val percent = ((completedTasks.toDouble() / totalTasks) * 100).toInt()
                    StatItem(
                        icon = Icons.Filled.CheckCircleOutline,
                        label = "Checklist Tasks",
                        value = "$completedTasks of $totalTasks done ($percent%)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reading Estimates Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                StatItem(
                    icon = Icons.Filled.AccessTime,
                    label = "Reading Time",
                    value = "~$readingTimeMinutes min (200 wpm)"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

                StatItem(
                    icon = Icons.Filled.RecordVoiceOver,
                    label = "Speaking Time",
                    value = "~$speakingTimeMinutes min (130 wpm)"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timestamps Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (createdAt > 0L) {
                    StatItem(
                        icon = Icons.Filled.CalendarToday,
                        label = "Created",
                        value = DateFormatter.formatFull(createdAt)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                }

                if (updatedAt > 0L) {
                    StatItem(
                        icon = Icons.Filled.CalendarToday,
                        label = "Modified",
                        value = DateFormatter.formatFull(updatedAt)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
