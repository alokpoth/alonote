package com.noto.app.data.repository

import com.noto.app.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining note data management operations.
 */
interface NoteRepository {

    /**
     * Observes all notes ordered by last updated timestamp descending.
     */
    fun getAllNotes(): Flow<List<Note>>

    /**
     * Observes all notes marked as favorites ordered by last updated timestamp descending.
     */
    fun getFavoriteNotes(): Flow<List<Note>>

    /**
     * Searches notes by matching text against title or body.
     *
     * @param query Search query text.
     * @param favoritesOnly If true, filters only within starred notes.
     */
    fun searchNotes(query: String, favoritesOnly: Boolean = false): Flow<List<Note>>

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
     * If note.id == 0L, performs an insert; otherwise updates existing record.
     * Automatically sets updatedAt timestamp.
     *
     * @return The ID of the saved note.
     */
    suspend fun saveNote(note: Note): Long

    /**
     * Deletes a note entity from local database.
     */
    suspend fun deleteNote(note: Note)

    /**
     * Deletes a note by its ID.
     */
    suspend fun deleteNoteById(id: Long)

    /**
     * Toggles or sets the favorite status of a note.
     */
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
}
