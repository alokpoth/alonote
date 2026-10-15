package com.noto.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noto.app.data.local.entity.Note
import com.noto.app.data.repository.NoteRepository
import com.noto.app.ui.navigation.BottomTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for the Main Screen.
 */
data class MainUiState(
    val notes: List<Note> = emptyList(),
    val currentTab: BottomTab = BottomTab.NOTES,
    val searchQuery: String = "",
    val isSearchBarVisible: Boolean = false,
    val recentlyDeletedNote: Note? = null
)

/**
 * ViewModel managing the note list, search filtering, tabs, and deletion undo.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(BottomTab.NOTES)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchBarVisible = MutableStateFlow(false)
    val isSearchBarVisible: StateFlow<Boolean> = _isSearchBarVisible.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _recentlyDeletedNote = MutableStateFlow<Note?>(null)
    val recentlyDeletedNote: StateFlow<Note?> = _recentlyDeletedNote.asStateFlow()

    // Debounced search query to prevent unnecessary queries during rapid typing
    private val debouncedQuery = _searchQuery
        .debounce(300L)

    // Flow that dynamically queries the repository based on active tab and search query
    val notes: StateFlow<List<Note>> = combine(_currentTab, debouncedQuery) { tab, query ->
        Pair(tab, query)
    }.flatMapLatest { (tab, query) ->
        if (query.isBlank()) {
            if (tab == BottomTab.FAVORITES) {
                repository.getFavoriteNotes()
            } else {
                repository.getAllNotes()
            }
        } else {
            repository.searchNotes(
                query = query,
                favoritesOnly = tab == BottomTab.FAVORITES
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun selectTab(tab: BottomTab) {
        if (tab == BottomTab.NEW_NOTE) return
        _currentTab.value = tab
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearchBar() {
        val nextVisible = !_isSearchBarVisible.value
        _isSearchBarVisible.value = nextVisible
        if (!nextVisible) {
            _searchQuery.value = ""
        }
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val copy = note.copy(
                id = 0L,
                title = if (note.title.isNotBlank()) "${note.title} (Copy)" else "",
                createdAt = now,
                updatedAt = now
            )
            repository.saveNote(copy)
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            repository.setFavorite(note.id, !note.isFavorite)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.setPinned(note.id, !note.isPinned)
        }
    }

    fun deleteNoteWithUndo(note: Note, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _recentlyDeletedNote.value = note
            repository.deleteNote(note)
            onDeleted()
        }
    }

    fun undoDelete() {
        val noteToRestore = _recentlyDeletedNote.value ?: return
        viewModelScope.launch {
            repository.saveNote(noteToRestore)
            _recentlyDeletedNote.value = null
        }
    }

    fun clearRecentlyDeleted() {
        _recentlyDeletedNote.value = null
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
