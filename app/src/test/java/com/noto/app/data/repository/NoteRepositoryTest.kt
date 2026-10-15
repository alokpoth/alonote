package com.noto.app.data.repository

import com.noto.app.data.local.entity.Note
import com.noto.app.test.FakeNoteDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteRepositoryTest {

    private lateinit var fakeDao: FakeNoteDao
    private lateinit var repository: NoteRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeDao = FakeNoteDao()
        repository = NoteRepositoryImpl(fakeDao, testDispatcher)
    }

    @Test
    fun saveNote_insertsNewNote_andGeneratesId() = runTest(testDispatcher) {
        val note = Note(title = "Shopping List", body = "Milk, Eggs, Bread")
        val savedId = repository.saveNote(note)

        assertTrue(savedId > 0L)
        val fetched = repository.getNoteByIdOnce(savedId)
        assertNotNull(fetched)
        assertEquals("Shopping List", fetched?.title)
        assertEquals("Milk, Eggs, Bread", fetched?.body)
        assertFalse(fetched?.isFavorite ?: true)
    }

    @Test
    fun saveNote_updatesExistingNote() = runTest(testDispatcher) {
        val note = Note(title = "Idea", body = "Initial draft")
        val savedId = repository.saveNote(note)

        val updatedNote = repository.getNoteByIdOnce(savedId)!!.copy(body = "Revised draft")
        repository.saveNote(updatedNote)

        val result = repository.getNoteByIdOnce(savedId)
        assertEquals("Revised draft", result?.body)
    }

    @Test
    fun deleteNote_removesFromDatabase() = runTest(testDispatcher) {
        val note = Note(title = "To Delete", body = "Goodbye")
        val savedId = repository.saveNote(note)

        val fetched = repository.getNoteByIdOnce(savedId)
        assertNotNull(fetched)

        repository.deleteNote(fetched!!)
        val afterDelete = repository.getNoteByIdOnce(savedId)
        assertNull(afterDelete)
    }

    @Test
    fun setFavorite_togglesFavoriteStatus() = runTest(testDispatcher) {
        val note = Note(title = "Favorite Note", body = "Very important", isFavorite = false)
        val savedId = repository.saveNote(note)

        repository.setFavorite(savedId, true)
        val favoriteNote = repository.getNoteByIdOnce(savedId)
        assertTrue(favoriteNote?.isFavorite == true)

        val favoritesList = repository.getFavoriteNotes().first()
        assertEquals(1, favoritesList.size)
        assertEquals(savedId, favoritesList.first().id)
    }

    @Test
    fun searchNotes_returnsMatchingResults() = runTest(testDispatcher) {
        repository.saveNote(Note(title = "Meeting Notes", body = "Discuss Q4 objectives"))
        repository.saveNote(Note(title = "Recipe", body = "Pasta with garlic and olive oil"))
        repository.saveNote(Note(title = "Goals", body = "Run 5km every morning"))

        val searchResults = repository.searchNotes("pasta").first()
        assertEquals(1, searchResults.size)
        assertEquals("Recipe", searchResults.first().title)

        val searchObjectives = repository.searchNotes("objectives").first()
        assertEquals(1, searchObjectives.size)
        assertEquals("Meeting Notes", searchObjectives.first().title)
    }
}
