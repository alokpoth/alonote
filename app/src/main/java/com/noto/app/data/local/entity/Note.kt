package com.noto.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a single note stored locally in the Room database.
 *
 * @property id Unique auto-generated identifier.
 * @property title The title of the note.
 * @property body The multiline body text of the note.
 * @property isFavorite Whether the note has been starred as a favorite.
 * @property createdAt Timestamp in milliseconds when the note was first created.
 * @property updatedAt Timestamp in milliseconds when the note was last modified.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns true if the note contains neither title nor body content.
     */
    val isEmpty: Boolean
        get() = title.isBlank() && body.isBlank()

    /**
     * Resolves the primary title to display in note lists.
     * Falls back to the first non-empty line of the body, or "Untitled Note".
     */
    val displayTitle: String
        get() {
            if (title.isNotBlank()) return title.trim()
            val firstLine = body.lines().firstOrNull { it.isNotBlank() }?.trim()
            return if (!firstLine.isNullOrBlank()) {
                if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine
            } else {
                "Untitled Note"
            }
        }

    /**
     * Resolves a short preview of the note content.
     * If the title was explicitly set, previews the body. If the title was empty,
     * previews subsequent lines of the body.
     */
    val previewText: String
        get() {
            val nonBlankLines = body.lines().filter { it.isNotBlank() }
            return if (title.isNotBlank()) {
                nonBlankLines.firstOrNull()?.trim() ?: "No additional text"
            } else {
                if (nonBlankLines.size > 1) {
                    nonBlankLines.drop(1).first().trim()
                } else {
                    "No additional text"
                }
            }
        }
}
