package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.camera.view.PreviewView
import com.example.camera.CameraCapabilities
import com.example.camera.CameraCapabilityManager
import com.example.camera.CameraCapabilityState
import com.example.camera.CameraController
import com.example.camera.CaptureMode
import com.example.camera.RecordingUiState
import com.example.db.Script
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var cameraPermissionGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.CAMERA))
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
    }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Capture.name) }
    var selectedScript by remember { mutableStateOf<Script?>(null) }

    if (!cameraPermissionGranted) {
        PermissionExplanationScreen(
            onRequestPermissions = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
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
        return
    }

    val tab = AppTab.valueOf(selectedTab)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(
                    selected = tab == AppTab.Capture,
                    onClick = { selectedTab = AppTab.Capture.name },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Capture") },
                    label = { Text("Capture") },
                )
                NavigationBarItem(
                    selected = tab == AppTab.Scripts,
                    onClick = { selectedTab = AppTab.Scripts.name },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Scripts") },
                    label = { Text("Scripts") },
                )
                NavigationBarItem(
                    selected = tab == AppTab.Gallery,
                    onClick = { selectedTab = AppTab.Gallery.name },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery") },
                    label = { Text("Gallery") },
                )
            }
        },
    ) { paddingValues ->
        when (tab) {
            AppTab.Capture -> CaptureScreen(
                modifier = Modifier.padding(paddingValues),
                selectedScript = selectedScript,
                onOpenScripts = { selectedTab = AppTab.Scripts.name },
                onOpenGallery = { selectedTab = AppTab.Gallery.name },
            )

            AppTab.Scripts -> ScriptsScreen(
                modifier = Modifier.padding(paddingValues),
                onUseScript = {
                    selectedScript = it
                    selectedTab = AppTab.Capture.name
                },
            )

            AppTab.Gallery -> GalleryScreen(modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun PermissionExplanationScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                text = "Record your perspective and reaction together.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "DuoCam uses the camera only when you open Capture. " +
                    "Dual-camera recording is enabled only when Android reports that the device supports it.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Allow camera")
            }
            if (onOpenSettings != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenSettings) {
                    Text("Open app settings")
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
        )
    }
    val microphoneLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            controller.startRecording(audioRequested = true)
        } else {
            audioEnabled = false
            controller.startRecording(audioRequested = false)
        }
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
    val canUseDual = capabilities?.supportsConcurrent == true
    val capabilityText = when (capabilities?.state) {
        null -> "Checking camera support…"
        CameraCapabilityState.DualCameraSupported -> "Dual-camera recording is supported on this device."
        CameraCapabilityState.SingleCameraSupported -> "This device supports single-camera recording."
        CameraCapabilityState.PermissionRequired -> "Camera permission is required."
        CameraCapabilityState.CameraUnavailable -> "No usable camera is available."
        CameraCapabilityState.InitializationError -> capabilities?.errorMessage
            ?: "Camera support could not be checked."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("DuoCam", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = capabilityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (actualMode == CaptureMode.DUAL) "DUAL" else "SINGLE",
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
                .aspectRatio(9f / 16f)
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
                    text = "●  REC  ${formatDuration(recordingState.durationMs)}",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = requestedCaptureMode == CaptureMode.SINGLE,
                onClick = { requestedMode = CaptureMode.SINGLE.name },
                enabled = !isBusy,
                label = { Text("Single camera") },
            )
            FilterChip(
                selected = requestedCaptureMode == CaptureMode.DUAL,
                onClick = { requestedMode = CaptureMode.DUAL.name },
                enabled = canUseDual && !isBusy,
                label = { Text("Dual camera") },
            )
        }
        if (modeMessage != null) {
            Text(
                text = modeMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Record audio")
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
                    Text(if (teleprompterVisible) "Hide script" else "Show script")
                }
            } else {
                TextButton(onClick = onOpenScripts, enabled = !isBusy) {
                    Text("Choose script")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (recordingState.isRecording) {
                    controller.stopRecording()
                } else if (!recordingState.isFinalizing) {
                    val hasMic = hasPermission(context, Manifest.permission.RECORD_AUDIO)
                    if (audioEnabled && !hasMic) {
                        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        controller.startRecording(audioEnabled)
                    }
                }
            },
            enabled = capabilities != null &&
                capabilities?.state != CameraCapabilityState.CameraUnavailable &&
                capabilities?.state != CameraCapabilityState.InitializationError &&
                !recordingState.isFinalizing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (recordingState.isFinalizing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Saving…")
            } else {
                Icon(
                    imageVector = if (recordingState.isRecording) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.Videocam
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (recordingState.isRecording) "Stop recording" else "Start recording")
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
                    Text("Recording saved to your local gallery.")
                    TextButton(onClick = onOpenGallery) { Text("Open gallery") }
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
    LaunchedEffect(scrolling, speed, script.id) {
        while (scrolling) {
            scrollState.scrollBy(speed)
            delay(24L)
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
                    Text("Speed ${speed}x", color = Color.White)
                }
                TextButton(onClick = { scrolling = !scrolling }) {
                    Text(if (scrolling) "Pause" else "Scroll", color = Color.White)
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
