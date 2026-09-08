package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraCapabilities
import com.example.camera.CameraCapabilityManager
import com.example.camera.CameraCapabilityState
import com.example.camera.CameraController
import com.example.camera.CaptureMode
import com.example.camera.RecordingUiState
import com.example.db.Script
import com.example.db.ScriptDatabase
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DuoCamApp()
            }
        }
    }
}

private enum class AppTab {
    Capture,
    Scripts,
    Gallery,
}

@Composable
private fun DuoCamApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraPermissionGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.CAMERA))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraPermissionGranted = hasPermission(
                    context,
                    Manifest.permission.CAMERA,
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
    }

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Capture.name) }
    var selectedScriptId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedScript by remember { mutableStateOf<Script?>(null) }
    var isRecordingBusy by remember { mutableStateOf(false) }

    val database = remember(context) { ScriptDatabase.getDatabase(context) }

    LaunchedEffect(selectedScriptId) {
        selectedScript = selectedScriptId?.let { id ->
            withContext(Dispatchers.IO) {
                database.scriptDao().getScriptById(id)
            }
        }
    }

    val tab = AppTab.valueOf(selectedTab)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == AppTab.Capture,
                    onClick = { selectedTab = AppTab.Capture.name },
                    icon = {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = stringResource(R.string.tab_capture),
                        )
                    },
                    label = { Text(stringResource(R.string.tab_capture)) },
                )
                NavigationBarItem(
                    selected = tab == AppTab.Scripts,
                    onClick = { selectedTab = AppTab.Scripts.name },
                    enabled = !isRecordingBusy,
                    icon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = stringResource(R.string.tab_scripts),
                        )
                    },
                    label = { Text(stringResource(R.string.tab_scripts)) },
                )
                NavigationBarItem(
                    selected = tab == AppTab.Gallery,
                    onClick = { selectedTab = AppTab.Gallery.name },
                    enabled = !isRecordingBusy,
                    icon = {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.tab_gallery),
                        )
                    },
                    label = { Text(stringResource(R.string.tab_gallery)) },
                )
            }
        },
    ) { paddingValues ->
        val screenModifier = Modifier
            .padding(paddingValues)
            .consumeWindowInsets(paddingValues)

        when (tab) {
            AppTab.Capture -> {
                if (!cameraPermissionGranted) {
                    PermissionExplanationScreen(
                        modifier = screenModifier,
                        onRequestPermissions = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    "package:${context.packageName}".toUri(),
                                ),
                            )
                        },
                    )
                } else {
                    CaptureScreen(
                        modifier = screenModifier,
                        selectedScript = selectedScript,
                        onOpenScripts = { selectedTab = AppTab.Scripts.name },
                        onOpenGallery = { selectedTab = AppTab.Gallery.name },
                        onRecordingBusyChanged = { isRecordingBusy = it },
                    )
                }
            }

            AppTab.Scripts -> ScriptsScreen(
                modifier = screenModifier,
                onUseScript = { script ->
                    selectedScriptId = script.id
                    selectedTab = AppTab.Capture.name
                },
            )

            AppTab.Gallery -> GalleryScreen(modifier = screenModifier)
        }
    }
}

@Composable
fun PermissionExplanationScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.permission_description),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.permission_allow_camera))
            }
            if (onOpenSettings != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.permission_open_settings))
                }
            }
        }
    }
}

@Composable
private fun CaptureScreen(
    modifier: Modifier,
    selectedScript: Script?,
    onOpenScripts: () -> Unit,
    onOpenGallery: () -> Unit,
    onRecordingBusyChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var capabilities by remember { mutableStateOf<CameraCapabilities?>(null) }
    var requestedMode by rememberSaveable { mutableStateOf(CaptureMode.SINGLE.name) }
    var actualMode by remember { mutableStateOf(CaptureMode.SINGLE) }
    var modeMessage by remember { mutableStateOf<String?>(null) }
    var recordingState by remember { mutableStateOf(RecordingUiState()) }
    var audioEnabled by rememberSaveable { mutableStateOf(true) }
    var teleprompterVisible by rememberSaveable { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }

    val requestedCaptureMode = CaptureMode.valueOf(requestedMode)
    val controller = remember(previewView, lifecycleOwner) {
        CameraController(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            onRecordingStateChanged = { recordingState = it },
            onModeChanged = { mode, message ->
                actualMode = mode
                modeMessage = message
            },
            onCameraReadyChanged = { ready ->
                cameraReady = ready
            },
        )
    }

    val recordingPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val legacyStorageRequired = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
        val legacyStorageGranted =
            !legacyStorageRequired ||
                results[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true ||
                hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (!legacyStorageGranted) {
            recordingState = RecordingUiState(
                errorMessage = "Storage permission is required to save video on this Android version.",
            )
            return@rememberLauncherForActivityResult
        }

        val microphoneGranted =
            !audioEnabled ||
                results[Manifest.permission.RECORD_AUDIO] == true ||
                hasPermission(context, Manifest.permission.RECORD_AUDIO)

        if (audioEnabled && !microphoneGranted) {
            audioEnabled = false
        }

        controller.startRecording(
            audioRequested = audioEnabled && microphoneGranted,
        )
    }

    LaunchedEffect(Unit) {
        CameraCapabilityManager.inspect(context) { capabilities = it }
    }
    LaunchedEffect(capabilities?.state, requestedCaptureMode) {
        capabilities?.let { controller.bind(it, requestedCaptureMode) }
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    val isBusy = recordingState.isRecording || recordingState.isFinalizing
    LaunchedEffect(isBusy) {
        onRecordingBusyChanged(isBusy)
    }

    val canUseDual = capabilities?.supportsConcurrent == true
    val capabilityText = when (capabilities?.state) {
        null -> stringResource(R.string.capture_checking)
        CameraCapabilityState.DualCameraSupported -> stringResource(R.string.capture_dual_supported)
        CameraCapabilityState.SingleCameraSupported -> stringResource(R.string.capture_single_supported)
        CameraCapabilityState.PermissionRequired -> stringResource(R.string.capture_permission_required)
        CameraCapabilityState.CameraUnavailable -> stringResource(R.string.capture_unavailable)
        CameraCapabilityState.InitializationError -> capabilities?.errorMessage
            ?: stringResource(R.string.capture_init_error)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    stringResource(R.string.capture_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = capabilityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (actualMode == CaptureMode.DUAL) {
                    stringResource(R.string.capture_mode_dual)
                } else {
                    stringResource(R.string.capture_mode_single)
                },
                color = if (actualMode == CaptureMode.DUAL) {
                    Color(0xFF66BB6A)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 180.dp)
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
            if (selectedScript != null && teleprompterVisible) {
                TeleprompterOverlay(
                    script = selectedScript,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            if (recordingState.isRecording) {
                Text(
                    text = stringResource(
                        R.string.capture_rec_indicator,
                        formatDuration(recordingState.durationMs),
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = requestedCaptureMode == CaptureMode.SINGLE,
                    onClick = { requestedMode = CaptureMode.SINGLE.name },
                    enabled = !isBusy,
                    label = { Text(stringResource(R.string.capture_mode_chip_single)) },
                )
                FilterChip(
                    selected = requestedCaptureMode == CaptureMode.DUAL,
                    onClick = { requestedMode = CaptureMode.DUAL.name },
                    enabled = canUseDual && !isBusy,
                    label = { Text(stringResource(R.string.capture_mode_chip_dual)) },
                )
            }
            if (modeMessage != null) {
                Text(
                    text = modeMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.capture_audio_label))
                    Switch(
                        checked = audioEnabled,
                        onCheckedChange = { audioEnabled = it },
                        enabled = !isBusy,
                    )
                }
                if (selectedScript != null) {
                    TextButton(
                        onClick = { teleprompterVisible = !teleprompterVisible },
                        enabled = !isBusy,
                    ) {
                        Text(
                            stringResource(
                                if (teleprompterVisible) R.string.capture_hide_script
                                else R.string.capture_show_script,
                            ),
                        )
                    }
                } else {
                    TextButton(onClick = onOpenScripts, enabled = !isBusy) {
                        Text(stringResource(R.string.capture_choose_script))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (recordingState.isRecording) {
                        controller.stopRecording()
                    } else if (!recordingState.isFinalizing) {
                        val requiredPermissions = buildList {
                            if (audioEnabled && !hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
                                add(Manifest.permission.RECORD_AUDIO)
                            }
                            if (
                                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                !hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            ) {
                                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }

                        if (requiredPermissions.isNotEmpty()) {
                            recordingPermissionLauncher.launch(requiredPermissions.toTypedArray())
                        } else {
                            controller.startRecording(audioRequested = audioEnabled)
                        }
                    }
                },
                enabled = capabilities != null &&
                    capabilities?.state != CameraCapabilityState.CameraUnavailable &&
                    capabilities?.state != CameraCapabilityState.InitializationError &&
                    cameraReady &&
                    !recordingState.isFinalizing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (recordingState.isFinalizing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.saving))
                } else {
                    Icon(
                        imageVector = if (recordingState.isRecording) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Videocam
                        },
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (recordingState.isRecording) R.string.stop_recording
                            else R.string.start_recording,
                        ),
                    )
                }
            }

            recordingState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (recordingState.savedUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.capture_saved_message),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onOpenGallery) {
                            Text(stringResource(R.string.capture_open_gallery))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeleprompterOverlay(script: Script, modifier: Modifier) {
    val scrollState = rememberScrollState()
    var scrolling by rememberSaveable(script.id) { mutableStateOf(false) }
    var speed by rememberSaveable(script.id) { mutableStateOf(1f) }

    LaunchedEffect(script.id) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(scrolling, speed, script.id) {
        while (scrolling && scrollState.value < scrollState.maxValue) {
            val consumed = scrollState.scrollBy(speed)
            if (consumed == 0f && scrollState.value >= scrollState.maxValue) break
            delay(24L)
        }
        if (scrollState.value >= scrollState.maxValue) {
            scrolling = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.72f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = script.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = script.content,
                modifier = Modifier
                    .height(120.dp)
                    .verticalScroll(scrollState)
                    .padding(top = 8.dp),
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 28.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { speed = (speed + 0.5f).coerceAtMost(5f) }) {
                    Text(
                        stringResource(R.string.teleprompter_speed, speed.toString()),
                        color = Color.White,
                    )
                }
                TextButton(onClick = { scrolling = !scrolling }) {
                    Text(
                        stringResource(
                            if (scrolling) R.string.teleprompter_pause
                            else R.string.teleprompter_scroll,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
