package com.noto.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateFormatterTest {

    @Test
    fun formatShort_returnsNonEmptyString_forValidTimestamp() {
        val now = System.currentTimeMillis()
        val result = DateFormatter.formatShort(now)
        assertFalse(result.isEmpty())
    }

    @Test
    fun formatShort_returnsYesterday_forYesterdayTimestamp() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val result = DateFormatter.formatShort(cal.timeInMillis)
        assertTrue(result == "Yesterday")
    }

    @Test
    fun formatEditor_returnsInformativeString() {
        val now = System.currentTimeMillis()
        val result = DateFormatter.formatEditor(now)
        assertTrue(result.startsWith("Edited today at"))
    }
}
