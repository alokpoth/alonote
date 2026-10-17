package com.noto.app.ui.editor

import com.noto.app.data.local.entity.Note
import com.noto.app.data.repository.NoteRepository
import com.noto.app.data.repository.NoteRepositoryImpl
import com.noto.app.test.FakeNoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeNoteDao
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeNoteDao()
        repository = NoteRepositoryImpl(fakeDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyDraft_isNotSavedToDatabase() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(initialNoteId = 0L, repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // User does not type anything and exits
        viewModel.flushSave()
        testDispatcher.scheduler.advanceUntilIdle()

        // Database should be completely empty
        val notesInDb = fakeDao.notesFlow.value
        assertTrue(notesInDb.isEmpty())
    }

    @Test
    fun typingInNewNote_autosavesAfterDebounce() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(initialNoteId = 0L, repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTitleChange("Quick Note")
        viewModel.onBodyChange("Remember to call mom")

        // Before debounce delay (400ms), nothing is saved yet
        advanceTimeBy(300L)
        assertTrue(fakeDao.notesFlow.value.isEmpty())

        // After debounce delay expires, note is saved
        advanceTimeBy(300L)
        testDispatcher.scheduler.advanceUntilIdle()

        val savedNotes = fakeDao.notesFlow.value.values.toList()
        assertEquals(1, savedNotes.size)
        assertEquals("Quick Note", savedNotes.first().title)
        assertEquals("Remember to call mom", savedNotes.first().body)
        assertTrue(viewModel.uiState.value.noteId > 0L)
        assertEquals(SaveStatus.SAVED, viewModel.uiState.value.saveStatus)
    }

    @Test
    fun clearingExistingNote_removesFromDatabase() = runTest(testDispatcher) {
        // Pre-insert an existing note
        val note = Note(id = 1L, title = "To be erased", body = "Some content")
        fakeDao.insertNote(note)

        val viewModel = NoteEditorViewModel(initialNoteId = 1L, repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("To be erased", viewModel.uiState.value.title)

        // User completely empties the note
        viewModel.onTitleChange("")
        viewModel.onBodyChange("")
        viewModel.flushSave()
        testDispatcher.scheduler.advanceUntilIdle()

        // The empty note should be deleted from DB
        val noteInDb = fakeDao.getNoteByIdOnce(1L)
        assertNull(noteInDb)
    }

    @Test
    fun deleteNote_movesToRecycleBin() = runTest(testDispatcher) {
        val note = Note(id = 2L, title = "To Trash", body = "Going to bin")
        fakeDao.insertNote(note)

        val viewModel = NoteEditorViewModel(initialNoteId = 2L, repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        var deletedCalled = false
        viewModel.deleteNote { deletedCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(deletedCalled)
        val noteInDb = fakeDao.getNoteByIdOnce(2L)
        assertNotNull(noteInDb)
        assertTrue(noteInDb?.isDeleted == true)
        assertNotNull(noteInDb?.deletedAt)
    }

    @Test
    fun toggleFavorite_updatesStatus() = runTest(testDispatcher) {
        val note = Note(id = 5L, title = "Important Note", body = "Must remember", isFavorite = false)
        fakeDao.insertNote(note)

        val viewModel = NoteEditorViewModel(initialNoteId = 5L, repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFavorite)

        viewModel.toggleFavorite()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFavorite)
        val noteInDb = fakeDao.getNoteByIdOnce(5L)
        assertTrue(noteInDb?.isFavorite == true)
    }

    @Test
    fun attachingImages_triggersAutosaveAndStoresPaths() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(initialNoteId = 0L, repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.attachImages(listOf("img_1.jpg", "img_2.jpg"))
        viewModel.flushSave()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.images.size)
        assertEquals("img_1.jpg,img_2.jpg", viewModel.uiState.value.imagePaths)

        val savedNotes = fakeDao.notesFlow.value.values.toList()
        assertEquals(1, savedNotes.size)
        assertEquals("img_1.jpg,img_2.jpg", savedNotes.first().imagePaths)
    }

    @Test
    fun noteWithOnlyImages_isNotConsideredBlankAndIsSaved() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(initialNoteId = 0L, repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Title and body are empty, but an image is attached
        viewModel.attachImages(listOf("photo_vacation.jpg"))
        viewModel.flushSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val savedNotes = fakeDao.notesFlow.value.values.toList()
        assertEquals(1, savedNotes.size)
        assertTrue(savedNotes.first().hasImages)
        assertEquals(1, savedNotes.first().images.size)
    }
}
