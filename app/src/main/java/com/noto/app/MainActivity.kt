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
import com.noto.app.ui.navigation.NotoNavigation
import com.noto.app.ui.theme.NotoTheme

/**
 * Single-activity entry point for Noto.
 * Configures edge-to-edge system bars and renders the Compose root.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notoApplication = application as NotoApplication

        setContent {
            val themeMode by notoApplication.themeRepository.themeMode.collectAsState()

            NotoTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotoNavigation(application = notoApplication)
                }
            }
        }
    }
}
