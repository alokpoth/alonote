package com.noto.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.data.local.entity.Note
import com.noto.app.util.DateFormatter
import com.noto.app.util.ReminderManager

/**
 * Minimalist Apple-inspired note card with direct, visible UI actions
 * for Pin, Star/Favorite, Delete, and Copy (or Restore & Delete Forever in Recycle Bin),
 * hashtag category pills, live search query highlighting, and tactile haptic feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit = {},
    onPermanentDelete: () -> Unit = {},
    searchQuery: String = "",
    onTagClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                if (note.isPinned && !note.isDeleted) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                cardShape
            )
            .border(
                width = if (note.isPinned && !note.isDeleted) 1.5.dp else 1.dp,
                color = if (note.isPinned && !note.isDeleted) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = cardShape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Attached Photo Preview Banner
            if (note.hasImages) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 10.dp)
                ) {
                    NoteImageThumbnail(
                        filename = note.images.first(),
                        cornerRadius = 12.dp,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (note.images.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+${note.images.size - 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Header Row: Title & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and Pinned / Trash Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (note.isPinned && !note.isDeleted) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PINNED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                               ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (note.isDeleted) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DELETED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    Text(
                        text = remember(note.displayTitle, searchQuery) {
                            highlightQuery(note.displayTitle, searchQuery, highlightColor)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Direct UI Action Buttons Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (note.isDeleted) {
                        // 1. Restore Button
                        IconButton(
                            onClick = onRestore,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestoreFromTrash,
                                contentDescription = "Restore note",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 2. Permanent Delete Button
                        IconButton(
                            onClick = onPermanentDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = "Delete forever",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else {
                        // 1. Visible Pin Button
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (note.isPinned) "Unpin note" else "Pin note to top",
                                tint = if (note.isPinned) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 2. Visible Favorite/Star Button
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (note.isFavorite) "Unmark favorite" else "Mark favorite",
                                tint = if (note.isFavorite) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 3. Visible Delete Button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body Preview Text with search highlighting
            val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            Text(
                text = remember(note.previewText, searchQuery) {
                    highlightQuery(note.previewText, searchQuery, highlightColor)
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Hashtag Pill Badges
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    note.tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .clickable { onTagClick(tag) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (note.tags.size > 3) {
                        Text(
                            text = "+${note.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Date, Word Count Badge, and Quick Copy Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp
                Text(
                    text = if (note.isDeleted && note.deletedAt != null) {
                        "Deleted ${DateFormatter.formatShort(note.deletedAt)}"
                    } else {
                        DateFormatter.formatShort(note.updatedAt)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Checklist Progress Badge
                    if (note.hasChecklist) {
                        val allCompleted = note.completedTasks == note.totalTasks
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (allCompleted) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (allCompleted) "✓ Done" else "✓ ${note.completedTasks}/${note.totalTasks}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Audio Memo Indicator Pill
                    if (note.hasAudio) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🎙️ ${note.audios.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Reminder Indicator Pill
                    if (note.hasReminder && note.reminderTime != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⏰ ${ReminderManager.formatReminderTime(note.reminderTime)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Word Count Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${note.wordCount} words",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Copy Icon Button
                    IconButton(
                        onClick = {
                            val fullContent = buildString {
                                if (note.title.isNotBlank()) appendLine(note.title)
                                if (note.body.isNotBlank()) append(note.body)
                            }.trim()
                            clipboardManager.setText(AnnotatedString(fullContent))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Highlights matches of [query] in [text] with [highlightColor].
 */
private fun highlightQuery(
    text: String,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty() || text.isEmpty()) return AnnotatedString(text)

    val builder = AnnotatedString.Builder(text)
    var startIndex = text.indexOf(trimmed, ignoreCase = true)
    var count = 0
    val textLen = text.length
    while (startIndex in 0 until textLen && count < 100) {
        count++
        val endIndex = (startIndex + trimmed.length).coerceAtMost(textLen)
        if (endIndex > startIndex) {
            builder.addStyle(
                SpanStyle(
                    background = highlightColor,
                    fontWeight = FontWeight.Bold
                ),
                startIndex,
                endIndex
            )
        }
        val nextSearch = if (endIndex > startIndex) endIndex else startIndex + 1
        if (nextSearch >= textLen) break
        startIndex = text.indexOf(trimmed, nextSearch, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}

