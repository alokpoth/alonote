package com.noto.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Bottom navigation tabs.
 */
@Serializable
enum class BottomTab {
    FAVORITES,
    NEW_NOTE,
    NOTES
}

/**
 * Navigation key for the main notes screen.
 */
@Serializable
data class MainNavKey(val initialTab: BottomTab = BottomTab.NOTES) : NavKey

/**
 * Navigation key for the note editor screen.
 * @param noteId The ID of an existing note to edit, or 0L to create a new draft.
 */
@Serializable
data class NoteEditorNavKey(val noteId: Long = 0L) : NavKey
