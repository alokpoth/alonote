package com.noto.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noto.app.data.local.entity.Note
import com.noto.app.data.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Status indicator for autosave operations.
 */
enum class SaveStatus {
    IDLE,
    SAVING,
    SAVED
}

/**
 * UI State for the Note Editor.
 */
data class NoteEditorUiState(
    val noteId: Long = 0L,
    val title: String = "",
    val body: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val imagePaths: String = "",
    val audioPaths: String = "",
    val reminderTime: Long? = null,
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L,
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val isLoaded: Boolean = false,
    val isNewNote: Boolean = true
) {
    val images: List<String>
        get() = if (imagePaths.isBlank()) emptyList() else imagePaths.split(",").filter { it.isNotBlank() }

    val audios: List<String>
        get() = if (audioPaths.isBlank()) emptyList() else audioPaths.split(",").filter { it.isNotBlank() }

    val hasReminder: Boolean
        get() = reminderTime != null && reminderTime > 0L
}

/**
 * ViewModel managing note creation, editing, debounced autosave, and empty draft policy.
 */
class NoteEditorViewModel(
    private val initialNoteId: Long,
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            noteId = initialNoteId,
            isNewNote = initialNoteId == 0L
        )
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var autosaveJob: Job? = null
    private var hasLoaded = false

    init {
        loadNote()
    }

    private fun loadNote() {
        if (hasLoaded) return
        hasLoaded = true

        if (initialNoteId > 0L) {
            viewModelScope.launch {
                val existing = repository.getNoteByIdOnce(initialNoteId)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            noteId = existing.id,
                            title = existing.title,
                            body = existing.body,
                            isFavorite = existing.isFavorite,
                            isPinned = existing.isPinned,
                            imagePaths = existing.imagePaths,
                            audioPaths = existing.audioPaths,
                            reminderTime = existing.reminderTime,
                            createdAt = existing.createdAt,
                            lastUpdated = existing.updatedAt,
                            saveStatus = SaveStatus.SAVED,
                            isLoaded = true,
                            isNewNote = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoaded = true, isNewNote = true) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoaded = true, isNewNote = true) }
        }
    }

    /**
     * Updates title and schedules debounced autosave without triggering animation jank during typing.
     */
    fun onTitleChange(newTitle: String) {
        if (_uiState.value.title == newTitle) return
        _uiState.update { it.copy(title = newTitle) }
        scheduleAutosave()
    }

    /**
     * Updates body and schedules debounced autosave without triggering animation jank during typing.
     */
    fun onBodyChange(newBody: String) {
        if (_uiState.value.body == newBody) return
        _uiState.update { it.copy(body = newBody) }
        scheduleAutosave()
    }

    /**
     * Toggles favorite status and immediately persists.
     */
    fun toggleFavorite() {
        val newFav = !_uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = newFav) }

        val currentId = _uiState.value.noteId
        if (currentId > 0L) {
            viewModelScope.launch {
                repository.setFavorite(currentId, newFav)
            }
        } else {
            scheduleAutosave()
        }
    }

    /**
     * Toggles pinned status and immediately persists.
     */
    fun togglePin() {
        val newPinned = !_uiState.value.isPinned
        _uiState.update { it.copy(isPinned = newPinned) }

        val currentId = _uiState.value.noteId
        if (currentId > 0L) {
            viewModelScope.launch {
                repository.setPinned(currentId, newPinned)
            }
        } else {
            scheduleAutosave()
        }
    }

    /**
     * Attaches newly saved image filenames and immediately triggers autosave.
     */
    fun attachImages(newFilenames: List<String>) {
        if (newFilenames.isEmpty()) return
        val existing = _uiState.value.images
        val combined = (existing + newFilenames).distinct()
        _uiState.update { it.copy(imagePaths = combined.joinToString(",")) }
        scheduleAutosave()
    }

    /**
     * Removes an attached image by filename and cleans up its internal file.
     */
    fun removeImage(filename: String, context: android.content.Context) {
        val existing = _uiState.value.images
        val updated = existing.filter { it != filename }
        _uiState.update { it.copy(imagePaths = updated.joinToString(",")) }
        com.noto.app.util.ImageStorageManager.deleteImage(context, filename)
        scheduleAutosave()
    }

    /**
     * Attaches newly saved audio voice recording filename and triggers autosave.
     */
    fun attachAudio(filename: String) {
        val existing = _uiState.value.audios
        val combined = (existing + filename).distinct()
        _uiState.update { it.copy(audioPaths = combined.joinToString(",")) }
        scheduleAutosave()
    }

    /**
     * Removes an attached audio memo and deletes its internal file.
     */
    fun removeAudio(filename: String, context: android.content.Context) {
        val existing = _uiState.value.audios
        val updated = existing.filter { it != filename }
        _uiState.update { it.copy(audioPaths = updated.joinToString(",")) }
        com.noto.app.util.AudioStorageManager.deleteAudio(context, filename)
        scheduleAutosave()
    }

    /**
     * Schedules or clears reminder for this note.
     */
    fun setReminder(timestamp: Long?, context: android.content.Context) {
        _uiState.update { it.copy(reminderTime = timestamp) }
        viewModelScope.launch {
            var currentId = _uiState.value.noteId
            if (currentId == 0L) {
                val now = System.currentTimeMillis()
                val noteToSave = Note(
                    id = 0L,
                    title = _uiState.value.title,
                    body = _uiState.value.body,
                    isFavorite = _uiState.value.isFavorite,
                    isPinned = _uiState.value.isPinned,
                    isDeleted = false,
                    deletedAt = null,
                    imagePaths = _uiState.value.imagePaths,
                    audioPaths = _uiState.value.audioPaths,
                    reminderTime = timestamp,
                    createdAt = now,
                    updatedAt = now
                )
                currentId = repository.saveNote(noteToSave)
                _uiState.update {
                    it.copy(
                        noteId = currentId,
                        lastUpdated = now,
                        createdAt = now,
                        saveStatus = SaveStatus.SAVED,
                        isNewNote = false
                    )
                }
            }

            if (timestamp != null && timestamp > System.currentTimeMillis()) {
                if (currentId > 0L) {
                    com.noto.app.util.ReminderManager.scheduleReminder(
                        context = context,
                        noteId = currentId,
                        title = _uiState.value.title.ifBlank { "Note Reminder" },
                        body = _uiState.value.body,
                        triggerAtMillis = timestamp
                    )
                }
            } else {
                if (currentId > 0L) {
                    com.noto.app.util.ReminderManager.cancelReminder(context, currentId)
                }
            }
        }
        scheduleAutosave()
    }

    /**
     * Schedules debounced autosave (400ms after user stops typing).
     */
    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(400L)
            _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
            saveCurrentNote()
        }
    }

    /**
     * Flushes any pending changes immediately to database without delay.
     * Called when exiting editor, pressing back, or when activity is backgrounded.
     */
    fun flushSave() {
        autosaveJob?.cancel()
        saveCurrentNote()
    }

    /**
     * Persists note adhering to the sensible empty-draft policy:
     * - If note has never been saved and title, body, images, and audio are blank, do NOT save it to DB.
     * - If note exists in DB and both title & body are cleared completely, delete it to keep DB tidy.
     * - Otherwise, inserts or updates note, preserving unique ID to prevent duplicates.
     */
    private fun saveCurrentNote() {
        val currentState = _uiState.value
        val title = currentState.title
        val body = currentState.body
        val isBlank = title.isBlank() && body.isBlank() && currentState.imagePaths.isBlank() && currentState.audioPaths.isBlank()
        val currentId = currentState.noteId

        if (isBlank) {
            if (currentId > 0L) {
                // User completely emptied an existing note; permanently remove it from database
                viewModelScope.launch {
                    repository.permanentlyDeleteNote(currentId)
                    _uiState.update {
                        it.copy(
                            noteId = 0L,
                            saveStatus = SaveStatus.IDLE,
                            isNewNote = true
                        )
                    }
                }
            } else {
                // Empty draft; leave database clean
                _uiState.update { it.copy(saveStatus = SaveStatus.IDLE) }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
            val now = System.currentTimeMillis()
            val noteToSave = Note(
                id = currentId,
                title = title,
                body = body,
                isFavorite = currentState.isFavorite,
                isPinned = currentState.isPinned,
                isDeleted = false,
                deletedAt = null,
                imagePaths = currentState.imagePaths,
                audioPaths = currentState.audioPaths,
                reminderTime = currentState.reminderTime,
                createdAt = if (currentState.createdAt == 0L) now else currentState.createdAt,
                updatedAt = now
            )

            val savedId = repository.saveNote(noteToSave)
            _uiState.update {
                it.copy(
                    noteId = savedId,
                    lastUpdated = now,
                    createdAt = noteToSave.createdAt,
                    saveStatus = SaveStatus.SAVED,
                    isNewNote = false
                )
            }
        }
    }

    /**
     * Moves the currently opened note to the Recycle Bin.
     */
    fun deleteNote(onDeleted: () -> Unit) {
        autosaveJob?.cancel()
        val currentId = _uiState.value.noteId
        if (currentId > 0L) {
            viewModelScope.launch {
                repository.moveToTrash(currentId)
                onDeleted()
            }
        } else {
            onDeleted()
        }
    }

    override fun onCleared() {
        flushSave()
        super.onCleared()
    }

    class Factory(
        private val noteId: Long,
        private val repository: NoteRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(noteId, repository) as T
        }
    }
}
