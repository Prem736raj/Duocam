package com.example

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.db.Folder
import com.example.db.Script
import com.example.db.ScriptDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MutStateWrapper<T>(val value: T, val onValueChange: (T) -> Unit) {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, newValue: T) {
        onValueChange(newValue)
    }
}

@Composable
fun <T> rememberStateWrapper(value: T, onValueChange: (T) -> Unit): MutStateWrapper<T> {
    return remember(value) { MutStateWrapper(value, onValueChange) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleprompterProOverlayBlock(
    isTeleprompterEnabled: Boolean,
    onTeleprompterEnabledChange: (Boolean) -> Unit,
    isEditingScript: Boolean,
    onEditingScriptChange: (Boolean) -> Unit,
    teleprompterBgOpacity: Float,
    onTeleprompterBgOpacityChange: (Float) -> Unit,
    teleprompterTextColorName: String,
    onTeleprompterTextColorNameChange: (String) -> Unit,
    isTeleprompterFullScreen: Boolean,
    onTeleprompterFullScreenChange: (Boolean) -> Unit,
    teleprompterDragOffsetX: Float,
    onTeleprompterDragOffsetXChange: (Float) -> Unit,
    teleprompterDragOffsetY: Float,
    onTeleprompterDragOffsetYChange: (Float) -> Unit,
    isTeleprompterScrolling: Boolean,
    onTeleprompterScrollingChange: (Boolean) -> Unit,
    scriptInputText: String,
    onScriptInputTextChange: (String) -> Unit,
    teleprompterScript: String,
    onTeleprompterScriptChange: (String) -> Unit,
    teleprompterSpeedMode: String,
    onTeleprompterSpeedModeChange: (String) -> Unit,
    teleprompterSpeed: Float,
    onTeleprompterSpeedChange: (Float) -> Unit,
    teleprompterTextSize: Float,
    onTeleprompterTextSizeChange: (Float) -> Unit,
    teleprompterScrollOffset: Float,
    onTeleprompterScrollOffsetChange: (Float) -> Unit,
    activeScriptTab: String,
    onActiveScriptTabChange: (String) -> Unit,
    currentScriptId: Long?,
    onCurrentScriptIdChange: (Long?) -> Unit,
    scriptTitleInput: String,
    onScriptTitleInputChange: (String) -> Unit,
    selectedFolderId: Long?,
    onSelectedFolderIdChange: (Long?) -> Unit,
    selectedLibraryFolderFilter: Long?,
    onSelectedLibraryFolderFilterChange: (Long?) -> Unit,
    showCreateFolderDialog: Boolean,
    onShowCreateFolderDialogChange: (Boolean) -> Unit,
    newFolderNameInput: String,
    onNewFolderNameInputChange: (String) -> Unit,
    teleprompterShowSettings: Boolean,
    onTeleprompterShowSettingsChange: (Boolean) -> Unit,
    savedFolders: List<Folder>,
    savedScripts: List<Script>,
    scriptDao: ScriptDao,
    coroutineScope: CoroutineScope,
    sharedPreferences: SharedPreferences,
    context: Context,
    fileImportLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
    // Elegant property delegates mapped directly to parent parameters using shadow wrapper bindings
    var isTeleprompterEnabled by rememberStateWrapper(isTeleprompterEnabled) { onTeleprompterEnabledChange(it) }
    var isEditingScript by rememberStateWrapper(isEditingScript) { onEditingScriptChange(it) }
    var teleprompterBgOpacity by rememberStateWrapper(teleprompterBgOpacity) { onTeleprompterBgOpacityChange(it) }
    var teleprompterTextColorName by rememberStateWrapper(teleprompterTextColorName) { onTeleprompterTextColorNameChange(it) }
    var isTeleprompterFullScreen by rememberStateWrapper(isTeleprompterFullScreen) { onTeleprompterFullScreenChange(it) }
    var teleprompterDragOffsetX by rememberStateWrapper(teleprompterDragOffsetX) { onTeleprompterDragOffsetXChange(it) }
    var teleprompterDragOffsetY by rememberStateWrapper(teleprompterDragOffsetY) { onTeleprompterDragOffsetYChange(it) }
    var isTeleprompterScrolling by rememberStateWrapper(isTeleprompterScrolling) { onTeleprompterScrollingChange(it) }
    var scriptInputText by rememberStateWrapper(scriptInputText) { onScriptInputTextChange(it) }
    var teleprompterScript by rememberStateWrapper(teleprompterScript) { onTeleprompterScriptChange(it) }
    var teleprompterSpeedMode by rememberStateWrapper(teleprompterSpeedMode) { onTeleprompterSpeedModeChange(it) }
    var teleprompterSpeed by rememberStateWrapper(teleprompterSpeed) { onTeleprompterSpeedChange(it) }
    var teleprompterTextSize by rememberStateWrapper(teleprompterTextSize) { onTeleprompterTextSizeChange(it) }
    var teleprompterScrollOffset by rememberStateWrapper(teleprompterScrollOffset) { onTeleprompterScrollOffsetChange(it) }
    var activeScriptTab by rememberStateWrapper(activeScriptTab) { onActiveScriptTabChange(it) }
    var currentScriptId by rememberStateWrapper(currentScriptId) { onCurrentScriptIdChange(it) }
    var scriptTitleInput by rememberStateWrapper(scriptTitleInput) { onScriptTitleInputChange(it) }
    var selectedFolderId by rememberStateWrapper(selectedFolderId) { onSelectedFolderIdChange(it) }
    var selectedLibraryFolderFilter by rememberStateWrapper(selectedLibraryFolderFilter) { onSelectedLibraryFolderFilterChange(it) }
    var showCreateFolderDialog by rememberStateWrapper(showCreateFolderDialog) { onShowCreateFolderDialogChange(it) }
    var newFolderNameInput by rememberStateWrapper(newFolderNameInput) { onNewFolderNameInputChange(it) }
    var teleprompterShowSettings by rememberStateWrapper(teleprompterShowSettings) { onTeleprompterShowSettingsChange(it) }

    fun saveTeleprompterScript(scriptText: String) {
        teleprompterScript = scriptText
        sharedPreferences.edit().putString("teleprompter_script", scriptText).apply()
    }

    AnimatedVisibility(
        visible = isTeleprompterEnabled,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut()
    ) {
        val dynamicBgColor = if (isEditingScript) {
            com.example.ui.theme.ObsidianBlack.copy(alpha = 0.95f)
        } else {
            Color.Black.copy(alpha = teleprompterBgOpacity)
        }

        val activeTextColor = when (teleprompterTextColorName) {
            "neon_green" -> com.example.ui.theme.NeonGreen
            "yellow" -> com.example.ui.theme.GoldenHour
            "cyan" -> Color(0xFF22D3EE)
            "red" -> com.example.ui.theme.GlowRecordRed
            else -> Color.White
        }

        Box(
            modifier = if (isTeleprompterFullScreen) {
                Modifier
                    .fillMaxSize()
                    .background(dynamicBgColor)
                    .padding(top = 95.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
                    .zIndex(15f)
            } else {
                Modifier
                    .offset {
                        IntOffset(
                            teleprompterDragOffsetX.roundToInt(),
                            teleprompterDragOffsetY.roundToInt()
                        )
                    }
                    .size(width = 330.dp, height = if (isEditingScript) 410.dp else 290.dp)
                    .pointerInput(isEditingScript) {
                        if (!isEditingScript) {
                            detectTransformGestures { _, pan, _, _ ->
                                teleprompterDragOffsetX = (teleprompterDragOffsetX + pan.x).coerceIn(0f, 1000f)
                                teleprompterDragOffsetY = (teleprompterDragOffsetY + pan.y).coerceIn(0f, 2000f)
                            }
                        }
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(dynamicBgColor)
                    .border(1.2.dp, if (isTeleprompterScrolling) com.example.ui.theme.NeonGreen else com.example.ui.theme.BorderGray, RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .zIndex(15f)
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row with status indicator and utility toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTeleprompterScrolling) com.example.ui.theme.NeonGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditingScript) "PROMPTER CONFIG" else (if (isTeleprompterFullScreen) "TELEPROMPTER PRO" else "PROMPTER OVERLAY"),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = if (isTeleprompterScrolling) com.example.ui.theme.NeonGreen else com.example.ui.theme.TextWhite
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Settings Gear Icon Toggle
                        Icon(
                            imageVector = if (isEditingScript) Icons.Rounded.CheckCircle else Icons.Rounded.Settings,
                            contentDescription = "Edit Script and Settings",
                            tint = if (isEditingScript) com.example.ui.theme.NeonGreen else com.example.ui.theme.TextWhite.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222225))
                                .clickable {
                                    if (isEditingScript) {
                                        saveTeleprompterScript(scriptInputText)
                                    } else {
                                        scriptInputText = teleprompterScript
                                    }
                                    isEditingScript = !isEditingScript
                                }
                                .padding(4.dp)
                                .testTag("teleprompter_config_toggle")
                        )

                        // Layout Toggle (Full screen vs Windowed Floating)
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = "Layout size toggle",
                            tint = if (isTeleprompterFullScreen) com.example.ui.theme.GoldenHour else com.example.ui.theme.TextWhite.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222225))
                                .clickable { isTeleprompterFullScreen = !isTeleprompterFullScreen }
                                .padding(4.dp)
                                .testTag("teleprompter_fullscreen_toggle")
                        )
                        
                        // Close Overlay
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Prompter",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E1010))
                                .clickable { isTeleprompterEnabled = false }
                                .padding(4.dp)
                                .testTag("teleprompter_close_button")
                        )
                    }
                }
                
                // Main workspace
                if (isEditingScript) {
                    // CONFIGURATION INTERFACE (Tabbed Library & Editor + Customization Settings)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val readSpeedWPM = when (teleprompterSpeedMode) {
                            "slow" -> 100
                            "fast" -> 160
                            else -> 130
                        }
                        // Dynamic Tab Bar: Script Editor | Library List
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24)),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            listOf("editor" to "Script Editor", "library" to "My Scripts").forEach { (tab, label) ->
                                val isSel = activeScriptTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSel) com.example.ui.theme.NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { activeScriptTab = tab }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label.uppercase(),
                                        fontSize = 11.sp,
                                        color = if (isSel) com.example.ui.theme.NeonGreen else com.example.ui.theme.TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        if (activeScriptTab == "editor") {
                            // SCRIPT EDITOR WORKSPACE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentScriptId == null) "NEW SCRIPT TEMPLATE" else "EDITING SAVED SCRIPT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.NeonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                if (currentScriptId != null) {
                                    Text(
                                        text = "ID: #$currentScriptId",
                                        fontSize = 9.sp,
                                        color = com.example.ui.theme.TextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Script Title input
                            OutlinedTextField(
                                value = scriptTitleInput,
                                onValueChange = { scriptTitleInput = it },
                                placeholder = { Text("Enter script title...", color = Color.Gray, fontSize = 12.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = com.example.ui.theme.TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("script_title_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.NeonGreen,
                                    unfocusedBorderColor = com.example.ui.theme.BorderGray,
                                    unfocusedContainerColor = Color(0xFF161619),
                                    focusedContainerColor = Color(0xFF161619)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Folder Assignment selection Dropdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Assign Folder:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.TextWhite,
                                    fontFamily = FontFamily.Monospace
                                )

                                var showFolderSelector by remember { mutableStateOf(false) }
                                val activeFolderName = if (selectedFolderId == null) {
                                    "Uncategorized"
                                } else {
                                    savedFolders.find { it.id == selectedFolderId }?.name ?: "Uncategorized"
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF27272A))
                                        .clickable { showFolderSelector = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeFolderName.uppercase() + " ▾",
                                        fontSize = 9.sp,
                                        color = com.example.ui.theme.NeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )

                                    DropdownMenu(
                                        expanded = showFolderSelector,
                                        onDismissRequest = { showFolderSelector = false },
                                        modifier = Modifier.background(Color(0xFF1C1C1F))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("None (Uncategorized)", color = Color.White, fontSize = 11.sp) },
                                            onClick = {
                                                selectedFolderId = null
                                                showFolderSelector = false
                                            }
                                        )
                                        savedFolders.forEach { folder ->
                                            DropdownMenuItem(
                                                text = { Text(folder.name, color = Color.White, fontSize = 11.sp) },
                                                onClick = {
                                                    selectedFolderId = folder.id
                                                    showFolderSelector = false
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                // Clear all text button
                                Text(
                                    text = "CLEAR TEXT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.GlowRecordRed,
                                    modifier = Modifier.clickable { scriptInputText = "" }
                                )
                            }

                            // Script content Paste/Type Text Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF121214))
                                    .border(0.5.dp, com.example.ui.theme.BorderGray, RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                BasicTextField(
                                    value = scriptInputText,
                                    onValueChange = { scriptInputText = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = com.example.ui.theme.TextWhite,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp
                                    ),
                                    cursorBrush = Brush.verticalGradient(
                                        listOf(com.example.ui.theme.NeonGreen, com.example.ui.theme.NeonGreen)
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .testTag("teleprompter_text_editor")
                                )
                                if (scriptInputText.isEmpty()) {
                                    Text(
                                        text = "Paste or type script... Protip: Surround emphasis words in double asterisks, example: **emphasis content**",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }

                            // Smart Metadata calculation values
                            val wordCount = remember(scriptInputText) {
                                scriptInputText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                            }
                            val estReadTimeSecs = remember(scriptInputText) {
                                if (wordCount == 0) 0 else ((wordCount.toFloat() / readSpeedWPM) * 60f).toInt()
                            }
                            val formattedEstTime = String.format("%02d:%02d", estReadTimeSecs / 60, estReadTimeSecs % 60)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetadataLabelDisplay(key = "WORDS", value = "$wordCount")
                                MetadataLabelDisplay(key = "EST. READ", value = formattedEstTime)
                                
                                // File Import Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E1E24))
                                        .border(1.dp, com.example.ui.theme.BorderGray, RoundedCornerShape(6.dp))
                                        .clickable { fileImportLauncher.launch("text/plain") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Rounded.CloudUpload, contentDescription = null, tint = com.example.ui.theme.NeonGreen, modifier = Modifier.size(12.dp))
                                        Text(text = "IMPORT .TXT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.TextWhite)
                                    }
                                }
                            }
                            
                            // Clear inputs or create new
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scriptTitleInput = ""
                                        scriptInputText = ""
                                        currentScriptId = null
                                        selectedFolderId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("NEW TEMPLATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                
                                Button(
                                    onClick = {
                                        if (scriptInputText.isBlank()) {
                                            android.widget.Toast.makeText(context, "Cannot save an empty script!", android.widget.Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val finalTitle = scriptTitleInput.ifBlank { "Untitled Script" }
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val currentId = currentScriptId
                                            if (currentId == null) {
                                                // Insert script
                                                val newId = scriptDao.insertScript(
                                                    Script(
                                                        title = finalTitle,
                                                        content = scriptInputText,
                                                        folderId = selectedFolderId
                                                    )
                                                )
                                                withContext(Dispatchers.Main) {
                                                    currentScriptId = newId
                                                    scriptTitleInput = finalTitle
                                                    teleprompterScript = scriptInputText
                                                    sharedPreferences.edit().putString("teleprompter_script", scriptInputText).apply()
                                                    android.widget.Toast.makeText(context, "Script Saved!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                // Update existing script
                                                val existing = scriptDao.getScriptById(currentId)
                                                if (existing != null) {
                                                    scriptDao.updateScript(
                                                        existing.copy(
                                                            title = finalTitle,
                                                            content = scriptInputText,
                                                            folderId = selectedFolderId
                                                        )
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        teleprompterScript = scriptInputText
                                                        sharedPreferences.edit().putString("teleprompter_script", scriptInputText).apply()
                                                        android.widget.Toast.makeText(context, "Script Updated!", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.NeonGreen),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("save_script_btn"),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("SAVE TO DATABASE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        } else {
                            // SCRIPT LIBRARY SELECTOR WORKSPACE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT SCRIPT TO LOAD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.NeonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                // Create Folder option button
                                Text(
                                    text = "+ NEW FOLDER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.GoldenHour,
                                    modifier = Modifier.clickable { showCreateFolderDialog = true }
                                )
                            }
                            
                            // Folder Filter Select Bar (Horizontal Row)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    val isSel = selectedLibraryFolderFilter == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) com.example.ui.theme.NeonGreen.copy(alpha = 0.2f) else Color(0xFF1E1E24))
                                            .border(1.dp, if (isSel) com.example.ui.theme.NeonGreen else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable { selectedLibraryFolderFilter = null }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("ALL", fontSize = 9.sp, color = if (isSel) com.example.ui.theme.NeonGreen else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                item {
                                    val isSel = selectedLibraryFolderFilter == -1L
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) com.example.ui.theme.NeonGreen.copy(alpha = 0.2f) else Color(0xFF1E1E24))
                                            .border(1.dp, if (isSel) com.example.ui.theme.NeonGreen else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable { selectedLibraryFolderFilter = -1L }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("UNCATEGORIZED", fontSize = 9.sp, color = if (isSel) com.example.ui.theme.NeonGreen else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                items(savedFolders.size) { index ->
                                    val folder = savedFolders[index]
                                    val isSel = selectedLibraryFolderFilter == folder.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) com.example.ui.theme.NeonGreen.copy(alpha = 0.2f) else Color(0xFF1E1E24))
                                            .border(1.dp, if (isSel) com.example.ui.theme.NeonGreen else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable { selectedLibraryFolderFilter = folder.id }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = folder.name.uppercase(),
                                                fontSize = 9.sp,
                                                color = if (isSel) com.example.ui.theme.NeonGreen else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete folder",
                                                tint = Color.Red.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clickable {
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            scriptDao.deleteFolder(folder)
                                                            if (selectedLibraryFolderFilter == folder.id) {
                                                                selectedLibraryFolderFilter = null
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            // Filtered list of scripts
                            val filteredScripts = remember(savedScripts, selectedLibraryFolderFilter) {
                                when (selectedLibraryFolderFilter) {
                                    null -> savedScripts
                                    -1L -> savedScripts.filter { it.folderId == null }
                                    else -> savedScripts.filter { it.folderId == selectedLibraryFolderFilter }
                                }
                            }

                            if (filteredScripts.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF111113), RoundedCornerShape(12.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF1C1C1E)), RoundedCornerShape(12.dp))
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Description,
                                        contentDescription = "No scripts",
                                        tint = Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No saved scripts found in this category.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.LightGray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Create a script using the input panel above.",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    filteredScripts.forEach { script ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF161619))
                                                .border(0.5.dp, com.example.ui.theme.BorderGray, RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = script.title.uppercase(), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val folderLabel = if (script.folderId == null) {
                                                        "UNCATEGORIZED"
                                                    } else {
                                                        savedFolders.find { it.id == script.folderId }?.name ?: "UNCATEGORIZED"
                                                    }
                                                    Text(text = "FOLDER: $folderLabel", fontSize = 8.sp, color = com.example.ui.theme.NeonGreen, fontWeight = FontWeight.SemiBold)
                                                    Text(text = "WORDS: ${script.wordCount}", fontSize = 8.sp, color = Color.Gray)
                                                }
                                            }
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowForward,
                                                    contentDescription = "Load script",
                                                    tint = com.example.ui.theme.NeonGreen,
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1C2C1D))
                                                        .clickable {
                                                            currentScriptId = script.id
                                                            scriptTitleInput = script.title
                                                            scriptInputText = script.content
                                                            selectedFolderId = script.folderId
                                                            teleprompterScript = script.content
                                                            sharedPreferences.edit().putString("teleprompter_script", script.content).apply()
                                                            activeScriptTab = "editor"
                                                        }
                                                        .padding(4.dp)
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "Delete script",
                                                    tint = Color.Red.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF331616))
                                                        .clickable {
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                scriptDao.deleteScript(script)
                                                                if (currentScriptId == script.id) {
                                                                    currentScriptId = null
                                                                    scriptTitleInput = ""
                                                                    scriptInputText = ""
                                                                    selectedFolderId = null
                                                                }
                                                            }
                                                        }
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // NEW FOLDER CREATION OVERLAY SHEET (embedded dialog state trigger)
                        if (showCreateFolderDialog) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                                    .border(1.dp, com.example.ui.theme.BorderGray, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "CREATE NEW SCRIPT FOLDER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.NeonGreen)
                                    OutlinedTextField(
                                        value = newFolderNameInput,
                                        onValueChange = { newFolderNameInput = it },
                                        placeholder = { Text("folder name...", color = Color.Gray, fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = com.example.ui.theme.NeonGreen,
                                            unfocusedBorderColor = com.example.ui.theme.BorderGray
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "CANCEL", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.clickable { showCreateFolderDialog = false })
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "CREATE",
                                            fontSize = 10.sp,
                                            color = com.example.ui.theme.NeonGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                if (newFolderNameInput.isNotBlank()) {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        scriptDao.insertFolder(Folder(name = newFolderNameInput))
                                                        withContext(Dispatchers.Main) {
                                                            newFolderNameInput = ""
                                                            showCreateFolderDialog = false
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // CONFIGURABLE OPACITY, COLORS, SIZE ACCENTS
                        Divider(color = com.example.ui.theme.BorderGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "CUSTOM DISPLAY PRESETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.TextWhite, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (teleprompterShowSettings) "HIDE SETTINGS ▴" else "SHOW SETTINGS ▾",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.NeonGreen,
                                modifier = Modifier.clickable { teleprompterShowSettings = !teleprompterShowSettings }
                            )
                        }

                        AnimatedVisibility(visible = teleprompterShowSettings) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 1. Speed Preset Selector Mode (Slow, Medium, Fast, or custom slider)
                                Text(text = "SCROLL SPEED MODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.TextWhite)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("slow" to "Slow", "medium" to "Medium", "fast" to "Fast", "custom" to "Custom").forEach { (mode, name) ->
                                        val isSel = teleprompterSpeedMode == mode
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSel) com.example.ui.theme.NeonGreen.copy(alpha = 0.15f) else Color(0xFF1E1E24), RoundedCornerShape(6.dp))
                                                .border(1.dp, if (isSel) com.example.ui.theme.NeonGreen else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    teleprompterSpeedMode = mode
                                                    sharedPreferences.edit().putString("teleprompter_speed_mode", mode).apply()
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = name, fontSize = 9.sp, color = if (isSel) com.example.ui.theme.NeonGreen else Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (teleprompterSpeedMode == "custom") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Slider(
                                            value = teleprompterSpeed,
                                            onValueChange = {
                                                teleprompterSpeed = it
                                                sharedPreferences.edit().putFloat("teleprompter_speed", it).apply()
                                            },
                                            valueRange = 1f..15f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = com.example.ui.theme.NeonGreen,
                                                activeTrackColor = com.example.ui.theme.NeonGreen
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${teleprompterSpeed.toInt()}x",
                                            fontSize = 11.sp,
                                            color = com.example.ui.theme.TextWhite,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // 2. Text Size Preset slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "TEXT SIZE (${teleprompterTextSize.toInt()}sp)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.TextWhite
                                    )
                                    Slider(
                                        value = teleprompterTextSize,
                                        onValueChange = {
                                            teleprompterTextSize = it
                                            sharedPreferences.edit().putFloat("teleprompter_text_size", it).apply()
                                        },
                                        valueRange = 14f..46f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = com.example.ui.theme.NeonGreen,
                                            activeTrackColor = com.example.ui.theme.NeonGreen
                                        )
                                    )
                                }

                                // 3. Background transparency opacity preset slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "BG OPACITY (${(teleprompterBgOpacity * 100).toInt()}%)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.TextWhite
                                    )
                                    Slider(
                                        value = teleprompterBgOpacity,
                                        onValueChange = {
                                            teleprompterBgOpacity = it
                                            sharedPreferences.edit().putFloat("teleprompter_bg_opacity", it).apply()
                                        },
                                        valueRange = 0.0f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = com.example.ui.theme.NeonGreen,
                                            activeTrackColor = com.example.ui.theme.NeonGreen
                                        )
                                    )
                                }

                                // 4. Reader Spotlight indicator color presets
                                Text(text = "TEXT INDICATOR COLORS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.TextWhite)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("white" to "White", "neon_green" to "Green", "yellow" to "Amber", "cyan" to "Cyan", "red" to "Red").forEach { (name, label) ->
                                        val isSel = teleprompterTextColorName == name
                                        val cVal = when (name) {
                                            "neon_green" -> com.example.ui.theme.NeonGreen
                                            "yellow" -> com.example.ui.theme.GoldenHour
                                            "cyan" -> Color(0xFF22D3EE)
                                            "red" -> com.example.ui.theme.GlowRecordRed
                                            else -> Color.White
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSel) com.example.ui.theme.NeonGreen.copy(alpha = 0.15f) else Color(0xFF1E1E24), RoundedCornerShape(6.dp))
                                                .border(1.dp, if (isSel) com.example.ui.theme.NeonGreen else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    teleprompterTextColorName = name
                                                    sharedPreferences.edit().putString("teleprompter_text_color_name", name).apply()
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = label, fontSize = 9.sp, color = cVal, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // RUNNING SCROLLER MODE (The actual scrolling camera cues)
                    val scrollState = rememberScrollState()
                    LaunchedEffect(teleprompterScrollOffset) {
                        try {
                            scrollState.scrollTo(teleprompterScrollOffset.toInt())
                        } catch (e: Exception) {
                            Log.e("Teleprompter", "Scroll error", e)
                        }
                    }

                    val dynamicMultiplier = when (teleprompterSpeedMode) {
                        "slow" -> 0.45f
                        "medium" -> 0.9f
                        "fast" -> 2.1f
                        else -> teleprompterSpeed * 0.25f
                    }

                    LaunchedEffect(isTeleprompterScrolling, dynamicMultiplier) {
                        if (isTeleprompterScrolling) {
                            while (isTeleprompterScrolling) {
                                teleprompterScrollOffset = (teleprompterScrollOffset + dynamicMultiplier).coerceAtLeast(0f)
                                delay(16L) // ~60fps target ticks
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .pointerInput(isTeleprompterScrolling) {
                                detectTransformGestures(
                                    onGesture = { _, pan, _, _ ->
                                        if (pan.y != 0f) {
                                            teleprompterScrollOffset = (teleprompterScrollOffset - pan.y).coerceAtLeast(0f)
                                        }
                                    }
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .pointerInput(isTeleprompterScrolling) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            isTeleprompterScrolling = !isTeleprompterScrolling
                                        },
                                        onLongPress = {
                                            teleprompterScrollOffset = 0f
                                            isTeleprompterScrolling = false
                                        },
                                        onTap = {
                                            teleprompterScrollOffset = (teleprompterScrollOffset - 10f).coerceAtLeast(0f)
                                        }
                                    )
                                }
                        ) {
                            Spacer(modifier = Modifier.height(if (isTeleprompterFullScreen) 220.dp else 120.dp))
                            
                            val annot = remember(teleprompterScript, activeTextColor) {
                                parseEmphasisText(teleprompterScript, activeTextColor)
                            }
                            Text(
                                text = annot,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = teleprompterTextSize.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = (teleprompterTextSize * 1.4f).sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(if (isTeleprompterFullScreen) 320.dp else 180.dp))
                        }
                        
                        // Reading guide / spotlight bar to denote reading focus
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(if (isTeleprompterFullScreen) 54.dp else 36.dp)
                                .background(com.example.ui.theme.NeonGreen.copy(alpha = 0.12f))
                                .border(width = 0.8.dp, color = com.example.ui.theme.NeonGreen.copy(alpha = 0.45f))
                        ) {
                            // Highlight anchors on edges
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 4.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.NeonGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.NeonGreen)
                            )
                        }
                    }
                }
                
                // Footer / Media Control panel
                if (!isEditingScript) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Scroll Play/Pause Toggle button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isTeleprompterScrolling) com.example.ui.theme.GoldenHour else com.example.ui.theme.NeonGreen)
                                    .clickable { isTeleprompterScrolling = !isTeleprompterScrolling }
                                    .testTag("teleprompter_toggle_scrolling")
                            ) {
                                Icon(
                                    imageVector = if (isTeleprompterScrolling) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Toggle scrolling",
                                    tint = com.example.ui.theme.ObsidianBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Fast Rewind Reset button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222225))
                                    .border(1.dp, com.example.ui.theme.BorderGray, CircleShape)
                                    .clickable {
                                        teleprompterScrollOffset = 0f
                                        isTeleprompterScrolling = false
                                    }
                                    .testTag("teleprompter_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropUp,
                                    contentDescription = "Reset scroll",
                                    tint = com.example.ui.theme.TextWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        val speedLabelDisplay = when (teleprompterSpeedMode) {
                            "slow" -> "Slow"
                            "medium" -> "Medium"
                            "fast" -> "Fast"
                            else -> "${teleprompterSpeed.toInt()}x"
                        }
                        Text(
                            text = "Speed: $speedLabelDisplay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configure layouts & values above",
                            fontSize = 10.sp,
                            color = com.example.ui.theme.TextMuted
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(com.example.ui.theme.NeonGreen)
                                .clickable {
                                    saveTeleprompterScript(scriptInputText)
                                    isEditingScript = false
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("teleprompter_save_button")
                        ) {
                            Text(
                                text = "SAVE & RUN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.ObsidianBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataLabelDisplay(key: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = key,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = com.example.ui.theme.TextMuted,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
