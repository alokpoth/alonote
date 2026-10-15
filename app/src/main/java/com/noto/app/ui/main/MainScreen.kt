package com.noto.app.ui.main

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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.data.local.entity.Note
import com.noto.app.data.repository.ThemeMode
import com.noto.app.ui.components.EmptyStateView
import com.noto.app.ui.components.NoteActionDialog
import com.noto.app.ui.components.NoteCard
import com.noto.app.ui.components.NotoBottomBar
import com.noto.app.ui.components.NotoSearchBar
import com.noto.app.ui.components.rememberReorderableState
import com.noto.app.ui.components.reorderableItem
import com.noto.app.ui.navigation.BottomTab
import kotlinx.coroutines.launch

/**
 * Filter mode for quick filter chips in the main screen.
 */
enum class QuickFilter {
    ALL,
    PINNED,
    FAVORITES
}

/**
 * Main screen displaying user's notes with search, tabs, floating bottom navigation bar,
 * grid/list view mode toggle, filter chips, theme switcher (Light/Dark),
 * and drag-to-reorder (press and hold up/down).
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNoteClick: (Long) -> Unit,
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchBarVisible by viewModel.isSearchBarVisible.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var quickFilter by remember { mutableStateOf(QuickFilter.ALL) }
    var selectedNoteForOptions by remember { mutableStateOf<Note?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableState(
        lazyListState = lazyListState,
        onMove = { fromIndex, toIndex ->
            viewModel.reorderNotes(fromIndex, toIndex)
        }
    )

    // Apply quick chip filter in addition to active tab
    val displayedNotes = remember(notes, quickFilter) {
        when (quickFilter) {
            QuickFilter.ALL -> notes
            QuickFilter.PINNED -> notes.filter { it.isPinned }
            QuickFilter.FAVORITES -> notes.filter { it.isFavorite }
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
            // App Header with Title, Count, Theme Toggle, Grid/List view toggle, and Search button
            HeaderSection(
                tab = currentTab,
                noteCount = displayedNotes.size,
                isGridView = isGridView,
                isSearchBarVisible = isSearchBarVisible,
                themeMode = themeMode,
                onToggleTheme = { viewModel.toggleTheme() },
                onToggleViewMode = { viewModel.toggleViewMode() },
                onSearchToggle = { viewModel.toggleSearchBar() }
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
                        placeholder = if (currentTab == BottomTab.FAVORITES) "Search favorites..." else "Search notes..."
                    )
                }
            }

            // Quick Filter Chips Row (All, Pinned, Favorites)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pinnedCount = notes.count { it.isPinned }
                val favCount = notes.count { it.isFavorite }

                FilterChipItem(
                    label = "All (${notes.size})",
                    isSelected = quickFilter == QuickFilter.ALL,
                    onClick = { quickFilter = QuickFilter.ALL }
                )

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
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Content List or Empty State
            if (displayedNotes.isEmpty()) {
                EmptyStateView(
                    tab = currentTab,
                    isSearching = searchQuery.isNotBlank(),
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
                                onClick = { onNoteClick(note.id) },
                                onLongClick = { selectedNoteForOptions = note },
                                onTogglePin = { viewModel.togglePin(note) },
                                onFavoriteToggle = { viewModel.toggleFavorite(note) },
                                onDelete = {
                                    viewModel.deleteNoteWithUndo(note) {
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Note deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                }
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
                                onClick = { onNoteClick(note.id) },
                                onLongClick = { selectedNoteForOptions = note },
                                onTogglePin = { viewModel.togglePin(note) },
                                onFavoriteToggle = { viewModel.toggleFavorite(note) },
                                onDelete = {
                                    viewModel.deleteNoteWithUndo(note) {
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Note deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                },
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

    // Action sheet dialog (Pin, Star/Favorite, Delete)
    selectedNoteForOptions?.let { note ->
        NoteActionDialog(
            note = note,
            onTogglePin = { viewModel.togglePin(note) },
            onToggleFavorite = { viewModel.toggleFavorite(note) },
            onDelete = {
                viewModel.deleteNoteWithUndo(note) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Note deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
            },
            onDismiss = { selectedNoteForOptions = null }
        )
    }
}

@Composable
private fun HeaderSection(
    tab: BottomTab,
    noteCount: Int,
    isGridView: Boolean,
    isSearchBarVisible: Boolean,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSearchToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title = when (tab) {
            BottomTab.FAVORITES -> "Favorites"
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
