package com.noto.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.noto.app.NotoApplication
import com.noto.app.ui.editor.NoteEditorScreen
import com.noto.app.ui.editor.NoteEditorViewModel
import com.noto.app.ui.main.MainScreen
import com.noto.app.ui.main.MainViewModel

import com.noto.app.ui.settings.SettingsScreen
import com.noto.app.ui.navigation.SettingsNavKey

/**
 * Top-level application navigation using Navigation 3.
 */
@Composable
fun NotoNavigation(
    application: NotoApplication,
    initialNoteId: Long = 0L,
    initialNewNote: Boolean = false,
    targetNoteId: Long = 0L,
    triggerNewNote: Boolean = false,
    triggerSearch: Boolean = false,
    onIntentHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(
        MainNavKey(),
        *if (initialNoteId > 0L) {
            arrayOf(NoteEditorNavKey(noteId = initialNoteId))
        } else if (initialNewNote) {
            arrayOf(NoteEditorNavKey(noteId = 0L, draftKey = System.currentTimeMillis()))
        } else {
            emptyArray()
        }
    )
    val repository = application.noteRepository

    LaunchedEffect(targetNoteId, triggerNewNote) {
        if (targetNoteId > 0L) {
            backStack.add(NoteEditorNavKey(noteId = targetNoteId))
            onIntentHandled()
        } else if (triggerNewNote) {
            backStack.add(NoteEditorNavKey(noteId = 0L, draftKey = System.currentTimeMillis()))
            onIntentHandled()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryProvider = entryProvider {
            entry<MainNavKey> {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(repository, application.themeRepository)
                )
                LaunchedEffect(triggerSearch) {
                    if (triggerSearch) {
                        mainViewModel.openSearchBar()
                        onIntentHandled()
                    }
                }
                MainScreen(
                    viewModel = mainViewModel,
                    onNoteClick = { noteId ->
                        backStack.add(NoteEditorNavKey(noteId = noteId))
                    },
                    onNewNoteClick = {
                        backStack.add(NoteEditorNavKey(noteId = 0L, draftKey = System.currentTimeMillis()))
                    },
                    onSettingsClick = {
                        backStack.add(SettingsNavKey())
                    }
                )
            }
            entry<NoteEditorNavKey> { key ->
                val vmKey = if (key.noteId > 0L) "note_editor_${key.noteId}" else "note_editor_new_${key.draftKey}"
                val editorViewModel: NoteEditorViewModel = viewModel(
                    key = vmKey,
                    factory = NoteEditorViewModel.Factory(key.noteId, repository)
                )
                NoteEditorScreen(
                    viewModel = editorViewModel,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<SettingsNavKey> {
                SettingsScreen(
                    themeRepository = application.themeRepository,
                    noteRepository = repository,
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}
