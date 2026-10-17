package com.noto.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.noto.app.MainActivity
import com.noto.app.R

/**
 * Android Home Screen Widget for Noto.
 * Provides instant 1-tap shortcuts for:
 * - 📝 New Note
 * - 📷 Photo Note
 * - 🎙️ Voice Memo
 * - 🔍 Search
 */
class NotoQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_noto_quick_actions)

        // General open intent
        val rootIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val rootPendingIntent = PendingIntent.getActivity(
            context,
            0,
            rootIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)

        // New Note
        val newNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, ACTION_NEW_NOTE)
        }
        val newNotePending = PendingIntent.getActivity(
            context,
            1,
            newNoteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_new_note, newNotePending)

        // Photo Note
        val photoIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, ACTION_PHOTO_NOTE)
        }
        val photoPending = PendingIntent.getActivity(
            context,
            2,
            photoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_photo, photoPending)

        // Voice Note
        val voiceIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, ACTION_VOICE_NOTE)
        }
        val voicePending = PendingIntent.getActivity(
            context,
            3,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_voice, voicePending)

        // Search
        val searchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, ACTION_SEARCH)
        }
        val searchPending = PendingIntent.getActivity(
            context,
            4,
            searchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_search, searchPending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "extra_widget_action"
        const val ACTION_NEW_NOTE = "action_new_note"
        const val ACTION_PHOTO_NOTE = "action_photo_note"
        const val ACTION_VOICE_NOTE = "action_voice_note"
        const val ACTION_SEARCH = "action_search"
    }
}
