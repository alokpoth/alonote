package com.noto.app.test

import com.noto.app.data.local.dao.NoteDao
import com.noto.app.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory Fake implementation of NoteDao for fast, reliable JVM unit tests.
 */
class FakeNoteDao : NoteDao {

    val notesFlow = MutableStateFlow<Map<Long, Note>>(emptyMap())
    private var nextId = 1L

    override fun getAllNotes(): Flow<List<Note>> {
        return notesFlow.map { map ->
            map.values.sortedByDescending { it.updatedAt }
        }
    }

    override fun getFavoriteNotes(): Flow<List<Note>> {
        return notesFlow.map { map ->
            map.values.filter { it.isFavorite }.sortedByDescending { it.updatedAt }
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return notesFlow.map { map ->
            map.values.filter {
                it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
            }.sortedByDescending { it.updatedAt }
        }
    }

    override fun searchFavoriteNotes(query: String): Flow<List<Note>> {
        return notesFlow.map { map ->
            map.values.filter {
                it.isFavorite && (it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true))
            }.sortedByDescending { it.updatedAt }
        }
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return notesFlow.map { it[id] }
    }

    override suspend fun getNoteByIdOnce(id: Long): Note? {
        return notesFlow.value[id]
    }

    override suspend fun insertNote(note: Note): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        val newNote = note.copy(id = id)
        notesFlow.value = notesFlow.value + (id to newNote)
        return id
    }

    override suspend fun updateNote(note: Note) {
        notesFlow.value = notesFlow.value + (note.id to note)
    }

    override suspend fun deleteNote(note: Note) {
        notesFlow.value = notesFlow.value - note.id
    }

    override suspend fun deleteNoteById(id: Long) {
        notesFlow.value = notesFlow.value - id
    }

    override suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long) {
        val existing = notesFlow.value[id] ?: return
        notesFlow.value = notesFlow.value + (id to existing.copy(isFavorite = isFavorite, updatedAt = updatedAt))
    }
}
