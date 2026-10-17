package com.noto.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.noto.app.MainActivity

/**
 * 100% Offline BroadcastReceiver for Note Reminders.
 * Fired by Android AlarmManager when a scheduled reminder is due.
 * Creates an offline local notification without any internet or cloud connection.
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        val title = intent.getStringExtra(EXTRA_NOTE_TITLE) ?: "Note Reminder"
        val body = intent.getStringExtra(EXTRA_NOTE_BODY) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        createNotificationChannel(context, notificationManager)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTE_ID, noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (title.isNotBlank()) title else "Noto Reminder")
            .setContentText(if (body.isNotBlank()) body else "Tap to open your note")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(noteId.toInt(), notification)
    }

    private fun createNotificationChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled note reminders in Noto"
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "noto_reminders_channel"
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_BODY = "extra_note_body"
    }
}
