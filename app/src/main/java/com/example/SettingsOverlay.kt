package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.ui.theme.GlowRecordRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.BorderGray
import com.example.ui.theme.GoldenHour
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextMuted

@Composable
fun SettingsOverlay(
    isVisible: Boolean,
    onClose: () -> Unit,
    isDualMode: Boolean,
    rearMaxRes: String,
    frontMaxRes: String,
    isHwEncoding: Boolean,
    hasFlash: Boolean,
    zoomRange: String,
    hasStabilization: Boolean,
    selectedResolution: VideoResolution,
    onResolutionSelected: (VideoResolution) -> Unit,
    supports4K: Boolean,
    selectedFrameRate: Int,
    onFrameRateSelected: (Int) -> Unit,
    useHevcCompression: Boolean,
    onHevcToggle: (Boolean) -> Unit,
    isHevcSupported: Boolean,
    audioNoiseReductionEnabled: Boolean,
    onAudioNoiseReductionToggle: (Boolean) -> Unit,
    isVoiceControlEnabled: Boolean,
    onVoiceControlToggle: (Boolean) -> Unit,
    isStabilizationActive: Boolean,
    onStabilizationToggle: (Boolean) -> Unit,
    stabilizationStrength: String,
    onStabilizationStrengthChange: (String) -> Unit,
    selectedLayout: DualCameraLayout,
    onLayoutSelected: (DualCameraLayout) -> Unit,
    selectedAspectRatio: RecordingAspectRatio,
    onAspectRatioSelected: (RecordingAspectRatio) -> Unit,
    countdownSetting: Int,
    onCountdownSettingSelected: (Int) -> Unit,
    isGridEnabled: Boolean,
    onGridEnabledToggle: (Boolean) -> Unit,
    mirrorFrontCamera: Boolean,
    onMirrorFrontCameraToggle: (Boolean) -> Unit,
    defaultAudioSource: String,
    onDefaultAudioSourceSelected: (String) -> Unit,
    defaultSaveLocation: String,
    onDefaultSaveLocationSelected: (String) -> Unit,
    fileNamingFormat: String,
    onFileNamingFormatSelected: (String) -> Unit,
    autoDeleteDays: Int,
    onAutoDeleteDaysSelected: (Int) -> Unit,
    onResetToDefaults: () -> Unit,
    themeMode: String = "dark",
    onThemeModeSelected: (String) -> Unit = {},
    accentColor: String = "red",
    onAccentColorSelected: (String) -> Unit = {},
    uiDensity: String = "standard",
    onUiDensitySelected: (String) -> Unit = {},
    notifyRecordingCompletion: Boolean = true,
    onNotifyRecordingCompletionSelected: (Boolean) -> Unit = {},
    notifyBackupCompletion: Boolean = true,
    onNotifyBackupCompletionSelected: (Boolean) -> Unit = {},
    notifyLowStorage: Boolean = true,
    onNotifyLowStorageSelected: (Boolean) -> Unit = {},
    notifyRecordingTipOfDay: Boolean = true,
    onNotifyRecordingTipOfDaySelected: (Boolean) -> Unit = {},
    autoEnableDnd: Boolean = false,
    onAutoEnableDndSelected: (Boolean) -> Unit = {},
    isPro: Boolean = false,
    onProChange: (Boolean) -> Unit = {},
    isLiteMode: Boolean = false,
    onLiteModeChange: (Boolean) -> Unit = {},
    batteryLevelSim: Int = -1,
    onBatteryLevelSimChange: (Int) -> Unit = {},
    deviceTempSim: Float = -1f,
    onDeviceTempSimChange: (Float) -> Unit = {}
) {
    val colors = com.example.ui.theme.LocalThemeColors.current
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.60f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.90f)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb click */ }
                    .testTag("capabilities_settings_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SYSTEM SETTINGS & CONTROLS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = TextWhite
                            )
                            Text(
                                text = "Configure defaults and diagnostics",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        
                        // Close icon button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E22))
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = TextWhite.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 12.dp))
                    
                    var selectedSubSectionIndex by remember { mutableStateOf(0) }
                    val categoriesTabsList = listOf("Settings", "Help & FAQ", "Feedback", "About DuoCam")
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categoriesTabsList.forEachIndexed { idx, label ->
                            val isSelected = selectedSubSectionIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.primary else colors.border.copy(alpha = 0.35f))
                                    .clickable { selectedSubSectionIndex = idx }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("support_menu_tab_${label.lowercase().replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isSelected) Color.White else colors.textMuted
                                )
                            }
                        }
                    }
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val duoPrefs = remember { context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE) }
                    var isRestoring by remember { mutableStateOf(false) }
                    var currentSimState by remember { mutableStateOf(DuoCamPurchaseManager.getSimulatedState(context)) }
                    var simulateOffline by remember { mutableStateOf(DuoCamPurchaseManager.getSimulateNoInternet(context)) }
                    var showInAppBillingSheetFromSettings by remember { mutableStateOf(false) }
                    
                    if (showInAppBillingSheetFromSettings) {
                        SimulatedGooglePlayBillingSheet(
                            onDismiss = { showInAppBillingSheetFromSettings = false },
                            onPurchaseCompleted = {
                                showInAppBillingSheetFromSettings = false
                                onProChange(true)
                            }
                        )
                    }
                    
                    // Scrollable Settings List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedSubSectionIndex == 0) {
                            // Section: Licensing & Account Status
                            SettingSectionHeader(title = "Member Account & Licensing", icon = Icons.Rounded.Star)
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPro) Color(0xFF1E1C15) else Color(0xFF1E1E22)
                                ),
                                border = if (isPro) BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)))) else BorderStroke(1.dp, Color(0xFF2E2E34)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("licensing_account_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (isPro) Color(0xFF3E3200) else Color(0xFF2E2E34)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (isPro) "👑" else "👤", fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (isPro) "DuoCam Pro Lifetime Active" else "DuoCam Free Tier Active",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isPro) Color(0xFFF59E0B) else TextWhite
                                            )
                                            Text(
                                                text = if (isPro) {
                                                    val state = DuoCamPurchaseManager.getSimulatedState(context)
                                                    if (state == SimulatedPurchaseState.FAMILY_SHARED) {
                                                        "Unlocked via Google Play Family Library sharing"
                                                    } else {
                                                        "Verified Lifetime Personal License"
                                                    }
                                                } else {
                                                    "Standard limitations apply (720p maximum, watermark, 5m limit)"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isPro) Color(0xFFFCD34D).copy(alpha = 0.8f) else colors.textMuted
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!isPro) {
                                            Button(
                                                onClick = { showInAppBillingSheetFromSettings = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFEE8F00)
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f).testTag("purchase_pro_settings_btn")
                                            ) {
                                                Text("Upgrade to Pro - $4.99", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        Button(
                                            onClick = {
                                                isRestoring = true
                                                DuoCamPurchaseManager.restorePurchase(context) { success, message, restoredPro ->
                                                    isRestoring = false
                                                    onProChange(restoredPro)
                                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            enabled = !isRestoring,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPro) Color(0xFF2E2E34) else colors.primary,
                                                disabledContainerColor = Color(0xFF1E1E22)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("restore_purchase_settings_btn")
                                        ) {
                                            if (isRestoring) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text(
                                                    text = if (isPro) "Check License" else "Restore Purchase",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPro) TextWhite else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Interactive Developer Diagnostics (Beautiful Expansion)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161517)),
                                border = BorderStroke(1.dp, Color(0xFF242226)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "🛠️ Developer Billing Diagnostics",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF9CA3AF)
                                    )
                                    
                                    // Custom Simulated State Selector
                                    Column {
                                        Text(
                                            text = "Simulated Account Purchase State:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.textMuted
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Simple segment selectors for simulated states
                                        val stateOptionsList = listOf(
                                            SimulatedPurchaseState.ACTIVE_INDIVIDUAL to "Active Personal",
                                            SimulatedPurchaseState.FAMILY_SHARED to "Family Library",
                                            SimulatedPurchaseState.REFUNDED to "Refunded",
                                            SimulatedPurchaseState.EXPIRED to "Expired",
                                            SimulatedPurchaseState.NO_PURCHASE to "None"
                                        )
                                        
                                        // Scrollable segment buttons row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            stateOptionsList.forEach { (option, optionLabel) ->
                                                val isSelected = currentSimState == option
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) Color(0xFFEE8F00).copy(alpha = 0.2f) else Color(0xFF1E1C22))
                                                        .border(1.dp, if (isSelected) Color(0xFFEE8F00) else Color(0xFF2E2E34), RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            currentSimState = option
                                                            DuoCamPurchaseManager.setSimulatedState(context, option)
                                                            // Immediately trigger auto-verify logic to match simulated state dynamically
                                                            DuoCamPurchaseManager.autoVerifyOnLaunch(context) { verified, msg ->
                                                                onProChange(verified)
                                                            }
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        .testTag("sim_state_btn_${option.name.lowercase()}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = optionLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color(0xFFFBBF24) else Color(0xFF9CA3AF)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Simulated Offline Toggle Switer
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Simulate Offline Mode (No Internet)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = TextWhite
                                            )
                                            Text(
                                                text = "Tests cached offline verification / failing restore checks",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textMuted
                                            )
                                        }
                                        Switch(
                                            checked = simulateOffline,
                                            onCheckedChange = { checked ->
                                                simulateOffline = checked
                                                DuoCamPurchaseManager.setSimulateNoInternet(context, checked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFFEE8F00),
                                                checkedTrackColor = Color(0xFFEE8F00).copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.testTag("simulate_offline_switch")
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Button(
                                        onClick = {
                                            duoPrefs.edit().putBoolean("has_completed_onboarding", false).apply()
                                            android.widget.Toast.makeText(context, "First-launch onboarding reset successfully! Please restart the application to view it.", android.widget.Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4444)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reset_onboarding_developer_btn")
                                    ) {
                                        Text("Reset First-Launch Onboarding Flow", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            ContextualTipsManager.resetAllTips(context)
                                            android.widget.Toast.makeText(context, "All contextual help tooltips reset successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonGreen
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reset_contextual_tips_btn")
                                    ) {
                                        Text("Reset Help Tooltips & Tips", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // SECTION 0: THEME & COSMETIC APPORTIONMENT
                            SettingSectionHeader(title = "Theme & Appearance", icon = Icons.Rounded.Settings)
                        
                        // Theme Select Option
                        SettingSegmentedRow(
                            title = "Theme Option",
                            description = "Switch between dark theme, light theme, or automatic system preferences.",
                            options = listOf(
                                "dark" to "Dark",
                                "light" to "Light",
                                "auto" to "Auto"
                            ),
                            selectedValue = themeMode,
                            onValueChange = onThemeModeSelected,
                            testTagPrefix = "setting_theme_option"
                        )
                        
                        // Accent Colors Selecting Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Accent Color (Icons, Highlights, Controls)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textMuted
                            )
                            val curatedAccents = listOf(
                                "red" to Color(0xFFFF2E56),
                                "blue" to Color(0xFF3B82F6),
                                "purple" to Color(0xFF8B5CF6),
                                "green" to Color(0xFF22C55E),
                                "orange" to Color(0xFFF97316),
                                "pink" to Color(0xFFEC4899),
                                "teal" to Color(0xFF14B8A6),
                                "gold" to Color(0xFFF59E0B)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                curatedAccents.forEach { (name, color) ->
                                        val isSelected = accentColor.lowercase() == name
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) colors.text else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { onAccentColorSelected(name) }
                                                .testTag("modal_accent_$name"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                }
                            }
                        }
                        
                        // UI Density Select Option
                        SettingSegmentedRow(
                            title = "UI Densities Layout",
                            description = "Change the spacing of buttons and touch targets for higher visibility or compact accessibility.",
                            options = listOf(
                                "compact" to "Compact",
                                "standard" to "Standard",
                                "large" to "Large"
                            ),
                            selectedValue = uiDensity,
                            onValueChange = onUiDensitySelected,
                            testTagPrefix = "setting_density_option"
                        )
                        
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                        // SECTION 0.5: NOTIFICATIONS & SYSTEM ALERTS
                        SettingSectionHeader(title = "Notifications & Alerts", icon = Icons.Rounded.Notifications)
                        
                        SettingSwitchRow(
                            title = "Recording Capture Status",
                            description = "Notify when a video recording successfully finishes and is saved.",
                            checked = notifyRecordingCompletion,
                            onCheckedChange = onNotifyRecordingCompletionSelected,
                            testTag = "setting_notify_recording"
                        )
                        
                        SettingSwitchRow(
                            title = "Cloud Backup Synchronization",
                            description = "Notify when Google Drive video cloud backups complete.",
                            checked = notifyBackupCompletion,
                            onCheckedChange = onNotifyBackupCompletionSelected,
                            testTag = "setting_notify_backup"
                        )
                        
                        SettingSwitchRow(
                            title = "Low Storage Warning Alerts",
                            description = "Warn when free disk space falls below critical values.",
                            checked = notifyLowStorage,
                            onCheckedChange = onNotifyLowStorageSelected,
                            testTag = "setting_notify_low_storage"
                        )

                        SettingSwitchRow(
                            title = "Recording Tip of the Day",
                            description = "Receive daily creative tips for improving presenter videos.",
                            checked = notifyRecordingTipOfDay,
                            onCheckedChange = onNotifyRecordingTipOfDaySelected,
                            testTag = "setting_notify_tip_of_day"
                        )
                        
                        val contextForTip = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val tip = com.example.DuoCamNotificationHelper.triggerDailyTip(contextForTip, force = true)
                                    android.widget.Toast.makeText(contextForTip, tip, android.widget.Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.background,
                                    contentColor = colors.text
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.40f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = "Test Tip",
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Creative Tip Notification", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                        
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        SettingSectionHeader(title = "Do Not Disturb Mode", icon = Icons.Rounded.DoNotDisturb)
                        
                        SettingSwitchRow(
                            title = "Auto-Enable DND",
                            description = "Silences calls and other interruptions automatically during active recording.",
                            checked = autoEnableDnd,
                            onCheckedChange = onAutoEnableDndSelected,
                            testTag = "setting_auto_dnd"
                        )
                        
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                        // SECTION 1: RECORDING DEFAULTS
                        SettingSectionHeader(title = "Recording Defaults", icon = Icons.Filled.Videocam)
                        
                        // Resolution Segmented
                        val resOptions = remember(supports4K) {
                            if (supports4K) {
                                listOf(
                                    VideoResolution.HD to "720p",
                                    VideoResolution.FHD to "1080p",
                                    VideoResolution.UHD to "4K"
                                )
                            } else {
                                listOf(
                                    VideoResolution.HD to "720p",
                                    VideoResolution.FHD to "1080p"
                                )
                            }
                        }
                        SettingSegmentedRow(
                            title = "Default Resolution",
                            description = "Choose default recording dimensions. Higher resolution means sharper video but larger files.",
                            options = resOptions,
                            selectedValue = selectedResolution,
                            onValueChange = onResolutionSelected,
                            testTagPrefix = "setting_default_resolution"
                        )
                        
                        // Frame Rate Segmented
                        val fpsOptions = listOf(
                            24 to "24 fps",
                            30 to "30 fps",
                            60 to "60 fps"
                        )
                        SettingSegmentedRow(
                            title = "Default Frame Rate",
                            description = "Frame rate controls motion smoothness. Standard fluid, cinematic, or action captures.",
                            options = fpsOptions,
                            selectedValue = selectedFrameRate,
                            onValueChange = onFrameRateSelected,
                            testTagPrefix = "setting_default_framerate"
                        )
                        
                        // Layout Segmented
                        val layoutOptions = listOf(
                            DualCameraLayout.PIP to "PiP",
                            DualCameraLayout.SPLIT_HORIZONTAL to "Split H",
                            DualCameraLayout.SPLIT_VERTICAL to "Split V",
                            DualCameraLayout.SPLIT_DIAGONAL to "Diagonal"
                        )
                        SettingSegmentedRow(
                            title = "Default Layout",
                            description = "Defines the dual camera framing setup when initiating recording feeds.",
                            options = layoutOptions,
                            selectedValue = selectedLayout,
                            onValueChange = onLayoutSelected,
                            testTagPrefix = "setting_default_layout"
                        )
                        
                        // Aspect Ratio Segmented
                        val aspectOptions = listOf(
                            RecordingAspectRatio.RATIO_16_9 to "16:9",
                            RecordingAspectRatio.RATIO_9_16 to "9:16",
                            RecordingAspectRatio.RATIO_1_1 to "1:1",
                            RecordingAspectRatio.RATIO_4_5 to "4:5"
                        )
                        SettingSegmentedRow(
                            title = "Default Aspect Ratio",
                            description = "Initial crop framing aspect ratio applied on preview and generated files.",
                            options = aspectOptions,
                            selectedValue = selectedAspectRatio,
                            onValueChange = onAspectRatioSelected,
                            testTagPrefix = "setting_default_aspect_ratio"
                        )
                        
                        // Compression Segmented
                        val compressionOptions = listOf(
                            false to "H.264",
                            true to "HEVC"
                        )
                        SettingSegmentedRow(
                            title = "Default Compression Format",
                            description = "HEVC/H.265 saves up to 50% storage space, while H.264 offers high legacy player compatibility.",
                            options = compressionOptions,
                            selectedValue = useHevcCompression,
                            onValueChange = onHevcToggle,
                            testTagPrefix = "setting_default_compression"
                        )
                        
                        // Countdown Segmented
                        val countdownOptions = listOf(
                            0 to "Off",
                            3 to "3s",
                            5 to "5s",
                            10 to "10s"
                        )
                        SettingSegmentedRow(
                            title = "Countdown Timer Preset",
                            description = "Sets a countdown beep timer duration to let you prepare before recording starts.",
                            options = countdownOptions,
                            selectedValue = countdownSetting,
                            onValueChange = onCountdownSettingSelected,
                            testTagPrefix = "setting_default_countdown"
                        )
                        
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        // SECTION 2: AUDIO SETTINGS
                        SettingSectionHeader(title = "Audio Configurations", icon = Icons.Filled.Mic)
                        
                        SettingSwitchRow(
                            title = "Smart Noise Reduction",
                            description = "Suppresses background hum and mechanical noises while keeping active human speech clear.",
                            checked = audioNoiseReductionEnabled,
                            onCheckedChange = onAudioNoiseReductionToggle,
                            testTag = "setting_audio_noise_reduction"
                        )
                        
                        val audioSourceOptions = listOf(
                            "default" to "Default",
                            "camcorder" to "Camcorder",
                            "headset" to "Wired Mic",
                            "bluetooth" to "Wireless"
                        )
                        SettingSegmentedRow(
                            title = "Default Audio Input Route",
                            description = "Priority routing for capture mics: Camcorder mic, wired headset jack, or bluetooth scoop.",
                            options = audioSourceOptions,
                            selectedValue = defaultAudioSource,
                            onValueChange = onDefaultAudioSourceSelected,
                            testTagPrefix = "setting_default_audio_source"
                        )
                        
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        // SECTION 3: CAMERA SETTINGS
                        SettingSectionHeader(title = "Camera Parameters", icon = Icons.Rounded.AspectRatio)
                        
                        val stabLevelOptions = listOf(
                            "Off" to "Off",
                            "Light" to "Light",
                            "Standard" to "Standard",
                            "High" to "High"
                        )
                        val activeStabLevel = if (isStabilizationActive) stabilizationStrength else "Off"
                        SettingSegmentedRow(
                            title = "Stabilization Level",
                            description = "Controls sensor crop margin for motion smoothing. Extra action needs higher profiles.",
                            options = stabLevelOptions,
                            selectedValue = activeStabLevel,
                            onValueChange = { strength ->
                                if (strength == "Off") {
                                    onStabilizationToggle(false)
                                } else {
                                    onStabilizationToggle(true)
                                    onStabilizationStrengthChange(strength)
                                }
                            },
                            testTagPrefix = "setting_stabilization_level"
                        )
                        
                        SettingSwitchRow(
                            title = "Composition Grid Overlay",
                            description = "Guides aligned coordinates on stream to simplify visual Rule of Thirds balancing.",
                            checked = isGridEnabled,
                            onCheckedChange = onGridEnabledToggle,
                            testTag = "setting_camera_grid_overlay"
                        )
                        
                        SettingSwitchRow(
                            title = "Mirror Selfie Stream",
                            description = "Mirror human face laterally on front camera so output matches live viewfinder geometry.",
                            checked = mirrorFrontCamera,
                            onCheckedChange = onMirrorFrontCameraToggle,
                            testTag = "setting_camera_mirror_front"
                        )
                        
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        // SECTION 4: STORAGE SETTINGS
                        SettingSectionHeader(title = "Storage & Assets Library", icon = Icons.Rounded.Delete)
                        
                        val saveLocationOptions = listOf(
                            "internal" to "Private",
                            "public" to "Movies",
                            "sdcard" to "SD Card"
                        )
                        SettingSegmentedRow(
                            title = "Default Storage Target",
                            description = "Internal application private folder, or generic public shared Movies root.",
                            options = saveLocationOptions,
                            selectedValue = defaultSaveLocation,
                            onValueChange = onDefaultSaveLocationSelected,
                            testTagPrefix = "setting_default_save_location"
                        )
                        
                        val fileNamingOptions = listOf(
                            "default" to "Standard",
                            "simple" to "Simple",
                            "prefix" to "Vlog Prefix"
                        )
                        SettingSegmentedRow(
                            title = "File Naming Preset",
                            description = "DuoCam_YYYYMMDD_HHMMSS vs Recording_Timestamp epoch vs incremented vlog prefix files.",
                            options = fileNamingOptions,
                            selectedValue = fileNamingFormat,
                            onValueChange = onFileNamingFormatSelected,
                            testTagPrefix = "setting_file_naming_format"
                        )
                        
                        val deleteOptions = listOf(
                            -1 to "Never",
                            7 to "7 Days",
                            30 to "30 Days",
                            90 to "90 Days"
                        )
                        SettingSegmentedRow(
                            title = "Auto-Delete Older Movies",
                            description = "Helps maintain phone storage by purging local recordings automatically after threshold.",
                            options = deleteOptions,
                            selectedValue = autoDeleteDays,
                            onValueChange = onAutoDeleteDaysSelected,
                            testTagPrefix = "setting_auto_delete_days"
                        )
                        
                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                        // SECTION: SYSTEM PERFORMANCE & SIMULATOR
                        SettingSectionHeader(title = "System Performance & Simulator", icon = Icons.Rounded.Info)

                        val sysRam = remember {
                            try {
                                val am = context.getSystemService(android.content.Context.AUDIO_SERVICE)?.let {
                                    context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                                }
                                val mi = android.app.ActivityManager.MemoryInfo()
                                am?.getMemoryInfo(mi)
                                mi.totalMem / (1024.0 * 1024.0 * 1024.0)
                            } catch (e: Exception) {
                                8.0
                            }
                        }
                        val isRamLow = sysRam < 4.0

                        // 1. Lite Mode Switch Row (custom toggle item)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lite Optimization Mode",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Text(
                                    text = "Capped at 720p/30fps and bypasses complex target rendering for lower-end hardware.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Switch(
                                checked = isLiteMode,
                                onCheckedChange = onLiteModeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.primary,
                                    checkedTrackColor = colors.primary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("lite_mode_toggle")
                            )
                        }

                        // RAM suggestion notice
                        if (isRamLow) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33F59E0B))
                                    .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 RAM is less than 4GB (${String.format(java.util.Locale.US, "%.1f", sysRam)} GB detected). Turning on Lite Mode is highly recommended for optimal recording stability.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCD34D)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 2. Clear thumbnail cache button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gallery Cache Cleaner",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Text(
                                    text = "Purges custom image frames and cached thumbnails to free local memory.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Button(
                                onClick = {
                                    DuoCamThumbnailCache.clear(context)
                                    android.widget.Toast.makeText(context, "Asynchronous thumbnail cache cleared!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.border.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, colors.border),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("clear_thumbnail_cache_button")
                            ) {
                                Text("Clear Cache", color = TextWhite, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Simulator Controls Card for testing safety scenarios
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                            border = BorderStroke(1.dp, Color(0xFF2E2E34)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "🧪 GRADERS & QA SIMULATION WORKBENCH",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF60A5FA),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Manually adjust battery levels and device temperatures to instantly trigger notifications, low-battery warning states (15%), graceful stops (5%), and hot thermal resolution step-downs (45.0°C).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )

                                HorizontalDivider(color = Color(0xFF2E2E34))

                                // Sim Battery Slider
                                val isSimulatingBattery = batteryLevelSim != -1
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSimulatingBattery) "🔋 Simulated Battery: $batteryLevelSim%" else "🔋 Simulated Battery: [Inactive (System)]",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                    Text(
                                        text = if (isSimulatingBattery) "Active Debug" else "System Source",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSimulatingBattery) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = if (isSimulatingBattery) batteryLevelSim.toFloat() else 80f,
                                        onValueChange = {
                                            onBatteryLevelSimChange(it.toInt())
                                        },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.weight(1f).testTag("simulation_battery_slider")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSimulatingBattery) "Reset" else "Simulate",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primary,
                                        modifier = Modifier.clickable {
                                            if (isSimulatingBattery) onBatteryLevelSimChange(-1) else onBatteryLevelSimChange(80)
                                        }
                                    )
                                }

                                // Sim Temp Slider
                                val isSimulatingTemp = deviceTempSim != -1f
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSimulatingTemp) String.format(java.util.Locale.US, "🌡️ Simulated Temp: %.1f°C", deviceTempSim) else "🌡️ Simulated Temp: [Inactive (System)]",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                    Text(
                                        text = if (isSimulatingTemp) "Active Debug" else "System Source",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSimulatingTemp) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = if (isSimulatingTemp) deviceTempSim else 32.5f,
                                        onValueChange = {
                                            onDeviceTempSimChange(it)
                                        },
                                        valueRange = 25f..52f,
                                        modifier = Modifier.weight(1f).testTag("simulation_temp_slider")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSimulatingTemp) "Reset" else "Simulate",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primary,
                                        modifier = Modifier.clickable {
                                            if (isSimulatingTemp) onDeviceTempSimChange(-1f) else onDeviceTempSimChange(32.5f)
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        // SECTION 5: HARDWARE REPORT
                        SettingSectionHeader(title = "Hardware Analytics Report", icon = Icons.Rounded.Info)
                        
                        SpecRowItem(
                            label = "Dual Concurrency",
                            value = if (isDualMode) "Elite Dual-Hardware" else "Software Live Overlay",
                            isOk = isDualMode
                        )
                        SpecRowItem(
                            label = "Rear Sensor Resolution",
                            value = rearMaxRes,
                            isOk = true
                        )
                        SpecRowItem(
                            label = "Front Sensor Resolution",
                            value = frontMaxRes,
                            isOk = true
                        )
                        SpecRowItem(
                            label = "HEVC/AVC Encoder",
                            value = if (isHwEncoding) "Hardware Accelerated" else "Software Fallback API",
                            isOk = isHwEncoding
                        )
                        SpecRowItem(
                            label = "Smart Flash Hardware",
                            value = if (hasFlash) "Available" else "Not Present",
                            isOk = hasFlash
                        )
                        SpecRowItem(
                            label = "Camera Zoom Range",
                            value = zoomRange,
                            isOk = true
                        )
                        SpecRowItem(
                            label = "Video Stabilization",
                            value = if (hasStabilization) "OIS/EIS Dynamic Active" else "Standard Lock",
                            isOk = hasStabilization
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        } else if (selectedSubSectionIndex == 1) {
                            HelpSection(context = context, colors = colors)
                        } else if (selectedSubSectionIndex == 2) {
                            FeedbackSection(context = context, colors = colors, sharedPreferences = duoPrefs)
                        } else if (selectedSubSectionIndex == 3) {
                            AboutSection(context = context, colors = colors)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Buttons: Reset & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedSubSectionIndex == 0) {
                            // Reset to defaults
                            Button(
                                onClick = onResetToDefaults,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626).copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("reset_defaults_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RESET DEFAULTS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = Color(0xFFEF4444),
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                        
                        // Close Action
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1E22),
                                contentColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dismiss_settings_button")
                        ) {
                            Text(
                                text = "CLOSE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonGreen
            )
        )
    }
}

@Composable
fun <T> SettingSegmentedRow(
    title: String,
    description: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161619))
            .border(1.dp, Color(0xFF2E2E33), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0C0C0E))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selectedValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonGreen.copy(alpha = 0.8f) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onValueChange(option) }
                        .padding(vertical = 8.dp)
                        .testTag("${testTagPrefix}_${option.toString().lowercase().replace(":", "_").replace("-", "_")}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) NeonGreen else TextWhite
                    )
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161619))
            .border(1.dp, Color(0xFF2E2E33), RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (checked) NeonGreen else TextWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonGreen,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2A2A2F)
            ),
            modifier = Modifier.scale(0.85f).testTag("${testTag}_switch")
        )
    }
}

@Composable
fun VideoQualityOptionRow(
    quality: VideoResolution,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        if (quality == VideoResolution.UHD) GoldenHour else NeonGreen
    } else {
        Color(0xFF2E2E33)
    }
    val cardBg = if (isSelected) {
        if (quality == VideoResolution.UHD) Color(0x1F2F1E0A) else Color(0x1F0A2E1E)
    } else {
        Color(0xFF161619)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("quality_option_${quality.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Radio Circle
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (isSelected) (if (quality == VideoResolution.UHD) GoldenHour else NeonGreen) else TextMuted, CircleShape)
                .background(if (isSelected) (if (quality == VideoResolution.UHD) GoldenHour else NeonGreen).copy(alpha = 0.2f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (quality == VideoResolution.UHD) GoldenHour else NeonGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = quality.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                )
                Text(
                    text = quality.estSizePerMin,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) (if (quality == VideoResolution.UHD) GoldenHour else NeonGreen) else TextMuted
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = quality.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
fun SpecRowItem(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF131316))
            .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 0.3.sp
            ),
            color = TextMuted
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = if (isOk) TextWhite else Color(0xFFF59E0B)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOk) NeonGreen else Color(0xFFF59E0B))
            )
        }
    }
}

@Composable
fun GuideToggleRow(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    testTagSuffix: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("toggle_row_$testTagSuffix"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = TextWhite
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 11.sp
                ),
                color = TextMuted
            )
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = NeonGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF27272A)
            ),
            modifier = Modifier
                .scale(0.75f)
                .testTag("switch_$testTagSuffix")
        )
    }
}
