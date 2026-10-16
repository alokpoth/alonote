package com.noto.app.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartListContinuationTest {

    @Test
    fun checklistItem_continuesOnEnter() {
        val old = TextFieldValue("- [ ] Buy milk", TextRange(14))
        val new = TextFieldValue("- [ ] Buy milk\n", TextRange(15))

        val result = SmartListContinuation.processChange(new, old)
        assertEquals("- [ ] Buy milk\n- [ ] ", result.text)
        assertEquals(21, result.selection.start)
    }

    @Test
    fun completedChecklistItem_continuesAsPendingOnEnter() {
        val old = TextFieldValue("- [x] Done task", TextRange(15))
        val new = TextFieldValue("- [x] Done task\n", TextRange(16))

        val result = SmartListContinuation.processChange(new, old)
        assertEquals("- [x] Done task\n- [ ] ", result.text)
        assertEquals(22, result.selection.start)
    }

    @Test
    fun emptyChecklistItem_clearsOnEnter() {
        val old = TextFieldValue("- [ ] First item\n- [ ] ", TextRange(23))
        val new = TextFieldValue("- [ ] First item\n- [ ] \n", TextRange(24))

        val result = SmartListContinuation.processChange(new, old)
        assertEquals("- [ ] First item\n", result.text)
        assertEquals(17, result.selection.start)
    }

    @Test
    fun bulletItem_continuesOnEnter() {
        val old = TextFieldValue("- Notes", TextRange(7))
        val new = TextFieldValue("- Notes\n", TextRange(8))

        val result = SmartListContinuation.processChange(new, old)
        assertEquals("- Notes\n- ", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun regularText_doesNotAlterOnEnter() {
        val old = TextFieldValue("Just plain text", TextRange(15))
        val new = TextFieldValue("Just plain text\n", TextRange(16))

        val result = SmartListContinuation.processChange(new, old)
        assertEquals("Just plain text\n", result.text)
        assertEquals(16, result.selection.start)
    }
}
