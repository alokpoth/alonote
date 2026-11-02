package com.noto.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility for formatting note modification dates in a minimalist, Apple-inspired style.
 */
object DateFormatter {

    /**
     * Formats timestamp into a concise relative or absolute string for note list cards.
     * Examples:
     * - "3:45 PM" (if today)
     * - "Yesterday" (if yesterday)
     * - "Monday" (if within past 6 days)
     * - "Oct 15" (if earlier this year)
     * - "10/15/25" (if previous years)
     */
    fun formatShort(timestamp: Long): String {
        if (timestamp <= 0L) return ""

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetMidnight = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayDiff = ((todayMidnight.timeInMillis - targetMidnight.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)

        return when {
            dayDiff == 0 -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            dayDiff == 1 -> {
                "Yesterday"
            }
            dayDiff in 2..6 -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            }
            isSameYear -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("M/d/yy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Formats timestamp into an informative string for the note editor.
     * Example: "Edited Oct 15, 2026, 3:45 PM" or "Edited Today at 3:45 PM"
     */
    fun formatEditor(timestamp: Long): String {
        if (timestamp <= 0L) return ""

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetMidnight = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayDiff = ((todayMidnight.timeInMillis - targetMidnight.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

        return when {
            dayDiff == 0 -> "Edited today at $timeFormat"
            dayDiff == 1 -> "Edited yesterday at $timeFormat"
            isSameYear -> {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
                "Edited $dateFormat at $timeFormat"
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
                "Edited $dateFormat"
            }
        }
    }

    /**
     * Formats timestamp into a full date and time string for note detail sheets.
     * Example: "October 16, 2026 at 7:30 PM"
     */
    fun formatFull(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        return SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
