package com.noto.app.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UndoRedoHistoryTest {

    private lateinit var history: UndoRedoHistory

    @Before
    fun setUp() {
        history = UndoRedoHistory(maxCapacity = 10)
    }

    @Test
    fun initialState_cannotUndoOrRedo() {
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertNull(history.undo(TextFieldValue("test")))
        assertNull(history.redo(TextFieldValue("test")))
    }

    @Test
    fun recordAndUndo_restoresPreviousState() {
        val initial = TextFieldValue("Hello", TextRange(5))
        history.record(initial, force = true)

        val updated = TextFieldValue("Hello World", TextRange(11))
        history.record(updated, force = true)

        assertTrue(history.canUndo)
        val reverted = history.undo(updated)
        assertNotNull(reverted)
        assertEquals("Hello", reverted?.text)
        assertTrue(history.canRedo)

        val redone = history.redo(reverted!!)
        assertNotNull(redone)
        assertEquals("Hello World", redone?.text)
    }

    @Test
    fun clear_resetsStacks() {
        history.record(TextFieldValue("A"), force = true)
        history.record(TextFieldValue("B"), force = true)
        assertTrue(history.canUndo)

        history.clear()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }
}
