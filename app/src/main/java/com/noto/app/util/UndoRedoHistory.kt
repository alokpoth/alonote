package com.noto.app.util

import androidx.compose.ui.text.input.TextFieldValue

/**
 * Lightweight, high-performance undo and redo history manager for Compose TextFieldValue.
 * Supports discrete action recording (e.g., toolbar formatting) and debounced typing snapshots.
 */
class UndoRedoHistory(
    private val maxCapacity: Int = 40
) {
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lastRecordedTime = 0L

    val canUndo: Boolean get() = undoStack.size > 1
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Record a snapshot of the text state.
     * @param current The current TextFieldValue.
     * @param force Set to true for explicit user actions (Bold, Italic, List, Enter) to capture immediately.
     */
    fun record(current: TextFieldValue, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (undoStack.isNotEmpty() && undoStack.last().text == current.text) {
            return
        }

        if (force || now - lastRecordedTime > 800L || undoStack.isEmpty()) {
            undoStack.add(current)
            if (undoStack.size > maxCapacity) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
            lastRecordedTime = now
        }
    }

    /**
     * Reverts to the previous snapshot.
     */
    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        if (undoStack.last().text != current.text) {
            undoStack.add(current)
            if (undoStack.size > maxCapacity) {
                undoStack.removeAt(0)
            }
        }
        if (undoStack.size <= 1) return null
        val top = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(top)
        lastRecordedTime = System.currentTimeMillis()
        return undoStack.last()
    }

    /**
     * Re-applies the undone snapshot.
     */
    fun redo(current: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(next)
        lastRecordedTime = System.currentTimeMillis()
        return next
    }

    /**
     * Clears undo and redo history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastRecordedTime = 0L
    }
}
