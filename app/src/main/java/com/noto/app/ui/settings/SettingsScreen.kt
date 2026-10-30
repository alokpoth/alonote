package com.noto.app.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noto.app.R
import com.noto.app.data.repository.AppFontFamily
import com.noto.app.data.repository.FontSizeScale
import com.noto.app.data.repository.NoteRepository
import com.noto.app.data.repository.ThemeMode
import com.noto.app.data.repository.ThemeRepository
import com.noto.app.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Minimalist Apple-inspired Settings Screen for Noto.
 * Includes Theme (Light, Dark, OLED Pitch Black, Eye Care Sepia),
 * Custom Typography & Scaling, and 1-Click JSON Backup & Restore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeRepository: ThemeRepository,
    noteRepository: NoteRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentTheme by themeRepository.themeMode.collectAsState()
    val currentFont by themeRepository.fontFamily.collectAsState()
    val currentScale by themeRepository.fontSizeScale.collectAsState()

    // Backup Export Launcher
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val notes = withContext(Dispatchers.IO) {
                        noteRepository.getAllNotesIncludingTrashSnapshot()
                    }
                    val json = BackupManager.exportToJson(notes)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(json.toByteArray())
                        }
                    }
                    Toast.makeText(context, "✅ Backup exported successfully (${notes.size} notes)", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to export backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Backup Restore Launcher
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().readText()
                        } ?: ""
                    }
                    val notes = BackupManager.parseFromJson(json)
                    if (notes.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            noteRepository.insertNotes(notes)
                        }
                        Toast.makeText(context, "✅ Restored ${notes.size} notes successfully", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "No notes found in backup file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to restore backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // SECTION: THEME / APPEARANCE
            SettingsSectionHeader(title = "Appearance & Eye Care", icon = Icons.Filled.ColorLens)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                ThemeOptionRow(
                    label = "System Default",
                    description = "Follows Android system light/dark theme",
                    selected = currentTheme == ThemeMode.SYSTEM,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        themeRepository.setThemeMode(ThemeMode.SYSTEM)
                    }
                )
                ThemeOptionRow(
                    label = "Pure Light",
                    description = "Clean Apple paper aesthetic",
                    selected = currentTheme == ThemeMode.LIGHT,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        themeRepository.setThemeMode(ThemeMode.LIGHT)
                    }
                )
                ThemeOptionRow(
                    label = "Charcoal Dark",
                    description = "Subtle dark mode for evening writing",
                    selected = currentTheme == ThemeMode.DARK,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        themeRepository.setThemeMode(ThemeMode.DARK)
                    }
                )
                ThemeOptionRow(
                    label = "🖤 OLED Pure Black",
                    description = "True #000000 - Maximum battery saving & deep contrast",
                    selected = currentTheme == ThemeMode.OLED,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        themeRepository.setThemeMode(ThemeMode.OLED)
                    }
                )
                ThemeOptionRow(
                    label = "📜 Eye Care Paper",
                    description = "Warm Sepia reading tone with 0% blue light eye strain",
                    selected = currentTheme == ThemeMode.EYE_CARE,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        themeRepository.setThemeMode(ThemeMode.EYE_CARE)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: TYPOGRAPHY
            SettingsSectionHeader(title = "Typography & Font", icon = Icons.Filled.TextFields)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Font Family",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontPill(
                        label = "Sans",
                        sample = "Clean",
                        selected = currentFont == AppFontFamily.SYSTEM_SANS,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeRepository.setFontFamily(AppFontFamily.SYSTEM_SANS)
                        }
                    )
                    FontPill(
                        label = "Serif",
                        sample = "Editorial",
                        selected = currentFont == AppFontFamily.SERIF,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeRepository.setFontFamily(AppFontFamily.SERIF)
                        }
                    )
                    FontPill(
                        label = "Mono",
                        sample = "Code",
                        selected = currentFont == AppFontFamily.MONOSPACE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeRepository.setFontFamily(AppFontFamily.MONOSPACE)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Text Scaling",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontPill(
                        label = "Small",
                        sample = "88%",
                        selected = currentScale == FontSizeScale.SMALL,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeRepository.setFontSizeScale(FontSizeScale.SMALL)
                        }
                    )
                    FontPill(
                        label = "Default",
                        sample = "100%",
                        selected = currentScale == FontSizeScale.DEFAULT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeRepository.setFontSizeScale(FontSizeScale.DEFAULT)
                        }
                    )
                    FontPill(
                        label = "Large",
                        sample = "115%",
                        selected = currentScale == FontSizeScale.LARGE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            themeRepository.setFontSizeScale(FontSizeScale.LARGE)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: BACKUP & DATA
            SettingsSectionHeader(title = "Backup & Portability", icon = Icons.Filled.Backup)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                // Export Backup Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val filename = "Noto_Backup_${System.currentTimeMillis()}.json"
                            exportBackupLauncher.launch(filename)
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Full Backup",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Saves all notes, checklists & references into a single .json file",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Restore Backup Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            restoreBackupLauncher.launch(arrayOf("application/json"))
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restore from Backup",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Select an existing .json backup to restore all notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: ALOKPATH BRAND & PRIVACY
            SettingsSectionHeader(title = "Alokpath Ecosystem", icon = Icons.Filled.Security)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                Image(
                    painter = painterResource(
                        if (isDark) R.drawable.alokpath_logo_dark else R.drawable.alokpath_logo_light
                    ),
                    contentDescription = "Alokpath Logo",
                    modifier = Modifier.height(26.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "An Alokpath Sovereign Privacy Product",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "আলোকপথের নিজস্ব অফলাইন ইকোসিস্টেম • ১০০% লোকাল ও প্রাইভেট",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                AboutInfoRow(title = "Architecture", subtitle = "100% Offline • Zero Analytics • Zero Cloud Sync")
                AboutInfoRow(title = "Display Engine", subtitle = "120Hz ProMotion & Low-End GPU Optimized")
                AboutInfoRow(title = "Parent Company", subtitle = "Alokpath Foundation (আলোকপথ)")
                AboutInfoRow(title = "App Version", subtitle = "v2.1.0 (Alokpath Edition)")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FontPill(
    label: String,
    sample: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sample,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutInfoRow(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
