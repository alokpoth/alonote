package com.noto.app.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.ui.components.EmptyStateView
import com.noto.app.ui.components.NoteCard
import com.noto.app.ui.components.NotoBottomBar
import com.noto.app.ui.components.NotoSearchBar
import com.noto.app.ui.navigation.BottomTab
import kotlinx.coroutines.launch

/**
 * Main screen displaying user's notes with search, tabs, and the floating bottom navigation bar.
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                onTabSelected = { tab -> viewModel.selectTab(tab) },
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
            // App Header
            HeaderSection(
                tab = currentTab,
                isSearchBarVisible = isSearchBarVisible,
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

            Spacer(modifier = Modifier.height(6.dp))

            // Content List or Empty State
            if (notes.isEmpty()) {
                EmptyStateView(
                    tab = currentTab,
                    isSearching = searchQuery.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = innerPadding.calculateBottomPadding() + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notes,
                        key = { it.id }
                    ) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    tab: BottomTab,
    isSearchBarVisible: Boolean,
    onSearchToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title = when (tab) {
            BottomTab.FAVORITES -> "Favorites"
            else -> "Noto"
        }

        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

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
