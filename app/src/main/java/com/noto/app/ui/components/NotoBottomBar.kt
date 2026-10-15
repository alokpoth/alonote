package com.noto.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.noto.app.ui.navigation.BottomTab

/**
 * Floating, bottom-aligned navigation bar with rounded corners and a subtle translucent glass effect.
 * Contains exactly three primary buttons:
 * 1. Favorites (star icon)
 * 2. New Note (plus/pencil icon)
 * 3. Notes (document icon, selected by default)
 */
@Composable
fun NotoBottomBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(32.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    val borderColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = barShape,
                    spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                    ambientColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                )
                .clip(barShape)
                .border(width = 1.dp, color = borderColor, shape = barShape),
            color = surfaceColor,
            shape = barShape
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Favorites Button
                BottomBarItem(
                    selected = currentTab == BottomTab.FAVORITES,
                    icon = if (currentTab == BottomTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorites",
                    onClick = { onTabSelected(BottomTab.FAVORITES) }
                )

                // 2. New Note Button (Centered action)
                NewNoteActionItem(
                    onClick = onNewNoteClick
                )

                // 3. Notes Button (Default)
                BottomBarItem(
                    selected = currentTab == BottomTab.NOTES,
                    icon = if (currentTab == BottomTab.NOTES) Icons.Filled.Description else Icons.Outlined.Description,
                    contentDescription = "All Notes",
                    onClick = { onTabSelected(BottomTab.NOTES) }
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val itemShape = CircleShape

    val indicatorBg = if (selected) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    val iconTint = animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "BottomBarIconTint"
    )

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(itemShape)
            .background(indicatorBg, shape = itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 23.dp),
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Tab
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint.value,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun NewNoteActionItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = CircleShape

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onBackground, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 23.dp, color = MaterialTheme.colorScheme.background),
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = "Create New Note"
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(24.dp)
        )
    }
}
