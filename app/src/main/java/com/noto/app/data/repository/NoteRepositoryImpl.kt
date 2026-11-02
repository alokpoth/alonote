package com.noto.app.data.repository

import android.content.Context
import com.noto.app.data.local.dao.NoteDao
import com.noto.app.data.local.entity.Note
import com.noto.app.util.AudioStorageManager
import com.noto.app.util.ImageStorageManager
import com.noto.app.util.ReminderManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of NoteRepository executing all Room queries on Dispatchers.IO.
 * Automatically cleans up attached images, audio memos, and cancels alarms upon permanent deletion.
 */
class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val context: Context? = null
) : NoteRepository {

    constructor(noteDao: NoteDao, context: Context?, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
        this(noteDao, ioDispatcher, context)

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().flowOn(ioDispatcher)
    }

    override fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().flowOn(ioDispatcher)
    }

    override fun getTrashNotes(): Flow<List<Note>> {
        return noteDao.getTrashNotes().flowOn(ioDispatcher)
    }

    override fun getTrashCount(): Flow<Int> {
        return noteDao.getTrashCount().flowOn(ioDispatcher)
    }

    override fun searchNotes(query: String, favoritesOnly: Boolean, trashOnly: Boolean): Flow<List<Note>> {
        val trimmed = query.trim()
        val flow = when {
            trashOnly -> noteDao.searchTrashNotes(trimmed)
            favoritesOnly -> noteDao.searchFavoriteNotes(trimmed)
            else -> noteDao.searchNotes(trimmed)
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
                updatedAt = now,
                isDeleted = false,
                deletedAt = null
            )
            noteDao.insertNote(newNote)
        } else {
            val updatedNote = note.copy(updatedAt = now)
            noteDao.updateNote(updatedNote)
            note.id
        }
    }

    override suspend fun moveToTrash(id: Long): Unit = withContext(ioDispatcher) {
        noteDao.moveToTrash(id)
    }

    override suspend fun restoreNote(id: Long): Unit = withContext(ioDispatcher) {
        noteDao.restoreNote(id)
    }

    override suspend fun permanentlyDeleteNote(id: Long): Unit = withContext(ioDispatcher) {
        val note = noteDao.getNoteByIdOnce(id)
        if (note != null && context != null) {
            note.images.forEach { ImageStorageManager.deleteImage(context, it) }
            note.audios.forEach { AudioStorageManager.deleteAudio(context, it) }
            ReminderManager.cancelReminder(context, id)
        }
        noteDao.permanentlyDeleteNote(id)
    }

    override suspend fun emptyTrash(): Unit = withContext(ioDispatcher) {
        if (context != null) {
            val trashNotes = noteDao.getTrashNotesSnapshot()
            trashNotes.forEach { note ->
                note.images.forEach { ImageStorageManager.deleteImage(context, it) }
                note.audios.forEach { AudioStorageManager.deleteAudio(context, it) }
                ReminderManager.cancelReminder(context, note.id)
            }
        }
        noteDao.emptyTrash()
    }

    override suspend fun deleteNote(note: Note): Unit = withContext(ioDispatcher) {
        noteDao.moveToTrash(note.id)
    }

    override suspend fun deleteNoteById(id: Long): Unit = withContext(ioDispatcher) {
        noteDao.moveToTrash(id)
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean): Unit = withContext(ioDispatcher) {
        noteDao.updateFavorite(id = id, isFavorite = isFavorite, updatedAt = System.currentTimeMillis())
    }

    override suspend fun setPinned(id: Long, isPinned: Boolean): Unit = withContext(ioDispatcher) {
        noteDao.updatePinned(id = id, isPinned = isPinned, updatedAt = System.currentTimeMillis())
    }

    override suspend fun insertNotes(notes: List<Note>): List<Long> = withContext(ioDispatcher) {
        noteDao.insertNotes(notes)
    }

    override suspend fun getAllNotesSnapshot(): List<Note> = withContext(ioDispatcher) {
        noteDao.getAllNotesSnapshot()
    }

    override suspend fun getAllNotesIncludingTrashSnapshot(): List<Note> = withContext(ioDispatcher) {
        noteDao.getAllNotesIncludingTrashSnapshot()
    }

    override suspend fun updateReminder(id: Long, reminderTime: Long?): Unit = withContext(ioDispatcher) {
        noteDao.updateReminder(id = id, reminderTime = reminderTime, updatedAt = System.currentTimeMillis())
    }
}
