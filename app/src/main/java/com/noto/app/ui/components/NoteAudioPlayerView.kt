package com.noto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.util.AudioStorageManager
import com.noto.app.util.NoteAudioPlayer

/**
 * Self-contained audio player item that collects playback position and state locally.
 * Prevents triggering full-screen recomposition storms on the parent editor or reader screen.
 */
@Composable
fun NoteAudioPlayerItem(
    filename: String,
    audioPlayer: NoteAudioPlayer,
    modifier: Modifier = Modifier,
    showRemoveButton: Boolean = true,
    onRemove: (() -> Unit)? = null
) {
    val isAudioPlaying by audioPlayer.isPlaying.collectAsState()
    val currentPlayingFile by audioPlayer.currentPlayingFile.collectAsState()
    val isThisPlaying = isAudioPlaying && currentPlayingFile == filename

    val currentPositionMs by if (isThisPlaying) audioPlayer.currentPositionMs.collectAsState() else remember { mutableIntStateOf(0) }
    val durationMs by if (isThisPlaying) audioPlayer.durationMs.collectAsState() else remember { mutableIntStateOf(0) }

    NoteAudioPlayerItem(
        filename = filename,
        isPlaying = isThisPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        onTogglePlay = { audioPlayer.togglePlay(filename) },
        onSeek = { if (isThisPlaying) audioPlayer.seekTo(it) },
        modifier = modifier,
        showRemoveButton = showRemoveButton,
        onRemove = onRemove
    )
}

/**
 * Minimalist Apple-inspired in-note audio voice memo player item.
 * Supports play, pause, seeking, real-time duration updates, and deletion.
 */
@Composable
fun NoteAudioPlayerItem(
    filename: String,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showRemoveButton: Boolean = true,
    onRemove: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    val safeDuration = if (durationMs > 0) durationMs else 1
    val progress = (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Circle Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePlay()
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Progress bar + Timestamps
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Voice Memo",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${AudioStorageManager.formatDuration(currentPositionMs)} / ${AudioStorageManager.formatDuration(durationMs)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    onSeek((newProgress * safeDuration).toInt())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // Remove Button
        if (showRemoveButton && onRemove != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove voice memo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
