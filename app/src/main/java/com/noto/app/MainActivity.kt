package com.noto.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.noto.app.ui.navigation.NotoNavigation
import com.noto.app.ui.splash.AlokpathSplashScreen
import com.noto.app.ui.theme.NotoTheme
import com.noto.app.util.ReminderBroadcastReceiver
import com.noto.app.widget.NotoQuickWidgetProvider

/**
 * Single-activity entry point for Noto.
 * Configures edge-to-edge system bars and renders the Compose root.
 */
class MainActivity : ComponentActivity() {

    private var targetNoteId by mutableStateOf(0L)
    private var triggerNewNote by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notoApplication = application as NotoApplication
        handleIntent(intent)

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
                    var showSplash by rememberSaveable { mutableStateOf(true) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Pre-composed behind splash to eliminate startup stutter
                        NotoNavigation(
                            application = notoApplication,
                            targetNoteId = targetNoteId,
                            triggerNewNote = triggerNewNote,
                            onIntentHandled = {
                                targetNoteId = 0L
                                triggerNewNote = false
                            }
                        )

                        AnimatedVisibility(
                            visible = showSplash,
                            enter = EnterTransition.None,
                            exit = fadeOut(
                                animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing)
                            )
                        ) {
                            AlokpathSplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val noteId = intent?.getLongExtra(ReminderBroadcastReceiver.EXTRA_NOTE_ID, 0L) ?: 0L
        val widgetAction = intent?.getStringExtra(NotoQuickWidgetProvider.EXTRA_WIDGET_ACTION)
        val isNewNote = widgetAction in listOf(
            NotoQuickWidgetProvider.ACTION_NEW_NOTE,
            NotoQuickWidgetProvider.ACTION_PHOTO_NOTE,
            NotoQuickWidgetProvider.ACTION_VOICE_NOTE
        )
        if (noteId > 0L) {
            targetNoteId = noteId
        }
        if (isNewNote) {
            triggerNewNote = true
        }
    }
}
