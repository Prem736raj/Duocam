package com.example

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.Composable

@Composable
fun CameraSettingsOverlayBlock(
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
    onResolutionSelectedInternal: (VideoResolution) -> Unit,
    
    supports4K: Boolean,
    selectedFrameRate: Int,
    onFrameRateSelectedInternal: (Int) -> Unit,
    
    useHevcCompression: Boolean,
    onHevcToggleInternal: (Boolean) -> Unit,
    isHevcSupported: Boolean,
    
    audioNoiseReductionEnabled: Boolean,
    onAudioNoiseReductionToggleInternal: (Boolean) -> Unit,
    
    isVoiceControlEnabled: Boolean,
    onVoiceControlToggleInternal: (Boolean) -> Unit,
    
    isStabilizationActive: Boolean,
    onStabilizationToggleInternal: (Boolean) -> Unit,
    
    stabilizationStrength: String,
    onStabilizationStrengthChangeInternal: (String) -> Unit,
    
    selectedLayout: DualCameraLayout,
    onLayoutSelectedInternal: (DualCameraLayout) -> Unit,
    
    selectedAspectRatio: RecordingAspectRatio,
    onAspectRatioSelectedInternal: (RecordingAspectRatio) -> Unit,
    
    countdownSetting: Int,
    onCountdownSettingSelectedInternal: (Int) -> Unit,
    
    isGridEnabled: Boolean,
    onGridEnabledToggleInternal: (Boolean) -> Unit,
    
    mirrorFrontCamera: Boolean,
    onMirrorFrontCameraToggleInternal: (Boolean) -> Unit,
    
    defaultAudioSource: String,
    onDefaultAudioSourceSelectedInternal: (String) -> Unit,
    
    defaultSaveLocation: String,
    onDefaultSaveLocationSelectedInternal: (String) -> Unit,
    
    fileNamingFormat: String,
    onFileNamingFormatSelectedInternal: (String) -> Unit,
    
    autoDeleteDays: Int,
    onAutoDeleteDaysSelectedInternal: (Int) -> Unit,
    
    onResetToDefaults: () -> Unit,
    
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    
    accentColor: String,
    onAccentColorChange: (String) -> Unit,
    
    uiDensity: String,
    onUiDensityChange: (String) -> Unit,
    
    notifyRecordingCompletion: Boolean,
    onNotifyRecordingCompletionSelectedInternal: (Boolean) -> Unit,
    
    notifyBackupCompletion: Boolean,
    onNotifyBackupCompletionSelectedInternal: (Boolean) -> Unit,
    
    notifyLowStorage: Boolean,
    onNotifyLowStorageSelectedInternal: (Boolean) -> Unit,
    
    notifyRecordingTipOfDay: Boolean,
    onNotifyRecordingTipOfDaySelectedInternal: (Boolean) -> Unit,
    
    autoEnableDnd: Boolean,
    onAutoEnableDndSelectedInternal: (Boolean) -> Unit,
    
    isPro: Boolean,
    onProChange: (Boolean) -> Unit,
    triggerGoPro: (String) -> Unit,
    
    isLiteMode: Boolean,
    onLiteModeChange: (Boolean) -> Unit,
    batteryLevelSim: Int,
    onBatteryLevelSimChange: (Int) -> Unit,
    deviceTempSim: Float,
    onDeviceTempSimChange: (Float) -> Unit,
    
    // External states to update inside Settings callbacks
    sharedPreferences: SharedPreferences,
    context: Context,
    onActivateLayoutPreview: (Boolean, Int) -> Unit
) {
    SettingsOverlay(
        isVisible = isVisible,
        onClose = onClose,
        isDualMode = isDualMode,
        rearMaxRes = rearMaxRes,
        frontMaxRes = frontMaxRes,
        isHwEncoding = isHwEncoding,
        hasFlash = hasFlash,
        zoomRange = zoomRange,
        hasStabilization = hasStabilization,
        selectedResolution = selectedResolution,
        onResolutionSelected = { res ->
            onResolutionSelectedInternal(res)
            if (!isPro && res != VideoResolution.HD) {
                Toast.makeText(context, "Recording in ${res.name} (Free Preview). Upgrade to remove watermark!", Toast.LENGTH_LONG).show()
            }
        },
        supports4K = supports4K,
        selectedFrameRate = selectedFrameRate,
        onFrameRateSelected = { onFrameRateSelectedInternal(it) },
        useHevcCompression = useHevcCompression,
        onHevcToggle = { onHevcToggleInternal(it) },
        isHevcSupported = isHevcSupported,
        audioNoiseReductionEnabled = audioNoiseReductionEnabled,
        onAudioNoiseReductionToggle = { onAudioNoiseReductionToggleInternal(it) },
        isVoiceControlEnabled = isVoiceControlEnabled,
        onVoiceControlToggle = { onVoiceControlToggleInternal(it) },
        isStabilizationActive = isStabilizationActive,
        onStabilizationToggle = { onStabilizationToggleInternal(it) },
        stabilizationStrength = stabilizationStrength,
        onStabilizationStrengthChange = { onStabilizationStrengthChangeInternal(it) },
        selectedLayout = selectedLayout,
        onLayoutSelected = { layout ->
            if (!isPro && layout != DualCameraLayout.PIP) {
                onLayoutSelectedInternal(layout)
                onActivateLayoutPreview(true, 3)
                onClose()
                Toast.makeText(context, "3-Second Layout Preview Active!", Toast.LENGTH_SHORT).show()
            } else {
                onLayoutSelectedInternal(layout)
                sharedPreferences.edit().putString("selected_layout", layout.name).apply()
            }
        },
        selectedAspectRatio = selectedAspectRatio,
        onAspectRatioSelected = { ratio ->
            onAspectRatioSelectedInternal(ratio)
            sharedPreferences.edit().putString("selected_aspect_ratio", ratio.name).apply()
        },
        countdownSetting = countdownSetting,
        onCountdownSettingSelected = { seconds ->
            onCountdownSettingSelectedInternal(seconds)
            sharedPreferences.edit().putInt("countdown_setting", seconds).apply()
        },
        isGridEnabled = isGridEnabled,
        onGridEnabledToggle = { enabled ->
            onGridEnabledToggleInternal(enabled)
            sharedPreferences.edit().putBoolean("is_grid_enabled", enabled).apply()
        },
        mirrorFrontCamera = mirrorFrontCamera,
        onMirrorFrontCameraToggle = { onMirrorFrontCameraToggleInternal(it) },
        defaultAudioSource = defaultAudioSource,
        onDefaultAudioSourceSelected = { onDefaultAudioSourceSelectedInternal(it) },
        defaultSaveLocation = defaultSaveLocation,
        onDefaultSaveLocationSelected = { onDefaultSaveLocationSelectedInternal(it) },
        fileNamingFormat = fileNamingFormat,
        onFileNamingFormatSelected = { onFileNamingFormatSelectedInternal(it) },
        autoDeleteDays = autoDeleteDays,
        onAutoDeleteDaysSelected = { onAutoDeleteDaysSelectedInternal(it) },
        onResetToDefaults = onResetToDefaults,
        themeMode = themeMode,
        onThemeModeSelected = onThemeModeChange,
        accentColor = accentColor,
        onAccentColorSelected = onAccentColorChange,
        uiDensity = uiDensity,
        onUiDensitySelected = onUiDensityChange,
        notifyRecordingCompletion = notifyRecordingCompletion,
        onNotifyRecordingCompletionSelected = {
            onNotifyRecordingCompletionSelectedInternal(it)
            sharedPreferences.edit().putBoolean("notify_recording_completion", it).apply()
        },
        notifyBackupCompletion = notifyBackupCompletion,
        onNotifyBackupCompletionSelected = { enabled ->
            if (enabled && !isPro) {
                triggerGoPro("Cloud Backup synchronization")
            } else {
                onNotifyBackupCompletionSelectedInternal(enabled)
                sharedPreferences.edit().putBoolean("notify_backup_completion", enabled).apply()
            }
        },
        notifyLowStorage = notifyLowStorage,
        onNotifyLowStorageSelected = {
            onNotifyLowStorageSelectedInternal(it)
            sharedPreferences.edit().putBoolean("notify_low_storage", it).apply()
        },
        notifyRecordingTipOfDay = notifyRecordingTipOfDay,
        onNotifyRecordingTipOfDaySelected = {
            onNotifyRecordingTipOfDaySelectedInternal(it)
            sharedPreferences.edit().putBoolean("notify_recording_tip_of_the_day", it).apply()
        },
        autoEnableDnd = autoEnableDnd,
        onAutoEnableDndSelected = {
            onAutoEnableDndSelectedInternal(it)
            sharedPreferences.edit().putBoolean("auto_enable_dnd", it).apply()
        },
        isPro = isPro,
        onProChange = onProChange,
        isLiteMode = isLiteMode,
        onLiteModeChange = onLiteModeChange,
        batteryLevelSim = batteryLevelSim,
        onBatteryLevelSimChange = onBatteryLevelSimChange,
        deviceTempSim = deviceTempSim,
        onDeviceTempSimChange = onDeviceTempSimChange
    )
}
