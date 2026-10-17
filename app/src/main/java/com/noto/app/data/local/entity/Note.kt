package com.noto.app.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.ceil

/**
 * Entity representing a single note stored locally in the Room database.
 *
 * @property id Unique auto-generated identifier.
 * @property title The title of the note.
 * @property body The multiline body text of the note.
 * @property isFavorite Whether the note has been starred as a favorite.
 * @property isPinned Whether the note has been pinned to the top of the list.
 * @property isDeleted Whether the note has been moved to the Recycle Bin.
 * @property deletedAt Timestamp in milliseconds when the note was soft-deleted.
 * @property createdAt Timestamp in milliseconds when the note was first created.
 * @property updatedAt Timestamp in milliseconds when the note was last modified.
 */
@Immutable
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val imagePaths: String = "",
    val audioPaths: String = "",
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * List of attached image filenames stored in the app's private files directory.
     */
    val images: List<String>
        get() = if (imagePaths.isBlank()) emptyList() else imagePaths.split(",").filter { it.isNotBlank() }

    /**
     * True if the note has at least one attached photo.
     */
    val hasImages: Boolean
        get() = images.isNotEmpty()

    /**
     * List of attached audio voice memo filenames stored in the app's private files directory.
     */
    val audios: List<String>
        get() = if (audioPaths.isBlank()) emptyList() else audioPaths.split(",").filter { it.isNotBlank() }

    /**
     * True if the note has at least one attached voice memo.
     */
    val hasAudio: Boolean
        get() = audios.isNotEmpty()

    /**
     * True if the note has a scheduled reminder timestamp in the future.
     */
    val hasReminder: Boolean
        get() = reminderTime != null && reminderTime > 0L

    /**
     * Returns true if the note contains neither title, body, attached photos, nor voice memos.
     */
    val isEmpty: Boolean
        get() = title.isBlank() && body.isBlank() && imagePaths.isBlank() && audioPaths.isBlank()

    /**
     * Calculates the word count of the combined title and body.
     */
    val wordCount: Int
        get() {
            val content = "$title $body".trim()
            return if (content.isEmpty()) 0 else content.split("\\s+".toRegex()).count { it.isNotBlank() }
        }

    /**
     * Calculates the character count of the combined title and body.
     */
    val charCount: Int
        get() = (title.length + body.length)

    /**
     * Estimated reading time in minutes (based on 200 words per minute average).
     */
    val readingTimeMinutes: Int
        get() = maxOf(1, ceil(wordCount / 200.0).toInt())

    /**
     * Total number of checklist task items in the note.
     */
    val totalTasks: Int
        get() = TotalTasksRegex.findAll(body).count()

    /**
     * Number of completed checklist task items in the note.
     */
    val completedTasks: Int
        get() = CompletedTasksRegex.findAll(body).count()

    /**
     * True if the note contains any checklist task items.
     */
    val hasChecklist: Boolean
        get() = totalTasks > 0

    /**
     * Extracted hashtags (e.g. #work, #ideas, #কাজ) from title and body.
     */
    val tags: List<String>
        get() = TagRegex.findAll("$title $body")
            .map { it.groupValues[1] }
            .distinct()
            .toList()

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
     * Strips Markdown formatting symbols for a clean card presentation.
     */
    val previewText: String
        get() {
            val nonBlankLines = body.lines().filter { it.isNotBlank() }
            val raw = if (title.isNotBlank()) {
                nonBlankLines.firstOrNull()?.trim() ?: "No additional text"
            } else {
                if (nonBlankLines.size > 1) {
                    nonBlankLines.drop(1).first().trim()
                } else {
                    "No additional text"
                }
            }
            return raw
                .replace("[ ] ", "☐ ")
                .replace("[x] ", "☑ ")
                .replace("- [ ] ", "☐ ")
                .replace("- [x] ", "☑ ")
                .replace(BoldStripRegex, "$1")
                .replace(ItalicStripRegex, "$1")
                .replace(StrikeStripRegex, "$1")
                .replace(HeadingStripRegex, "")
                .replace(CodeStripRegex, "$1")
        }
}

// Pre-compiled top-level regexes to prevent repeated compilation and thermal overhead
private val TotalTasksRegex = Regex("""(?:^|\n)\s*(?:-\s+)?\[[ x]\]""")
private val CompletedTasksRegex = Regex("""(?:^|\n)\s*(?:-\s+)?\[x\]""")
private val BoldStripRegex = Regex("""\*\*(.*?)\*\*""")
private val ItalicStripRegex = Regex("""\*(.*?)\*""")
private val StrikeStripRegex = Regex("""~~(.*?)~~""")
private val HeadingStripRegex = Regex("""^#{1,6}\s+""")
private val CodeStripRegex = Regex("""`([^`]+)`""")
private val TagRegex = Regex("""#([\p{L}\p{N}_]+)""")

