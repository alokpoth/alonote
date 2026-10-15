package com.noto.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noto.app.ui.components.DeleteConfirmationDialog
import com.noto.app.util.DateFormatter

/**
 * Clean, distraction-free note editor screen.
 */
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }

    // Intercept back navigation to flush pending changes
    BackHandler {
        viewModel.flushSave()
        onNavigateBack()
    }

    // Flush save when screen is stopped, disposed, or app is backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.flushSave()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.flushSave()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Naturally focus and open keyboard for new notes
    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded && uiState.isNewNote) {
            titleFocusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EditorTopBar(
                isFavorite = uiState.isFavorite,
                saveStatus = uiState.saveStatus,
                onBack = {
                    viewModel.flushSave()
                    onNavigateBack()
                },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onDelete = { showDeleteDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Last edited timestamp indicator
            if (uiState.lastUpdated > 0L) {
                Text(
                    text = DateFormatter.formatEditor(uiState.lastUpdated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Editable Title Field
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (uiState.title.isEmpty()) {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                BasicTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multiline Note Body Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.TopStart
            ) {
                if (uiState.body.isEmpty()) {
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            lineHeight = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                BasicTextField(
                    value = uiState.body,
                    onValueChange = { viewModel.onBodyChange(it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(bodyFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteNote {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun EditorTopBar(
    isFavorite: Boolean,
    saveStatus: SaveStatus,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Status Indicator
        AnimatedVisibility(
            visible = saveStatus == SaveStatus.SAVING || saveStatus == SaveStatus.SAVED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = if (saveStatus == SaveStatus.SAVING) "Saving..." else "Saved",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        // Star / Favorite Toggle Button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (isFavorite) "Starred" else "Star Note",
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp)
            )
        }

        // Delete Button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete Note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
