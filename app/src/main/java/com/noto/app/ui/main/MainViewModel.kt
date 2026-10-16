package com.noto.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noto.app.data.local.entity.Note
import com.noto.app.data.repository.NoteRepository
import com.noto.app.data.repository.ThemeMode
import com.noto.app.data.repository.ThemeRepository
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
 * Available sort orders for notes.
 */
enum class NoteSortOrder(val label: String) {
    RECENTLY_MODIFIED("Recently Edited"),
    DATE_CREATED("Date Created"),
    TITLE_A_Z("Title (A-Z)"),
    TODO_PROGRESS("Task Progress")
}

/**
 * ViewModel managing active notes, Recycle Bin (soft delete, restore, empty trash),
 * sorting, hashtag filtering, search, tabs, theme switching, and deletion undo.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(
    private val repository: NoteRepository,
    private val themeRepository: ThemeRepository? = null
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        themeRepository?.themeMode ?: MutableStateFlow(ThemeMode.SYSTEM)

    private val _currentTab = MutableStateFlow(BottomTab.NOTES)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _isTrashFilter = MutableStateFlow(false)
    val isTrashFilter: StateFlow<Boolean> = _isTrashFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(NoteSortOrder.RECENTLY_MODIFIED)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _isSearchBarVisible = MutableStateFlow(false)
    val isSearchBarVisible: StateFlow<Boolean> = _isSearchBarVisible.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _recentlyDeletedNote = MutableStateFlow<Note?>(null)
    val recentlyDeletedNote: StateFlow<Note?> = _recentlyDeletedNote.asStateFlow()

    // Live count of notes in Recycle Bin
    val trashCount: StateFlow<Int> = repository.getTrashCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    // Debounced search query to prevent unnecessary queries during rapid typing
    private val debouncedQuery = _searchQuery
        .debounce(300L)

    // Raw database notes flow based on tab, trash mode, and search
    private val rawNotes = combine(_currentTab, _isTrashFilter, debouncedQuery) { tab, isTrash, query ->
        Triple(tab, isTrash, query)
    }.flatMapLatest { (tab, isTrash, query) ->
        if (query.isBlank()) {
            when {
                isTrash -> repository.getTrashNotes()
                tab == BottomTab.FAVORITES -> repository.getFavoriteNotes()
                else -> repository.getAllNotes()
            }
        } else {
            repository.searchNotes(
                query = query,
                favoritesOnly = !isTrash && tab == BottomTab.FAVORITES,
                trashOnly = isTrash
            )
        }
    }

    // Dynamic notes flow supporting sorting and tag filtering
    val notes: StateFlow<List<Note>> = combine(rawNotes, _sortOrder, _selectedTag) { list, sort, tag ->
        val tagFiltered = if (tag.isNullOrBlank()) list else list.filter { it.tags.contains(tag) }
        when (sort) {
            NoteSortOrder.RECENTLY_MODIFIED -> tagFiltered.sortedWith(
                compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
            )
            NoteSortOrder.DATE_CREATED -> tagFiltered.sortedWith(
                compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt }
            )
            NoteSortOrder.TITLE_A_Z -> tagFiltered.sortedWith(
                compareByDescending<Note> { it.isPinned }.thenBy { it.displayTitle.lowercase() }
            )
            NoteSortOrder.TODO_PROGRESS -> tagFiltered.sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { if (it.totalTasks > 0) it.completedTasks.toDouble() / it.totalTasks else -1.0 }
                    .thenByDescending { it.updatedAt }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // All available distinct tags across notes
    val allTags: StateFlow<List<String>> = rawNotes.combine(_isTrashFilter) { list, isTrash ->
        if (isTrash) emptyList() else list.flatMap { it.tags }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun selectTab(tab: BottomTab) {
        if (tab == BottomTab.NEW_NOTE) return
        _isTrashFilter.value = false
        _selectedTag.value = null
        _currentTab.value = tab
    }

    fun setSortOrder(order: NoteSortOrder) {
        _sortOrder.value = order
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }


    fun setTrashFilter(enabled: Boolean) {
        _isTrashFilter.value = enabled
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
                isDeleted = false,
                deletedAt = null,
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

    fun moveToTrash(note: Note, onMoved: () -> Unit) {
        viewModelScope.launch {
            _recentlyDeletedNote.value = note
            repository.moveToTrash(note.id)
            onMoved()
        }
    }

    fun deleteNoteWithUndo(note: Note, onDeleted: () -> Unit) {
        moveToTrash(note, onDeleted)
    }

    fun restoreNote(note: Note, onRestored: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreNote(note.id)
            if (_recentlyDeletedNote.value?.id == note.id) {
                _recentlyDeletedNote.value = null
            }
            onRestored()
        }
    }

    fun permanentlyDeleteNote(note: Note, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.permanentlyDeleteNote(note.id)
            if (_recentlyDeletedNote.value?.id == note.id) {
                _recentlyDeletedNote.value = null
            }
            onDeleted()
        }
    }

    fun emptyTrash(onEmptied: () -> Unit = {}) {
        viewModelScope.launch {
            repository.emptyTrash()
            _recentlyDeletedNote.value = null
            onEmptied()
        }
    }

    fun undoDelete() {
        val noteToRestore = _recentlyDeletedNote.value ?: return
        viewModelScope.launch {
            repository.restoreNote(noteToRestore.id)
            _recentlyDeletedNote.value = null
        }
    }

    fun toggleAllTasks(note: Note, onToggled: () -> Unit = {}) {
        viewModelScope.launch {
            val allDone = note.completedTasks == note.totalTasks && note.totalTasks > 0
            val newBody = if (allDone) {
                // Reset all tasks to pending
                note.body
                    .replace("[x] ", "[ ] ")
                    .replace("- [x] ", "- [ ] ")
            } else {
                // Mark all tasks completed
                note.body
                    .replace("[ ] ", "[x] ")
                    .replace("- [ ] ", "- [x] ")
            }
            val updated = note.copy(body = newBody, updatedAt = System.currentTimeMillis())
            repository.saveNote(updated)
            onToggled()
        }
    }

    fun toggleTheme() {
        themeRepository?.toggleTheme()
    }

    fun reorderNotes(fromIndex: Int, toIndex: Int) {
        if (_isTrashFilter.value) return // Don't reorder trash
        val currentList = notes.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return
        val moved = currentList.removeAt(fromIndex)
        currentList.add(toIndex, moved)

        val baseTime = System.currentTimeMillis()
        viewModelScope.launch {
            currentList.forEachIndexed { index, note ->
                val newTime = baseTime - (index * 1000L)
                if (note.updatedAt != newTime) {
                    repository.saveNote(note.copy(updatedAt = newTime))
                }
            }
        }
    }

    fun clearRecentlyDeleted() {
        _recentlyDeletedNote.value = null
    }

    class Factory(
        private val repository: NoteRepository,
        private val themeRepository: ThemeRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, themeRepository) as T
        }
    }
}
