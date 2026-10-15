package com.noto.app.data.repository

import com.noto.app.data.local.dao.NoteDao
import com.noto.app.data.local.entity.Note
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of NoteRepository executing all Room queries on Dispatchers.IO.
 */
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().flowOn(ioDispatcher)
    }

    override fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().flowOn(ioDispatcher)
    }

    override fun searchNotes(query: String, favoritesOnly: Boolean): Flow<List<Note>> {
        val trimmed = query.trim()
        val flow = if (favoritesOnly) {
            noteDao.searchFavoriteNotes(trimmed)
        } else {
            noteDao.searchNotes(trimmed)
        }
        return flow.flowOn(ioDispatcher)
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return noteDao.getNoteById(id).flowOn(ioDispatcher)
    }

    override suspend fun getNoteByIdOnce(id: Long): Note? = withContext(ioDispatcher) {
        noteDao.getNoteByIdOnce(id)
    }

    override suspend fun saveNote(note: Note): Long = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        if (note.id == 0L) {
            val newNote = note.copy(
                createdAt = if (note.createdAt == 0L) now else note.createdAt,
                updatedAt = now
            )
            noteDao.insertNote(newNote)
        } else {
            val updatedNote = note.copy(updatedAt = now)
            noteDao.updateNote(updatedNote)
            note.id
        }
    }

    override suspend fun deleteNote(note: Note): Unit = withContext(ioDispatcher) {
        noteDao.deleteNote(note)
    }

    override suspend fun deleteNoteById(id: Long): Unit = withContext(ioDispatcher) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean): Unit = withContext(ioDispatcher) {
        noteDao.updateFavorite(id = id, isFavorite = isFavorite, updatedAt = System.currentTimeMillis())
    }
}
