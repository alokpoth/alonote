package com.noto.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.R
import kotlinx.coroutines.delay

/**
 * Facebook/Meta-style Alokpath startup splash screen.
 * Displays the app's minimalist icon at the center, and "from ALOKPATH (আলোকপথ)"
 * with the official company logo at the bottom.
 * Transitions to the main application after a smooth ~1.3s delay.
 */
@Composable
fun AlokpathSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        // Facebook/Meta standard splash duration
        delay(1300L)
        onSplashFinished()
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Center: App Icon and Title
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.noto_app_icon),
                contentDescription = "Noto App Icon",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(22.dp)
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Noto",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Offline Notes",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Bottom: "from" + Alokpath official company logo (Facebook/Meta Style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "from",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(
                    id = if (isDark) R.drawable.alokpath_logo_dark else R.drawable.alokpath_logo_light
                ),
                contentDescription = "Alokpath - আলোকপথ",
                modifier = Modifier.height(28.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "আলোকপথ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
