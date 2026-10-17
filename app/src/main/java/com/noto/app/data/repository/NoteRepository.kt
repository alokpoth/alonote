package com.noto.app.data.repository

import com.noto.app.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining note data management operations,
 * including active notes and Recycle Bin lifecycle.
 */
interface NoteRepository {

    /**
     * Observes all active (non-deleted) notes ordered by pinned and last updated timestamp descending.
     */
    fun getAllNotes(): Flow<List<Note>>

    /**
     * Observes all active notes marked as favorites.
     */
    fun getFavoriteNotes(): Flow<List<Note>>

    /**
     * Observes all soft-deleted notes in the Recycle Bin.
     */
    fun getTrashNotes(): Flow<List<Note>>

    /**
     * Observes the count of notes currently in the Recycle Bin.
     */
    fun getTrashCount(): Flow<Int>

    /**
     * Searches notes by matching text against title or body.
     */
    fun searchNotes(query: String, favoritesOnly: Boolean = false, trashOnly: Boolean = false): Flow<List<Note>>

    /**
     * Observes a specific note by its unique identifier.
     */
    fun getNoteById(id: Long): Flow<Note?>

    /**
     * Reads a specific note once from the database.
     */
    suspend fun getNoteByIdOnce(id: Long): Note?

    /**
     * Inserts or updates a note.
     */
    suspend fun saveNote(note: Note): Long

    /**
     * Moves a note to the Recycle Bin (soft delete).
     */
    suspend fun moveToTrash(id: Long)

    /**
     * Restores a note from the Recycle Bin back to active notes.
     */
    suspend fun restoreNote(id: Long)

    /**
     * Permanently deletes a note by its ID from the database.
     */
    suspend fun permanentlyDeleteNote(id: Long)

    /**
     * Empties all notes currently in the Recycle Bin.
     */
    suspend fun emptyTrash()

    /**
     * Legacy delete - moves to trash by default.
     */
    suspend fun deleteNote(note: Note)

    /**
     * Legacy delete by ID - moves to trash by default.
     */
    suspend fun deleteNoteById(id: Long)

    /**
     * Toggles or sets the favorite status of a note.
     */
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    /**
     * Toggles or sets the pinned status of a note.
     */
    suspend fun setPinned(id: Long, isPinned: Boolean)

    /**
     * Inserts a list of notes for backup restore.
     */
    suspend fun insertNotes(notes: List<Note>): List<Long>

    /**
     * Retrieves an immediate snapshot of active notes.
     */
    suspend fun getAllNotesSnapshot(): List<Note>

    /**
     * Retrieves an immediate snapshot of all notes including trash.
     */
    suspend fun getAllNotesIncludingTrashSnapshot(): List<Note>

    /**
     * Updates the scheduled reminder timestamp of a note.
     */
    suspend fun updateReminder(id: Long, reminderTime: Long?)
}
