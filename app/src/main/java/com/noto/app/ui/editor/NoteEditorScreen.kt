package com.noto.app.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noto.app.ui.components.DeleteConfirmationDialog
import com.noto.app.ui.components.MarkdownToolbar
import com.noto.app.util.DateFormatter
import com.noto.app.util.MarkdownFormatter

/**
 * Clean, distraction-free note editor screen with zero typing lag,
 * visible UI action buttons for Pin, Star, Copy, Delete, Done,
 * live word/character count pill, and an Apple-inspired Markdown formatting toolbar
 * (Bold, Italic, Heading, Bullet List, To-do Checklist, Strikethrough).
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
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Maintain local TextFieldValue state for smooth selection and formatting
    var bodyTextFieldValue by remember(uiState.isLoaded) {
        mutableStateOf(TextFieldValue(uiState.body, selection = TextRange(uiState.body.length)))
    }

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

    // Automatically focus when opening a new draft
    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded && uiState.isNewNote) {
            titleFocusRequester.requestFocus()
        }
    }

    // Calculate word and character count
    val combinedText = remember(uiState.title, bodyTextFieldValue.text) {
        "${uiState.title} ${bodyTextFieldValue.text}".trim()
    }
    val wordCount = remember(combinedText) {
        if (combinedText.isEmpty()) 0 else combinedText.split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val charCount = remember(uiState.title, bodyTextFieldValue.text) {
        uiState.title.length + bodyTextFieldValue.text.length
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EditorTopBar(
                isFavorite = uiState.isFavorite,
                isPinned = uiState.isPinned,
                saveStatus = uiState.saveStatus,
                onBack = {
                    viewModel.flushSave()
                    onNavigateBack()
                },
                onDone = {
                    viewModel.flushSave()
                    onNavigateBack()
                },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onTogglePin = { viewModel.togglePin() },
                onCopy = {
                    val contentToCopy = buildString {
                        if (uiState.title.isNotBlank()) appendLine(uiState.title)
                        if (bodyTextFieldValue.text.isNotBlank()) append(bodyTextFieldValue.text)
                    }.trim()
                    clipboardManager.setText(AnnotatedString(contentToCopy))
                    Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                onDelete = { showDeleteDialog = true }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Docked Formatting Toolbar (Bold, Italic, Heading, Bullet, Checklist, Strike)
                MarkdownToolbar(
                    value = bodyTextFieldValue,
                    onValueChange = {
                        bodyTextFieldValue = it
                        viewModel.onBodyChange(it.text)
                    }
                )

                // Minimal Live Word & Character Count Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$wordCount words • $charCount characters",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.lastUpdated > 0L) {
                        Text(
                            text = DateFormatter.formatEditor(uiState.lastUpdated),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    bodyFocusRequester.requestFocus()
                }
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Editable Title Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        titleFocusRequester.requestFocus()
                    },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { bodyFocusRequester.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Multiline Note Body Field - tap anywhere on this surface to write
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 450.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        bodyFocusRequester.requestFocus()
                    },
                contentAlignment = Alignment.TopStart
            ) {
                if (bodyTextFieldValue.text.isEmpty()) {
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
                    value = bodyTextFieldValue,
                    onValueChange = { newValue ->
                        // Automatically continue checklist [ ] or bullet • on Enter key press
                        if (newValue.text.length == bodyTextFieldValue.text.length + 1 &&
                            newValue.selection.start > 0 &&
                            newValue.text.getOrNull(newValue.selection.start - 1) == '\n') {
                            val handled = MarkdownFormatter.handleEnterKey(bodyTextFieldValue)
                            if (handled != null) {
                                bodyTextFieldValue = handled
                                viewModel.onBodyChange(handled.text)
                                return@BasicTextField
                            }
                        }
                        bodyTextFieldValue = newValue
                        viewModel.onBodyChange(newValue.text)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 450.dp)
                        .focusRequester(bodyFocusRequester)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
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
    isPinned: Boolean,
    saveStatus: SaveStatus,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
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
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        // 1. Visible Pin Button
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (isPinned) "Unpin note" else "Pin note",
                tint = if (isPinned) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Visible Star / Favorite Toggle Button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(38.dp)
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
                modifier = Modifier.size(20.dp)
            )
        }

        // 3. Visible Copy Note Button
        IconButton(
            onClick = onCopy,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy Note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }

        // 4. Visible Delete Button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete Note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // 5. Visible Done / Save Checkmark Button
        IconButton(
            onClick = onDone,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Done",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
