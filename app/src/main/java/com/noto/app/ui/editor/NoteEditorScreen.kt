package com.noto.app.ui.editor

import android.content.Intent
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.material.icons.filled.Close
import com.noto.app.data.local.entity.Note
import com.noto.app.ui.components.DeleteConfirmationDialog
import com.noto.app.ui.components.MarkdownReaderView
import com.noto.app.ui.components.MarkdownToolbar
import com.noto.app.ui.components.NoteAudioPlayerItem
import com.noto.app.ui.components.NoteImageThumbnail
import com.noto.app.ui.components.NoteStatsBottomSheet
import com.noto.app.ui.components.PhotoViewerDialog
import com.noto.app.util.AudioStorageManager
import com.noto.app.util.DateFormatter
import com.noto.app.util.ImageStorageManager
import com.noto.app.util.MarkdownFormatter
import com.noto.app.util.NoteAudioPlayer
import com.noto.app.util.NoteAudioRecorder
import com.noto.app.util.PdfExporter
import com.noto.app.util.ReminderManager
import com.noto.app.util.SmartListContinuation
import com.noto.app.util.UndoRedoHistory
import com.noto.app.util.rememberMarkdownVisualTransformation
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean, distraction-free note editor screen with live visual Markdown rendering,
 * zero typing lag, visible UI action buttons (Pin, Star, Share, Copy, Delete, Done),
 * word/character count pill, and an Apple-inspired formatting toolbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isReaderMode by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val undoHistory = remember { UndoRedoHistory() }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val savedFilenames = uris.mapNotNull { uri ->
                    ImageStorageManager.saveImageFromUri(context, uri)
                }
                if (savedFilenames.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        viewModel.attachImages(savedFilenames)
                    }
                }
            }
        }
    }

    // Maintain local TextFieldValue state for smooth selection and formatting
    var bodyTextFieldValue by remember(uiState.isLoaded) {
        val initial = TextFieldValue(uiState.body, selection = TextRange(uiState.body.length))
        undoHistory.record(initial, force = true)
        mutableStateOf(initial)
    }

    val audioRecorder = remember { NoteAudioRecorder(context) }
    val audioPlayer = remember { NoteAudioPlayer(context) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableStateOf(0) }
    var showReminderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingDurationSeconds = 0
            while (isRecordingVoice) {
                kotlinx.coroutines.delay(1000L)
                recordingDurationSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.stopRecording()
            audioPlayer.release()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = audioRecorder.startRecording()
            if (started != null) {
                isRecordingVoice = true
                recordingDurationSeconds = 0
                Toast.makeText(context, "Recording voice memo...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to start audio recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice memos", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showReminderDialog = true
        } else {
            Toast.makeText(context, "Notification permission is required for reminders", Toast.LENGTH_SHORT).show()
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val currentNote = Note(
                            id = uiState.noteId,
                            title = uiState.title,
                            body = bodyTextFieldValue.text,
                            isFavorite = uiState.isFavorite,
                            isPinned = uiState.isPinned,
                            isDeleted = false,
                            deletedAt = null,
                            imagePaths = uiState.imagePaths,
                            audioPaths = uiState.audioPaths,
                            reminderTime = uiState.reminderTime,
                            createdAt = if (uiState.createdAt == 0L) System.currentTimeMillis() else uiState.createdAt,
                            updatedAt = System.currentTimeMillis()
                        )
                        PdfExporter.exportNoteToPdf(currentNote, outputStream)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Note exported to PDF successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val mdExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val content = buildString {
                            if (uiState.title.isNotBlank()) {
                                appendLine("# ${uiState.title}")
                                appendLine()
                            }
                            if (bodyTextFieldValue.text.isNotBlank()) {
                                append(bodyTextFieldValue.text)
                            }
                        }.trim()
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Note exported to Markdown (.md) successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to export Markdown: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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
        if (combinedText.isEmpty()) 0 else combinedText.split(WordSplitRegex).count { it.isNotBlank() }
    }
    val charCount = remember(uiState.title, bodyTextFieldValue.text) {
        uiState.title.length + bodyTextFieldValue.text.length
    }
    val readingTime = maxOf(1, kotlin.math.ceil(wordCount / 200.0).toInt())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EditorTopBar(
                isFavorite = uiState.isFavorite,
                isPinned = uiState.isPinned,
                isReaderMode = isReaderMode,
                saveStatus = uiState.saveStatus,
                onBack = {
                    viewModel.flushSave()
                    onNavigateBack()
                },
                onDone = {
                    viewModel.flushSave()
                    onNavigateBack()
                },
                onToggleReaderMode = {
                    if (!isReaderMode) {
                        keyboardController?.hide()
                    }
                    isReaderMode = !isReaderMode
                },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onTogglePin = { viewModel.togglePin() },
                onShare = {
                    val shareContent = buildString {
                        if (uiState.title.isNotBlank()) appendLine(uiState.title)
                        if (bodyTextFieldValue.text.isNotBlank()) append(bodyTextFieldValue.text)
                    }.trim()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareContent)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share note via"))
                },
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
            if (isReaderMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showStatsSheet = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📖 Reader Mode • $wordCount words • ~${readingTime}m read",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { isReaderMode = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Tap ✏️ to edit",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    // Docked Formatting Toolbar (Undo, Redo, Bold, Italic, Heading, Bullet, Checklist, Strike, Code, Tag)
                    MarkdownToolbar(
                        value = bodyTextFieldValue,
                        onValueChange = { newValue ->
                            undoHistory.record(bodyTextFieldValue, force = true)
                            bodyTextFieldValue = newValue
                            viewModel.onBodyChange(newValue.text)
                            undoHistory.record(newValue, force = true)
                        },
                        canUndo = undoHistory.canUndo,
                        canRedo = undoHistory.canRedo,
                        onUndo = {
                            undoHistory.undo(bodyTextFieldValue)?.let { reverted ->
                                bodyTextFieldValue = reverted
                                viewModel.onBodyChange(reverted.text)
                            }
                        },
                        onRedo = {
                            undoHistory.redo(bodyTextFieldValue)?.let { redone ->
                                bodyTextFieldValue = redone
                                viewModel.onBodyChange(redone.text)
                            }
                        },
                        onAddPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRecordVoice = {
                            if (isRecordingVoice) {
                                val recordedFile = audioRecorder.stopRecording()
                                isRecordingVoice = false
                                if (recordedFile != null) {
                                    viewModel.attachAudio(recordedFile)
                                    Toast.makeText(context, "Voice memo attached", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        isRecordingVoice = isRecordingVoice,
                        recordingDurationSeconds = recordingDurationSeconds,
                        onSetReminder = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                showReminderDialog = true
                            }
                        },
                        onExportPdf = {
                            val sanitizedTitle = (uiState.title.ifBlank { "AloNote_Export" })
                                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            pdfExportLauncher.launch("$sanitizedTitle.pdf")
                        },
                        onExportMarkdown = {
                            val sanitizedTitle = (uiState.title.ifBlank { "AloNote_Export" })
                                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            mdExportLauncher.launch("$sanitizedTitle.md")
                        }
                    )

                    // Minimal Live Word & Character Count Bar (Clickable to open Note Details modal)
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
                                .clickable { showStatsSheet = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$wordCount words • $charCount chars • ~${readingTime}m read",
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
        }
    ) { innerPadding ->
        if (isReaderMode) {
            MarkdownReaderView(
                title = uiState.title,
                body = bodyTextFieldValue.text,
                images = uiState.images,
                onImageClick = { previewImage = it },
                audios = uiState.audios,
                audioPlayer = audioPlayer,
                onToggleTaskAtLine = { lineIdx ->
                    val lines = bodyTextFieldValue.text.lines().toMutableList()
                    if (lineIdx in lines.indices) {
                        val line = lines[lineIdx]
                        val toggled = when {
                            line.contains("- [ ] ") -> line.replaceFirst("- [ ] ", "- [x] ")
                            line.contains("[ ] ") -> line.replaceFirst("[ ] ", "[x] ")
                            line.contains("- [x] ") -> line.replaceFirst("- [x] ", "- [ ] ")
                            line.contains("[x] ") -> line.replaceFirst("[x] ", "[ ] ")
                            else -> line
                        }
                        lines[lineIdx] = toggled
                        val newText = lines.joinToString("\n")
                        val newSel = TextRange(bodyTextFieldValue.selection.start.coerceIn(0, newText.length))
                        val updated = TextFieldValue(newText, newSel)
                        undoHistory.record(bodyTextFieldValue, force = true)
                        bodyTextFieldValue = updated
                        viewModel.onBodyChange(newText)
                        undoHistory.record(updated, force = true)
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
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
                // Attached Photos Horizontal Strip
                if (uiState.images.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.images.forEach { filename ->
                            NoteImageThumbnail(
                                filename = filename,
                                modifier = Modifier.size(width = 130.dp, height = 100.dp),
                                showRemoveButton = true,
                                onClick = { previewImage = filename },
                                onRemove = { viewModel.removeImage(filename, context) }
                            )
                        }

                        // Quick "+ Add" card
                        Box(
                            modifier = Modifier
                                .size(width = 90.dp, height = 100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Add photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Active Reminder Banner
                if (uiState.hasReminder && uiState.reminderTime != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { showReminderDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏰ Reminder: ${ReminderManager.formatReminderTime(uiState.reminderTime!!)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                viewModel.setReminder(null, context)
                                Toast.makeText(context, "Reminder cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear reminder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Attached Voice Memos Player List
                if (uiState.audios.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.audios.forEach { audioFilename ->
                            NoteAudioPlayerItem(
                                filename = audioFilename,
                                audioPlayer = audioPlayer,
                                showRemoveButton = true,
                                onRemove = {
                                    audioPlayer.stop()
                                    viewModel.removeAudio(audioFilename, context)
                                }
                            )
                        }
                    }
                }

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

                // Multiline Note Body Field with Live Markdown Transformation & Smart List Continuation
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
                            val processed = SmartListContinuation.processChange(newValue, bodyTextFieldValue)
                            undoHistory.record(bodyTextFieldValue, force = processed != newValue)
                            bodyTextFieldValue = processed
                            viewModel.onBodyChange(processed.text)
                        },
                        visualTransformation = rememberMarkdownVisualTransformation(),
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
    }

    if (showStatsSheet) {
        NoteStatsBottomSheet(
            title = uiState.title,
            body = bodyTextFieldValue.text,
            createdAt = uiState.createdAt,
            updatedAt = uiState.lastUpdated,
            onDismiss = { showStatsSheet = false }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Move to Recycle Bin?",
            message = "You can restore this note anytime from the Recycle Bin.",
            confirmText = "Move to Bin",
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteNote {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (previewImage != null) {
        PhotoViewerDialog(
            filename = previewImage!!,
            onDismiss = { previewImage = null }
        )
    }

    if (showReminderDialog) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val in1Hour = now + (60 * 60 * 1000L)

        val calEvening = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val calMorning = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val calTomorrowEvening = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = {
                Text(
                    text = "Set Reminder",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderOptionRow(
                        label = "In 1 Hour",
                        timeDesc = ReminderManager.formatReminderTime(in1Hour),
                        onClick = {
                            showReminderDialog = false
                            viewModel.setReminder(in1Hour, context)
                            Toast.makeText(context, "Reminder set for 1 hour later", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ReminderOptionRow(
                        label = if (calEvening.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) "This Evening" else "Tomorrow Evening",
                        timeDesc = ReminderManager.formatReminderTime(calEvening.timeInMillis),
                        onClick = {
                            showReminderDialog = false
                            viewModel.setReminder(calEvening.timeInMillis, context)
                            Toast.makeText(context, "Reminder set for ${ReminderManager.formatReminderTime(calEvening.timeInMillis)}", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ReminderOptionRow(
                        label = "Tomorrow Morning",
                        timeDesc = ReminderManager.formatReminderTime(calMorning.timeInMillis),
                        onClick = {
                            showReminderDialog = false
                            viewModel.setReminder(calMorning.timeInMillis, context)
                            Toast.makeText(context, "Reminder set for tomorrow morning", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ReminderOptionRow(
                        label = "Tomorrow Evening",
                        timeDesc = ReminderManager.formatReminderTime(calTomorrowEvening.timeInMillis),
                        onClick = {
                            showReminderDialog = false
                            viewModel.setReminder(calTomorrowEvening.timeInMillis, context)
                            Toast.makeText(context, "Reminder set for tomorrow evening", Toast.LENGTH_SHORT).show()
                        }
                    )
                    if (uiState.hasReminder) {
                        Spacer(modifier = Modifier.height(4.dp))
                        ReminderOptionRow(
                            label = "Clear Reminder",
                            timeDesc = "Remove active notification",
                            isDestructive = true,
                            onClick = {
                                showReminderDialog = false
                                viewModel.setReminder(null, context)
                                Toast.makeText(context, "Reminder cleared", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ReminderOptionRow(
    label: String,
    timeDesc: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = timeDesc,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditorTopBar(
    isFavorite: Boolean,
    isPinned: Boolean,
    isReaderMode: Boolean,
    saveStatus: SaveStatus,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onToggleReaderMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
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

        // 1. Pin Button
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

        // 2. Star / Favorite Toggle Button
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

        // 3. Share Note Button (Native Android Intent)
        IconButton(
            onClick = onShare,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Share Note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }

        // 4. Copy Note Button
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

        // 5. Delete Button (Moves to Recycle Bin)
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Move to Recycle Bin",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // 6. Reader / Focus Mode Toggle Button
        IconButton(
            onClick = onToggleReaderMode,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isReaderMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
        ) {
            Icon(
                imageVector = if (isReaderMode) Icons.Filled.Edit else Icons.Filled.AutoStories,
                contentDescription = if (isReaderMode) "Switch to Editor" else "Reader Mode",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }

        // 7. Done / Save Checkmark Button
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

private val WordSplitRegex = Regex("""\s+""")

