package com.noto.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manages scheduling and cancelling local, offline reminders using Android AlarmManager.
 */
object ReminderManager {
    private const val TAG = "ReminderManager"

    fun scheduleReminder(
        context: Context,
        noteId: Long,
        title: String,
        body: String,
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_NOTE_BODY, body)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied, falling back to standard alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    fun cancelReminder(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * User-friendly format for displaying reminder time.
     * e.g., "Today at 4:30 PM", "Tomorrow at 9:00 AM", or "Oct 24 at 10:00 AM".
     */
    fun formatReminderTime(millis: Long): String {
        if (millis <= 0L) return ""

        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = millis }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeString = timeFormat.format(Date(millis))

        val isSameDay = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = tomorrowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                tomorrowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> "Today at $timeString"
            isTomorrow -> "Tomorrow at $timeString"
            else -> {
                val dateFormat = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
                dateFormat.format(Date(millis))
            }
        }
    }
}
