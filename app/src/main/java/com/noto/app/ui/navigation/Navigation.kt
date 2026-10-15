package com.noto.app.ui.navigation

import androidx.compose.runtime.Composable
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

/**
 * Top-level application navigation using Navigation 3.
 */
@Composable
fun NotoNavigation(
    application: NotoApplication,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(MainNavKey())
    val repository = application.noteRepository

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryProvider = entryProvider {
            entry<MainNavKey> {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(repository)
                )
                MainScreen(
                    viewModel = mainViewModel,
                    onNoteClick = { noteId ->
                        backStack.add(NoteEditorNavKey(noteId = noteId))
                    },
                    onNewNoteClick = {
                        backStack.add(NoteEditorNavKey(noteId = 0L, draftKey = System.currentTimeMillis()))
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
        }
    )
}
