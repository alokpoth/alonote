package com.noto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import android.content.Intent
import com.noto.app.ui.navigation.NotoNavigation
import com.noto.app.ui.theme.NotoTheme
import com.noto.app.util.ReminderBroadcastReceiver
import com.noto.app.widget.NotoQuickWidgetProvider

/**
 * Single-activity entry point for Noto.
 * Configures edge-to-edge system bars and renders the Compose root.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notoApplication = application as NotoApplication

        val initialNoteId = intent?.getLongExtra(ReminderBroadcastReceiver.EXTRA_NOTE_ID, 0L) ?: 0L
        val widgetAction = intent?.getStringExtra(NotoQuickWidgetProvider.EXTRA_WIDGET_ACTION)
        val initialNewNote = widgetAction in listOf(
            NotoQuickWidgetProvider.ACTION_NEW_NOTE,
            NotoQuickWidgetProvider.ACTION_PHOTO_NOTE,
            NotoQuickWidgetProvider.ACTION_VOICE_NOTE
        )

        setContent {
            val themeMode by notoApplication.themeRepository.themeMode.collectAsState()
            val fontFamily by notoApplication.themeRepository.fontFamily.collectAsState()
            val fontSizeScale by notoApplication.themeRepository.fontSizeScale.collectAsState()

            NotoTheme(
                themeMode = themeMode,
                fontFamily = fontFamily,
                fontSizeScale = fontSizeScale
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotoNavigation(
                        application = notoApplication,
                        initialNoteId = initialNoteId,
                        initialNewNote = initialNewNote
                    )
                }
            }
        }
    }
}
