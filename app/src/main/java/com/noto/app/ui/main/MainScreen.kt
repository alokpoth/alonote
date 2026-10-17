package com.noto.app.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.data.local.entity.Note
import com.noto.app.data.repository.ThemeMode
import com.noto.app.ui.components.DeleteConfirmationDialog
import com.noto.app.ui.components.EmptyStateView
import com.noto.app.ui.components.NoteActionBottomSheet
import com.noto.app.ui.components.NoteCard
import com.noto.app.ui.components.NotoBottomBar
import com.noto.app.ui.components.NotoSearchBar
import com.noto.app.ui.components.rememberReorderableState
import com.noto.app.ui.components.reorderableItem
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.noto.app.ui.navigation.BottomTab
import kotlinx.coroutines.launch

/**
 * Filter modes for the quick filter chips row.
 */
enum class QuickFilter {
    ALL,
    PINNED,
    FAVORITES,
    TODO,
    TRASH
}

/**
 * Main screen displaying notes with Apple-style action sheet popup, Recycle Bin,
 * smooth animations, drag-to-reorder, grid/list view toggle, and search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNoteClick: (Long) -> Unit,
    onNewNoteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchBarVisible by viewModel.isSearchBarVisible.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val trashCount by viewModel.trashCount.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    var quickFilter by remember { mutableStateOf(QuickFilter.ALL) }
    var selectedNoteForOptions by remember { mutableStateOf<Note?>(null) }
    var noteToPermanentlyDelete by remember { mutableStateOf<Note?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableState(
        lazyListState = lazyListState,
        onMove = { fromIndex, toIndex ->
            viewModel.reorderNotes(fromIndex, toIndex)
        }
    )

    // Sync ViewModel trash filter state with UI quick filter
    LaunchedEffect(quickFilter) {
        viewModel.setTrashFilter(quickFilter == QuickFilter.TRASH)
    }

    // Apply client-side pinned, favorite, or to-do checklist filtering for active notes
    val displayedNotes = remember(notes, quickFilter) {
        when (quickFilter) {
            QuickFilter.ALL, QuickFilter.TRASH -> notes
            QuickFilter.PINNED -> notes.filter { it.isPinned }
            QuickFilter.FAVORITES -> notes.filter { it.isFavorite }
            QuickFilter.TODO -> notes.filter { it.hasChecklist }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        },
        bottomBar = {
            NotoBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    quickFilter = QuickFilter.ALL
                    viewModel.selectTab(tab)
                },
                onNewNoteClick = onNewNoteClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .statusBarsPadding()
        ) {
            // Header: Title, Count, Theme Toggle, Grid/List view toggle, Search, Sort, or Empty Bin
            HeaderSection(
                tab = currentTab,
                quickFilter = quickFilter,
                noteCount = displayedNotes.size,
                trashCount = trashCount,
                isGridView = isGridView,
                isSearchBarVisible = isSearchBarVisible,
                themeMode = themeMode,
                sortOrder = sortOrder,
                onSelectSortOrder = { viewModel.setSortOrder(it) },
                onToggleTheme = { viewModel.toggleTheme() },
                onToggleViewMode = { viewModel.toggleViewMode() },
                onSearchToggle = { viewModel.toggleSearchBar() },
                onSettingsClick = onSettingsClick,
                onEmptyTrash = { showEmptyTrashDialog = true }
            )

            // Collapsible Search Field
            AnimatedVisibility(
                visible = isSearchBarVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    NotoSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = when {
                            quickFilter == QuickFilter.TRASH -> "Search recycle bin..."
                            currentTab == BottomTab.FAVORITES -> "Search favorites..."
                            else -> "Search notes..."
                        }
                    )
                }
            }

            // Quick Filter Chips Row (All, Pinned, Favorites, Recycle Bin)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isTrash = quickFilter == QuickFilter.TRASH
                val activeNotesCount = if (isTrash) notes.size else notes.size
                val pinnedCount = if (!isTrash) notes.count { it.isPinned } else 0
                val favCount = if (!isTrash) notes.count { it.isFavorite } else 0
                val todoCount = if (!isTrash) notes.count { it.hasChecklist } else 0

                FilterChipItem(
                    label = if (isTrash) "Notes" else "All ($activeNotesCount)",
                    isSelected = quickFilter == QuickFilter.ALL,
                    onClick = { quickFilter = QuickFilter.ALL }
                )

                if (!isTrash) {
                    FilterChipItem(
                        label = "📌 Pinned ($pinnedCount)",
                        isSelected = quickFilter == QuickFilter.PINNED,
                        onClick = { quickFilter = QuickFilter.PINNED }
                    )

                    FilterChipItem(
                        label = "⭐ Favorites ($favCount)",
                        isSelected = quickFilter == QuickFilter.FAVORITES,
                        onClick = { quickFilter = QuickFilter.FAVORITES }
                    )

                    FilterChipItem(
                        label = "☑ Tasks ($todoCount)",
                        isSelected = quickFilter == QuickFilter.TODO,
                        onClick = { quickFilter = QuickFilter.TODO }
                    )
                }

                FilterChipItem(
                    label = "🗑️ Bin ($trashCount)",
                    isSelected = quickFilter == QuickFilter.TRASH,
                    onClick = { quickFilter = QuickFilter.TRASH }
                )
            }

            // Hashtag Filter Chips Row (shown if notes have hashtags and not in trash)
            AnimatedVisibility(visible = allTags.isNotEmpty() && quickFilter != QuickFilter.TRASH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allTags.forEach { tag ->
                        val isSelected = selectedTag == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }
                                )
                                .clickable { viewModel.selectTag(tag) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Content List or Empty State
            if (displayedNotes.isEmpty()) {
                EmptyStateView(
                    tab = currentTab,
                    isSearching = searchQuery.isNotBlank(),
                    isTrash = quickFilter == QuickFilter.TRASH,
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (isGridView) {
                    // 2-Column Staggered Grid View
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp
                    ) {
                        items(
                            items = displayedNotes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(
                                note = note,
                                onClick = {
                                    if (quickFilter == QuickFilter.TRASH) {
                                        selectedNoteForOptions = note
                                    } else {
                                        onNoteClick(note.id)
                                    }
                                },
                                onLongClick = { selectedNoteForOptions = note },
                                onTogglePin = { viewModel.togglePin(note) },
                                onFavoriteToggle = { viewModel.toggleFavorite(note) },
                                onDelete = {
                                    viewModel.moveToTrash(note) {
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Note moved to Recycle Bin",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                },
                                onRestore = {
                                    viewModel.restoreNote(note) {
                                        Toast.makeText(context, "Note restored", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPermanentDelete = {
                                    noteToPermanentlyDelete = note
                                },
                                searchQuery = searchQuery,
                                onTagClick = { viewModel.selectTag(it) }
                            )
                        }
                    }
                } else {
                    // 1-Column List View with Press-and-Hold Drag Reordering
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = displayedNotes,
                            key = { _, note -> note.id }
                        ) { index, note ->
                            NoteCard(
                                note = note,
                                onClick = {
                                    if (quickFilter == QuickFilter.TRASH) {
                                        selectedNoteForOptions = note
                                    } else {
                                        onNoteClick(note.id)
                                    }
                                },
                                onLongClick = { selectedNoteForOptions = note },
                                onTogglePin = { viewModel.togglePin(note) },
                                onFavoriteToggle = { viewModel.toggleFavorite(note) },
                                onDelete = {
                                    viewModel.moveToTrash(note) {
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Note moved to Recycle Bin",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                },
                                onRestore = {
                                    viewModel.restoreNote(note) {
                                        Toast.makeText(context, "Note restored", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPermanentDelete = {
                                    noteToPermanentlyDelete = note
                                },
                                searchQuery = searchQuery,
                                onTagClick = { viewModel.selectTag(it) },
                                modifier = Modifier
                                    .animateItem()
                                    .reorderableItem(state = reorderState, index = index)
                            )
                        }
                    }
                }
            }
        }
    }

    // Apple-style Action Sheet Popup
    selectedNoteForOptions?.let { note ->
        NoteActionBottomSheet(
            note = note,
            onDismiss = { selectedNoteForOptions = null },
            onTogglePin = { viewModel.togglePin(note) },
            onToggleFavorite = { viewModel.toggleFavorite(note) },
            onDuplicate = { viewModel.duplicateNote(note) },
            onToggleAllTasks = {
                viewModel.toggleAllTasks(note) {
                    val msg = if (note.completedTasks == note.totalTasks) "Tasks reset to pending" else "All tasks completed"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onMoveToTrash = {
                viewModel.moveToTrash(note) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Note moved to Recycle Bin",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
            },
            onRestore = {
                viewModel.restoreNote(note) {
                    Toast.makeText(context, "Note restored", Toast.LENGTH_SHORT).show()
                }
            },
            onPermanentDelete = {
                noteToPermanentlyDelete = note
            }
        )
    }

    // Permanent Delete Confirmation Dialog
    noteToPermanentlyDelete?.let { note ->
        DeleteConfirmationDialog(
            title = "Delete Forever?",
            message = "This note will be permanently erased. This action cannot be undone.",
            confirmText = "Delete Forever",
            isDestructive = true,
            onConfirm = {
                val target = note
                noteToPermanentlyDelete = null
                viewModel.permanentlyDeleteNote(target) {
                    Toast.makeText(context, "Note permanently deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { noteToPermanentlyDelete = null }
        )
    }

    // Empty Recycle Bin Confirmation Dialog
    if (showEmptyTrashDialog) {
        DeleteConfirmationDialog(
            title = "Empty Recycle Bin?",
            message = "All $trashCount notes in the Recycle Bin will be permanently erased.",
            confirmText = "Empty Bin",
            isDestructive = true,
            onConfirm = {
                showEmptyTrashDialog = false
                viewModel.emptyTrash {
                    Toast.makeText(context, "Recycle Bin emptied", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }
}

@Composable
private fun HeaderSection(
    tab: BottomTab,
    quickFilter: QuickFilter,
    noteCount: Int,
    trashCount: Int,
    isGridView: Boolean,
    isSearchBarVisible: Boolean,
    themeMode: ThemeMode,
    sortOrder: NoteSortOrder,
    onSelectSortOrder: (NoteSortOrder) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSearchToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title = when {
            quickFilter == QuickFilter.TRASH -> "Recycle Bin"
            quickFilter == QuickFilter.TODO -> "Tasks"
            tab == BottomTab.FAVORITES -> "Favorites"
            else -> "Noto"
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Subtitle / Note Count Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$noteCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (quickFilter == QuickFilter.TRASH) {
                // Empty Bin Button (shown when there are notes in the trash)
                if (trashCount > 0) {
                    IconButton(
                        onClick = onEmptyTrash,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Empty Recycle Bin",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Theme Mode Toggle Button (Light / Dark)
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (themeMode == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (themeMode == ThemeMode.DARK) "Switch to Light mode" else "Switch to Dark mode",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Sort Options Dropdown Menu Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort notes",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        NoteSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = order.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (order == sortOrder) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectSortOrder(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                // View Mode Toggle Button (Grid / List)
                IconButton(
                    onClick = onToggleViewMode,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                        contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Search Toggle Button
            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (isSearchBarVisible) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (isSearchBarVisible) "Close search" else "Search notes",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
