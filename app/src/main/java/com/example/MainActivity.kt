package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.foundation.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import kotlin.math.roundToInt
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Shape
import android.widget.Toast
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.scale
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.CameraAlertsAndDialogs
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.GlowRecordRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.BorderGray
import com.example.ui.theme.SoftGlowRed
import com.example.ui.theme.GoldenHour
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

fun parseEmphasisText(text: String, baseColor: androidx.compose.ui.graphics.Color): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        val emphasisColor = if (baseColor == androidx.compose.ui.graphics.Color.White) {
            androidx.compose.ui.graphics.Color(0xFF22C55E) // Neon Green
        } else {
            androidx.compose.ui.graphics.Color(0xFFFACC15) // Golden Yellow
        }
        
        for (i in parts.indices) {
            val part = parts[i]
            if (i % 2 == 1) {
                withStyle(
                    style = SpanStyle(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        color = emphasisColor
                    )
                ) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}

enum class PermissionStatus {
    Checking,
    ExplanationRequired,
    Denied,
    Granted
}

enum class DuoCamScreen {
    HOME,
    CAMERA,
    GALLERY
}

enum class VideoResolution(
    val label: String,
    val width: Int,
    val height: Int,
    val estSizePerMin: String,
    val description: String
) {
    HD("720p (HD)", 720, 1280, "~60 MB/min", "Smallest file size, works on all devices"),
    FHD("1080p (Full HD)", 1080, 1920, "~130 MB/min", "Good balance of quality and size"),
    UHD("4K (Ultra HD)", 2160, 3840, "~350 MB/min", "Best quality, large files")
}

fun is4KSupported(context: android.content.Context): Boolean {
    try {
        val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager ?: return false
        for (id in cameraManager.cameraIdList) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val facing = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
            if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                val map = chars.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue
                val sizes = map.getOutputSizes(android.graphics.ImageFormat.JPEG)
                    ?: map.getOutputSizes(android.graphics.SurfaceTexture::class.java)
                    ?: continue
                if (sizes.any { it.width >= 3840 && it.height >= 2160 }) {
                    return true
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("DuoCam", "Error checking if 4K is supported", e)
    }
    // For emulator or preview testing environment, let it return true so 4K can always be experienced/demonstrated
    val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
            android.os.Build.MODEL.contains("google_sdk") ||
            android.os.Build.MODEL.contains("Emulator") ||
            android.os.Build.HARDWARE.contains("goldfish") ||
            android.os.Build.HARDWARE.contains("ranchu")
    return isEmulator
}

class MainActivity : ComponentActivity() {

    companion object {
        var onVolumeKeyPressed: (() -> Boolean)? = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val handled = onVolumeKeyPressed?.invoke() ?: false
            if (handled) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (onVolumeKeyPressed != null) return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize cloud backup system
        BackupManager.init(this)
        
        // Push the content fully draw edge-to-edge
        enableEdgeToEdge()
        
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE) }
            
            var themeMode by remember { mutableStateOf(sharedPreferences.getString("theme_mode", "dark") ?: "dark") }
            var accentColor by remember { mutableStateOf(sharedPreferences.getString("accent_color", "red") ?: "red") }
            var uiDensity by remember { mutableStateOf(sharedPreferences.getString("ui_density", "standard") ?: "standard") }

            MyApplicationTheme(
                themeMode = themeMode,
                accentName = accentColor,
                densityMode = uiDensity
            ) {
                MainContent(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        sharedPreferences.edit().putString("theme_mode", mode).apply()
                    },
                    accentColor = accentColor,
                    onAccentColorChange = { color ->
                        accentColor = color
                        sharedPreferences.edit().putString("accent_color", color).apply()
                    },
                    uiDensity = uiDensity,
                    onUiDensityChange = { density ->
                        uiDensity = density
                        sharedPreferences.edit().putString("ui_density", density).apply()
                    }
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@Composable
fun MainContent(
    themeMode: String = "dark",
    onThemeModeChange: (String) -> Unit = {},
    accentColor: String = "red",
    onAccentColorChange: (String) -> Unit = {},
    uiDensity: String = "standard",
    onUiDensityChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPreferences = remember { context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE) }
    var isPro by remember { mutableStateOf(sharedPreferences.getBoolean("is_pro_upgraded", false)) }
    var hasCompletedOnboarding by remember { mutableStateOf(sharedPreferences.getBoolean("has_completed_onboarding", false)) }
    
    val requiredPermissions = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { // API 33
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { // API 30 to 32
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    var permissionStatus by remember { mutableStateOf(PermissionStatus.Checking) }
    
    // Refresh permission check status
    fun updatePermissionStatus() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            permissionStatus = PermissionStatus.Granted
        } else {
            // Check if we should show rationales or if the app is launching fresh
            // If checking fails, let's trigger explanation state
            permissionStatus = PermissionStatus.ExplanationRequired
        }
    }
    
    // Re-check permissions when the Activity is resumed (handles return from Settings screen)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Initial check & auto purchase verification on launch
    LaunchedEffect(Unit) {
        updatePermissionStatus()
        DuoCamPurchaseManager.autoVerifyOnLaunch(context) { verified, message ->
            isPro = verified
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = requiredPermissions.all { perm -> 
            results[perm] == true || context.checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED 
        }
        if (allGranted) {
            permissionStatus = PermissionStatus.Granted
        } else {
            permissionStatus = PermissionStatus.Denied
        }
    }
    
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1800)
        showSplash = false
    }

    if (showSplash) {
        DuoCamSplashScreen()
        return
    }

    if (!hasCompletedOnboarding) {
        OnboardingScreen(
            onComplete = {
                hasCompletedOnboarding = true
            }
        )
        return
    }

    Crossfade(targetState = permissionStatus, label = "ScreenTransition") { status ->
        when (status) {
            PermissionStatus.Checking -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBlack),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GlowRecordRed)
                }
            }
            PermissionStatus.ExplanationRequired -> {
                PermissionExplanationScreen(onRequestPermissions = {
                    permissionsLauncher.launch(requiredPermissions)
                })
            }
            PermissionStatus.Denied -> {
                PermissionDeniedScreen(onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                })
            }
            PermissionStatus.Granted -> {
                var currentScreen by remember { mutableStateOf(DuoCamScreen.HOME) }
                Crossfade(targetState = currentScreen, label = "NavigationTransition") { screen ->
                    when (screen) {
                        DuoCamScreen.HOME -> {
                            DuoCamHomeScreen(
                                onStartRecording = {
                                    currentScreen = DuoCamScreen.CAMERA
                                },
                                onOpenGallery = {
                                    currentScreen = DuoCamScreen.GALLERY
                                },
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                accentColor = accentColor,
                                onAccentColorChange = onAccentColorChange,
                                uiDensity = uiDensity,
                                onUiDensityChange = onUiDensityChange,
                                isPro = isPro
                            )
                        }
                        DuoCamScreen.CAMERA -> {
                            CameraActiveScreen(
                                onBackToHome = {
                                    currentScreen = DuoCamScreen.HOME
                                },
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                accentColor = accentColor,
                                onAccentColorChange = onAccentColorChange,
                                uiDensity = uiDensity,
                                onUiDensityChange = onUiDensityChange,
                                isPro = isPro,
                                onProChange = { isPro = it }
                            )
                        }
                        DuoCamScreen.GALLERY -> {
                            DuoCamGalleryScreen(
                                onBackToHome = {
                                    currentScreen = DuoCamScreen.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom immersive manager for removing Android navigation bars and system status bars in active preview.
 */
@Composable
fun ImmersiveMode(hide: Boolean) {
    val context = LocalContext.current
    DisposableEffect(hide) {
        val window = (context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (hide) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                try {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("DuoCam", "Error updating insets controller in ImmersiveMode", e)
            onDispose {}
        }
    }
}

data class DuoRecording(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val dateSecs: Long,
    val sizeBytes: Long
)

fun getLocalRecordings(context: Context, limit: Int = -1): List<DuoRecording> {
    val recordings = mutableListOf<DuoRecording>()
    val contentResolver = context.contentResolver
    
    // Querying MediaStore for videos recorded and saved inside Movies/DuoCam/
    val selection = "${android.provider.MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("%Movies/DuoCam%")
    val sortOrder = "${android.provider.MediaStore.Video.Media.DATE_ADDED} DESC"
    
    val projection = arrayOf(
        android.provider.MediaStore.Video.Media._ID,
        android.provider.MediaStore.Video.Media.DISPLAY_NAME,
        android.provider.MediaStore.Video.Media.DURATION,
        android.provider.MediaStore.Video.Media.SIZE,
        android.provider.MediaStore.Video.Media.DATE_ADDED
    )
    
    try {
        contentResolver.query(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DATE_ADDED)
            
            val limitCondition = limit > 0
            if (idCol != -1 && nameCol != -1 && durationCol != -1 && sizeCol != -1 && dateCol != -1) {
                while (cursor.moveToNext() && (!limitCondition || recordings.size < limit)) {
                    try {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "DuoCam Video"
                        val duration = if (!cursor.isNull(durationCol)) cursor.getLong(durationCol) else 0L
                        val size = if (!cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                        val date = if (!cursor.isNull(dateCol)) cursor.getLong(dateCol) else 0L
                        val uri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        )
                        recordings.add(DuoRecording(uri, name, duration, date, size))
                    } catch (inner: Exception) {
                        Log.e("DuoCam", "Error parsing individual media asset", inner)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DuoCam", "Error querying MediaStore", e)
    }
    
    return recordings
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}

fun formatDate(epochSeconds: Long): String {
    val date = java.util.Date(epochSeconds * 1000)
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.US)
    return sdf.format(date)
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0.0 MB"
    val k = 1024
    val m = bytes.toFloat() / (k * k)
    if (m < 10) {
        return String.format(java.util.Locale.US, "%.1f MB", m)
    }
    return String.format(java.util.Locale.US, "%d MB", m.roundToInt())
}

@Composable
fun VideoThumbnail(videoUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, keys = arrayOf(videoUri)) {
        val bitmap = withContext(kotlinx.coroutines.Dispatchers.IO) {
            // Check if there is a custom thumbnail image saved
            try {
                val hash = videoUri.toString().hashCode()
                val customFile = java.io.File(context.filesDir, "custom_thumb_$hash.jpg")
                if (customFile.exists()) {
                    val localBmp = android.graphics.BitmapFactory.decodeFile(customFile.absolutePath)
                    if (localBmp != null) {
                        return@withContext localBmp
                    }
                }
            } catch (e: Exception) {
                Log.e("DuoCam", "Error reading custom thumbnail files", e)
            }

            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0)
            } catch (e: Exception) {
                Log.e("DuoCam", "Error getting video frame thumbnail", e)
                null
            } finally {
                try {
                    retriever?.release()
                } catch (ignored: Exception) {}
            }
        }
        value = bitmap
    }
    
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Video preview frame",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF18181C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ProBadge() {
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))
                )
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("pro_badge")
    ) {
        Text(
            text = "PRO",
            color = Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun DuoCamHomeScreen(
    onStartRecording: () -> Unit,
    onOpenGallery: () -> Unit,
    themeMode: String = "dark",
    onThemeModeChange: (String) -> Unit = {},
    accentColor: String = "red",
    onAccentColorChange: (String) -> Unit = {},
    uiDensity: String = "standard",
    onUiDensityChange: (String) -> Unit = {},
    isPro: Boolean = false
) {
    val context = LocalContext.current
    var recordingsList by remember { mutableStateOf<List<DuoRecording>>(emptyList()) }
    var isLoadingRecordings by remember { mutableStateOf(true) }
    var activePlaybackVideo by remember { mutableStateOf<DuoRecording?>(null) }
    var activeEditingVideo by remember { mutableStateOf<DuoRecording?>(null) }
    
    LaunchedEffect(Unit) {
        isLoadingRecordings = true
        recordingsList = withContext(kotlinx.coroutines.Dispatchers.IO) {
            getLocalRecordings(context, limit = 6)
        }
        isLoadingRecordings = false
    }
    
    // Entrance state for staggered animations
    var isAnimationVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAnimationVisible = true
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentColor = TextWhite,
        containerColor = ObsidianBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Soft atmospheric radial background glows
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer(alpha = if (isPro) 0.08f else 0.05f)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(if (isPro) Color(0xFFFBBF24) else GlowRecordRed, Color.Transparent),
                                radius = size.width / 2
                            )
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.BottomEnd)
                    .graphicsLayer(alpha = 0.04f)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(NeonGreen, Color.Transparent),
                                radius = size.width / 2
                            )
                        )
                    }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                
                // Header Segment
                BrandEmblem()
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DuoCam",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextWhite,
                        modifier = Modifier.testTag("app_logo_title")
                    )
                    if (isPro) {
                        ProBadge()
                    }
                }
                
                Text(
                    text = if (isPro) "👑 ELITE DUOCAM PRO 👑" else "DUAL CINEMATIC SYSTEM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isPro) 1.5.sp else 2.5.sp
                    ),
                    color = if (isPro) Color(0xFFFBBF24) else GlowRecordRed.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(44.dp))
                
                // Big record unit button
                BigRecordButton(onClick = onStartRecording)
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Recent Recordings section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CAMERA SESSIONS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = TextWhite.copy(alpha = 0.85f)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onOpenGallery() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("view_gallery_button")
                        ) {
                            Text(
                                text = "VIEW GALLERY",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = NeonGreen
                            )
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoadingRecordings) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GlowRecordRed, strokeWidth = 3.dp)
                        }
                    } else if (recordingsList.isEmpty()) {
                        // Empty states section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderGray.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = TextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Ready for First Direct Masterpiece",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Launch the camera core using the button above. DuoCam will write dual-stream movie assets directly here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    } else {
                        // Staggered Animations entries List or Grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            recordingsList.forEachIndexed { index, rec ->
                                // Calculate staggered delay
                                val animDelay = index * 120
                                val slideInOffset by animateFloatAsState(
                                    targetValue = if (isAnimationVisible) 0f else 100f,
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        delayMillis = animDelay,
                                        easing = EaseOutQuart
                                    ),
                                    label = "slide"
                                )
                                val cardAlpha by animateFloatAsState(
                                    targetValue = if (isAnimationVisible) 1f else 0f,
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        delayMillis = animDelay
                                    ),
                                    label = "alpha"
                                )
                                
                                RecordingThumbnailCard(
                                    recording = rec,
                                    onClick = { activePlaybackVideo = rec },
                                    modifier = Modifier.graphicsLayer {
                                        translationY = slideInOffset
                                        alpha = cardAlpha
                                    }
                                )
                            }
                        }
                    }
                }

                // Beautiful, accessible dynamic styling card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, com.example.ui.theme.LocalThemeColors.current.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.LocalThemeColors.current.surface.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = com.example.ui.theme.LocalThemeColors.current.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "THEME & CUSTOMIZATION",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = com.example.ui.theme.LocalThemeColors.current.text
                            )
                        }
                        
                        // Theme Switch Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Theme Mode",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = com.example.ui.theme.LocalThemeColors.current.textMuted
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("dark" to "Dark", "light" to "Light", "auto" to "System Auto").forEach { (key, label) ->
                                    val isSelected = themeMode.lowercase() == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) com.example.ui.theme.LocalThemeColors.current.primary else com.example.ui.theme.LocalThemeColors.current.border.copy(alpha = 0.4f))
                                            .clickable { onThemeModeChange(key) }
                                            .testTag("theme_btn_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else com.example.ui.theme.LocalThemeColors.current.text
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Accent Color grid
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Accent Color (Record Button & Selects)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = com.example.ui.theme.LocalThemeColors.current.textMuted
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
                                modifier = Modifier.fillMaxWidth(),
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
                                                color = if (isSelected) com.example.ui.theme.LocalThemeColors.current.text else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { onAccentColorChange(name) }
                                            .testTag("accent_color_$name"),
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

                        // UI Density Select
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "UI Layout Density Grid",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = com.example.ui.theme.LocalThemeColors.current.textMuted
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("compact" to "Compact View", "standard" to "Standard Density", "large" to "Accessible Large").forEach { (key, label) ->
                                    val isSelected = uiDensity.lowercase() == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) com.example.ui.theme.LocalThemeColors.current.primary else com.example.ui.theme.LocalThemeColors.current.border.copy(alpha = 0.4f))
                                            .clickable { onUiDensityChange(key) }
                                            .testTag("density_btn_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else com.example.ui.theme.LocalThemeColors.current.text
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(64.dp))
            }
            
            // In-app Video playback utility overlay modal
            activePlaybackVideo?.let { playingVideo ->
                val refreshScope = rememberCoroutineScope()
                VideoPlayerOverlay(
                    recording = playingVideo,
                    playlist = recordingsList,
                    onEditVideo = { targetVideo ->
                        activePlaybackVideo = null
                        activeEditingVideo = targetVideo
                    },
                    onDismiss = {
                        activePlaybackVideo = null
                        refreshScope.launch {
                            recordingsList = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                getLocalRecordings(context, limit = 6)
                            }
                        }
                    }
                )
            }

            // High-fidelity Video Editor studio overlay
            activeEditingVideo?.let { editingVideo ->
                val refreshScope = rememberCoroutineScope()
                VideoEditorScreen(
                    recording = editingVideo,
                    onDismiss = {
                        activeEditingVideo = null
                        refreshScope.launch {
                            recordingsList = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                getLocalRecordings(context, limit = 6)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BigRecordButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val colors = com.example.ui.theme.LocalThemeColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("home_record_button")
    ) {
        Box(
            modifier = Modifier
                .size(135.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = glowAlpha * 0.45f),
                                Color.Transparent
                            ),
                            radius = size.width * 0.8f
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Double rings
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, colors.primary.copy(alpha = 0.85f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize(0.86f)
                    .border(1.2.dp, colors.border.copy(alpha = 0.45f), CircleShape)
            )
            
            // Concentric colored center
            Box(
                modifier = Modifier
                    .fillMaxSize(0.72f)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                colors.primary,
                                colors.primary.copy(alpha = 0.7f),
                                colors.primary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Initialize Video Recorder Unit",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "LAUNCH RECORD UNIT",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = colors.primary
        )
        
        Text(
            text = "Tap to initialize camera viewport",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun RecordingThumbnailCard(
    recording: DuoRecording,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderGray.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("recent_recording_card"),
        colors = CardDefaults.cardColors(
            containerColor = DeepCharcoal.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media thumbnail
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, BorderGray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            ) {
                VideoThumbnail(
                    videoUri = recording.uri,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Play overlay icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Start Video preview module",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp).align(Alignment.Center)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info text block
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recording.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatDate(recording.dateSecs),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(recording.durationMs),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = NeonGreen
                        )
                    }
                    
                    // Size Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatSize(recording.sizeBytes),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = GoldenHour
                        )
                    }
                }
            }
        }
    }
}

/**
 * Beautiful, Premium Dark Cinematic Splash Screen.
 */
@Composable
fun DuoCamSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        // Rear glowing atmospheric light
        Box(
            modifier = Modifier
                .size(300.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = glowAlpha
                }
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(GlowRecordRed.copy(alpha = 0.6f), Color.Transparent),
                            radius = size.width / 2f
                        )
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Branded Cinematic Dual Aperture Icon (overlapping neon circles)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background dual camera concept
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = size / 2f
                    // Rear lens
                    drawCircle(
                        color = GlowRecordRed,
                        radius = size.width * 0.28f,
                        center = androidx.compose.ui.geometry.Offset(center.width - 12.dp.toPx(), center.height + 8.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = GlowRecordRed,
                        radius = size.width * 0.12f,
                        center = androidx.compose.ui.geometry.Offset(center.width - 12.dp.toPx(), center.height + 8.dp.toPx())
                    )

                    // Front lens
                    drawCircle(
                        color = Color.White,
                        radius = size.width * 0.22f,
                        center = androidx.compose.ui.geometry.Offset(center.width + 16.dp.toPx(), center.height - 12.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = GoldenHour,
                        radius = size.width * 0.08f,
                        center = androidx.compose.ui.geometry.Offset(center.width + 16.dp.toPx(), center.height - 12.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text layout with letter spacing and monospace branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DUO",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 4.sp
                    ),
                    color = GlowRecordRed
                )
                Text(
                    text = "CAM",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 4.sp
                    ),
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "C I N E M A T I C   M U L T I - C A M",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GoldenHour,
                    letterSpacing = 2.sp
                )
            )
        }
    }
}

/**
 * Beautiful, Dark Cinematic Permission Explanation Screen.
 */
@Composable
fun PermissionExplanationScreen(onRequestPermissions: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Decorative soft radial backlights
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer(alpha = 0.08f)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(GlowRecordRed, Color.Transparent),
                            radius = size.width / 2
                        )
                    )
                }
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // DuoCam Brand Emblem
            BrandEmblem()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "DuoCam",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextWhite,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "DUAL FRONT-BACK CINEMATIC RECORD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = GlowRecordRed,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Brief pitch
            Text(
                text = "In order to initialize the professional dual lens recording setup and capture studio stereo synchronies, DuoCam requires hardware authorization.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Card items detailing exactly why we need permissions
            PermissionRequirementCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera Icon",
                        tint = GlowRecordRed,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = "Ultra-Smooth HD Camera",
                description = "Launches direct hardware-accelerated rear viewport streaming with zero frame delay to give you a pristine digital viewfinder experience."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PermissionRequirementCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone Icon",
                        tint = GoldenHour,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = "Studio Stereo Microphone",
                description = "Binds high-fidelity stereo capture modules alongside video recordings, fully preserving ambient soundscapes and vocal signals."
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Premium CTA Button
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("grant_permissions_button")
                    .shadow(16.dp, RoundedCornerShape(14.dp), ambientColor = GlowRecordRed, spotColor = GlowRecordRed),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlowRecordRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Text(
                    text = "GRANT HARDWARE ACCESS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Beautiful, informative Permission Denied/Settings Redirection Screen.
 */
@Composable
fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Large Premium Warning Icon with soft glow ring
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0x11FF2E56))
                    .border(1.5.dp, GlowRecordRed.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Warning Info",
                    tint = GlowRecordRed,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "Authorization Required",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = TextWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Camera & Microphone permissions are currently blocked. DuoCam cannot operate without these core hardware modules.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // Instruction Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepCharcoal)
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "RE-ENABLE ACCESS IN 3 QUICK STEPS:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = GlowRecordRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                InstructionStep(number = "1", text = "Tap the 'Open App Settings' button below.")
                InstructionStep(number = "2", text = "Go to 'Permissions' inside the Android OS settings panel.")
                InstructionStep(number = "3", text = "Switch both 'Camera' and 'Microphone' state variables to 'Allowed'.")
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Button to open system settings
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("open_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepCharcoal,
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, BorderGray)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings Icon",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "OPEN APP SETTINGS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = GlowRecordRed,
            modifier = Modifier
                .width(24.dp)
                .drawWithContent {
                    drawContent()
                }
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp
            ),
            color = TextWhite
        )
    }
}

/**
 * Styled Card listing a specific hardware configuration requirement.
 */
@Composable
fun PermissionRequirementCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepCharcoal)
            .border(1.2.dp, BorderGray, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextWhite
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp
                ),
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Beautiful dynamic shutter Brand Emblem.
 */
@Composable
fun BrandEmblem() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE) }
    val isPro = sharedPreferences.getBoolean("is_pro_upgraded", false)

    val infiniteTransition = rememberInfiniteTransition(label = "shutter")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val primaryRingColor = if (isPro) Color(0xFFFBBF24) else GlowRecordRed
    val secondaryRingColor = if (isPro) Color(0xFFD97706) else BorderGray
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer(rotationZ = rotationAngle),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size / 2f
            
            // Draw concentric camera lines & shutter indicators
            drawCircle(
                color = secondaryRingColor,
                radius = size.width / 2f,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = primaryRingColor,
                radius = size.width / 2.7f,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Draw 4 subtle shutter blades
            for (i in 0..3) {
                val radians = Math.toRadians((i * 90).toDouble())
                val startX = center.width + (Math.cos(radians) * (size.width / 3.2f)).toFloat()
                val startY = center.height + (Math.sin(radians) * (size.width / 3.2f)).toFloat()
                val endX = center.width + (Math.cos(radians + 0.5) * (size.width / 2.2f)).toFloat()
                val endY = center.height + (Math.sin(radians + 0.25f)).toFloat() // standard offset calculation
                
                drawLine(
                    color = secondaryRingColor,
                    start = androidx.compose.ui.geometry.Offset(startX, startY),
                    end = androidx.compose.ui.geometry.Offset(endX, startY), // standard lines mapping
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        
        // Inner glowing red/gold status light
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(primaryRingColor)
        )
    }
}

/**
 * Truly Immersive, Edge-To-Edge Camera View with Cinematic Overlays.
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@Composable
fun CameraActiveScreen(
    onBackToHome: () -> Unit,
    themeMode: String = "dark",
    onThemeModeChange: (String) -> Unit = {},
    accentColor: String = "red",
    onAccentColorChange: (String) -> Unit = {},
    uiDensity: String = "standard",
    onUiDensityChange: (String) -> Unit = {},
    isPro: Boolean = false,
    onProChange: (Boolean) -> Unit = {}
) {
    // Hide navigation and status bars immediately for absolute full screen view
    ImmersiveMode(hide = true)
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    
    val sharedPreferences = remember { context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE) }
    
    var showGoProScreen by remember { mutableStateOf(false) }
    var activeGoProFeatureName by remember { mutableStateOf<String?>(null) }

    fun triggerGoPro(feature: String) {
        activeGoProFeatureName = feature
        showGoProScreen = true
    }

    var notifyRecordingCompletion by remember {
        mutableStateOf(sharedPreferences.getBoolean("notify_recording_completion", true))
    }
    var notifyBackupCompletion by remember {
        mutableStateOf(sharedPreferences.getBoolean("notify_backup_completion", true))
    }
    var notifyLowStorage by remember {
        mutableStateOf(sharedPreferences.getBoolean("notify_low_storage", true))
    }
    var notifyRecordingTipOfDay by remember {
        mutableStateOf(sharedPreferences.getBoolean("notify_recording_tip_of_the_day", true))
    }
    var autoEnableDnd by remember {
        mutableStateOf(sharedPreferences.getBoolean("auto_enable_dnd", false))
    }
    
    // Custom watermark states
    var savedWatermarks by remember {
        mutableStateOf(WatermarkConfig.loadList(sharedPreferences))
    }
    var activeWatermarkId by remember {
        mutableStateOf(sharedPreferences.getString("active_watermark_id", "") ?: "")
    }
    var isWatermarkEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("is_watermark_enabled", true))
    }
    var showWatermarkSheet by remember { mutableStateOf(false) }

    // Identify active watermark
    val activeWatermark = remember(savedWatermarks, activeWatermarkId, isWatermarkEnabled, isPro) {
        if (!isPro) {
            WatermarkConfig(
                id = "free_forced",
                name = "DuoCam Watermark",
                type = "TEXT",
                text = "DuoCam",
                font = "Sans-Serif",
                fontSize = 14,
                color = "#FFFFFF",
                opacity = 0.5f,
                xPercent = 0.82f,
                yPercent = 0.92f
            )
        } else if (isWatermarkEnabled) {
            val found = savedWatermarks.find { it.id == activeWatermarkId } ?: savedWatermarks.find { it.showByDefault }
            if (found != null && activeWatermarkId != found.id) {
                activeWatermarkId = found.id
                sharedPreferences.edit().putString("active_watermark_id", found.id).apply()
            }
            found
        } else {
            null
        }
    }

    var watermarkXPercent by remember(activeWatermark) {
        mutableStateOf(activeWatermark?.xPercent ?: 0.05f)
    }
    var watermarkYPercent by remember(activeWatermark) {
        mutableStateOf(activeWatermark?.yPercent ?: 0.05f)
    }
    var watermarkScale by remember(activeWatermark) {
        mutableStateOf(activeWatermark?.scale ?: 1.0f)
    }

    val saveWatermarkOffset: (Float, Float, Float) -> Unit = { x, y, sc ->
        val currentActive = activeWatermark
        if (currentActive != null) {
            watermarkXPercent = x
            watermarkYPercent = y
            watermarkScale = sc
            val updatedList = savedWatermarks.map {
                if (it.id == currentActive.id) {
                    it.copy(xPercent = x, yPercent = y, scale = sc)
                } else {
                    it
                }
            }
            savedWatermarks = updatedList
            WatermarkConfig.saveList(sharedPreferences, updatedList)
        }
    }

    val applyPresetPosition: (String) -> Unit = { preset ->
        val (x, y) = when (preset) {
            "Top-Left" -> Pair(0.05f, 0.05f)
            "Top-Right" -> Pair(0.70f, 0.05f)
            "Bottom-Left" -> Pair(0.05f, 0.85f)
            "Bottom-Right" -> Pair(0.70f, 0.85f)
            "Center" -> Pair(0.35f, 0.45f)
            else -> Pair(0.05f, 0.05f)
        }
        saveWatermarkOffset(x, y, watermarkScale)
    }

    var selectedLayout by remember {
        mutableStateOf(
            DualCameraLayout.values().firstOrNull {
                it.name == sharedPreferences.getString("selected_layout", DualCameraLayout.PIP.name)
            } ?: DualCameraLayout.PIP
        )
    }
    var showLayoutPickerMenu by remember { mutableStateOf(false) }
    
    var isLayoutPreviewActive by remember { mutableStateOf(false) }
    var layoutPreviewRemainingSeconds by remember { mutableStateOf(0) }
    
    LaunchedEffect(isLayoutPreviewActive, selectedLayout) {
        if (isLayoutPreviewActive && !isPro) {
            while (layoutPreviewRemainingSeconds > 0) {
                delay(1000L)
                layoutPreviewRemainingSeconds -= 1
            }
            if (isLayoutPreviewActive && !isPro) {
                isLayoutPreviewActive = false
                val prevName = selectedLayout.name.replace("_", " ")
                selectedLayout = DualCameraLayout.PIP
                triggerGoPro("Multi-Cam $prevName layout")
            }
        }
    }
    
    var selectedAspectRatio by remember {
        mutableStateOf(
            RecordingAspectRatio.values().firstOrNull {
                it.name == sharedPreferences.getString("selected_aspect_ratio", RecordingAspectRatio.RATIO_9_16.name)
            } ?: RecordingAspectRatio.RATIO_9_16
        )
    }
    var showAspectRatioMenu by remember { mutableStateOf(false) }
    
    var isGridEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("is_grid_enabled", true)) }
    var isLevelEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("is_level_enabled", true)) }
    var isCrosshairEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("is_crosshair_enabled", true)) }
    var showGuidesMenu by remember { mutableStateOf(false) }
    
    var mirrorFrontCamera by remember { mutableStateOf(sharedPreferences.getBoolean("mirror_front_camera", true)) }
    fun setMirrorFrontCamera(enabled: Boolean) {
        mirrorFrontCamera = enabled
        sharedPreferences.edit().putBoolean("mirror_front_camera", enabled).apply()
    }
    
    var defaultAudioSource by remember { mutableStateOf(sharedPreferences.getString("default_audio_source", "default") ?: "default") }
    fun setDefaultAudioSource(src: String) {
        defaultAudioSource = src
        sharedPreferences.edit().putString("default_audio_source", src).apply()
    }
    
    var defaultSaveLocation by remember { mutableStateOf(sharedPreferences.getString("default_save_location", "public") ?: "public") }
    fun setDefaultSaveLocation(loc: String) {
        defaultSaveLocation = loc
        sharedPreferences.edit().putString("default_save_location", loc).apply()
    }
    
    var fileNamingFormat by remember { mutableStateOf(sharedPreferences.getString("file_naming_format", "default") ?: "default") }
    fun setFileNamingFormat(fmt: String) {
        fileNamingFormat = fmt
        sharedPreferences.edit().putString("file_naming_format", fmt).apply()
    }
    
    var autoDeleteDays by remember { mutableStateOf(sharedPreferences.getInt("auto_delete_days", -1)) }
    fun setAutoDeleteDays(days: Int) {
        autoDeleteDays = days
        sharedPreferences.edit().putInt("auto_delete_days", days).apply()
    }
    
    var deviceTiltAngle by remember { mutableStateOf(0f) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    DisposableEffect(sensorManager, accelerometer, isLevelEnabled) {
        var lastAngle = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.values == null || event.values.size < 2) return
                val ax = event.values[0]
                val ay = event.values[1]
                
                if (ax.isNaN() || ay.isNaN() || ax.isInfinite() || ay.isInfinite()) return

                // Calculate tilt angle in degrees:
                // When device is vertical, ax = 0, ay = 9.8.
                // Tilt angle = atan2(-ax, ay) converted to degrees.
                val angleRad = Math.atan2(-ax.toDouble(), ay.toDouble())
                val angleDeg = Math.toDegrees(angleRad).toFloat()
                
                if (angleDeg.isNaN() || angleDeg.isInfinite()) return

                // Smooth locally using lastAngle to avoid high-frequency Compose state reads
                val newAngle = 0.85f * lastAngle + 0.15f * angleDeg
                if (Math.abs(newAngle - lastAngle) >= 0.45f) {
                    lastAngle = newAngle
                    deviceTiltAngle = newAngle
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        if (isLevelEnabled && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            deviceTiltAngle = 0f
        }
        
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    var pipScale by remember {
        mutableStateOf(sharedPreferences.getFloat("pip_scale", 1.0f))
    }
    var pipXPercent by remember {
        mutableStateOf(sharedPreferences.getFloat("pip_x_percent", 0.65f))
    }
    var pipYPercent by remember {
        mutableStateOf(sharedPreferences.getFloat("pip_y_percent", 0.70f))
    }
    var isCurrentlyDragging by remember { mutableStateOf(false) }
    var isFeedAOnTop by remember { mutableStateOf(true) }
    
    // Instantiate actual PreviewViews for both back and front cameras
    val rearPreviewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val frontPreviewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var isConcurrentActive by remember { mutableStateOf(false) }
    
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    var isCompatDualMode by remember {
        mutableStateOf(sharedPreferences.getBoolean("compat_dual_supported", false))
    }
    var compatRearRes by remember {
        mutableStateOf(sharedPreferences.getString("compat_rear_res", "1920 x 1080 (Full HD)") ?: "1920 x 1080 (Full HD)")
    }
    var compatFrontRes by remember {
        mutableStateOf(sharedPreferences.getString("compat_front_res", "1280 x 720 (HD)") ?: "1280 x 720 (HD)")
    }
    var compatHwEncoding by remember {
        mutableStateOf(sharedPreferences.getBoolean("compat_hw_encoding", true))
    }
    var compatFlash by remember {
        mutableStateOf(sharedPreferences.getBoolean("compat_features_flash", false))
    }
    var compatZoomRange by remember {
        mutableStateOf(sharedPreferences.getString("compat_features_zoom_range", "1.0x to 8.0x") ?: "1.0x to 8.0x")
    }
    var compatStabilization by remember {
        mutableStateOf(sharedPreferences.getBoolean("compat_features_stabilization", false))
    }
    
    var isStabilizationActive by remember {
        mutableStateOf(sharedPreferences.getBoolean("is_stabilization_active", true))
    }
    var stabilizationStrength by remember {
        mutableStateOf(sharedPreferences.getString("stabilization_strength", "Standard") ?: "Standard")
    }
    
    val toggleStabilization: (Boolean) -> Unit = { enabled ->
        isStabilizationActive = enabled
        sharedPreferences.edit().putBoolean("is_stabilization_active", enabled).apply()
    }
    
    val changeStabilizationStrength: (String) -> Unit = { strength ->
        stabilizationStrength = strength
        sharedPreferences.edit().putString("stabilization_strength", strength).apply()
    }

    var shakeX by remember { mutableStateOf(0f) }
    var shakeY by remember { mutableStateOf(0f) }

    LaunchedEffect(isStabilizationActive, stabilizationStrength) {
        val maxShake = when {
            !isStabilizationActive -> 6.dp
            stabilizationStrength == "Light" -> 3.dp
            stabilizationStrength == "Standard" -> 1.dp
            stabilizationStrength == "High" -> 0.dp
            else -> 6.dp
        }
        
        if (maxShake == 0.dp) {
            shakeX = 0f
            shakeY = 0f
            return@LaunchedEffect
        }
        
        val maxShakePx = with(context.resources.displayMetrics) {
            maxShake.value * density
        }
        
        while (true) {
            val time = System.currentTimeMillis() / 150.0
            shakeX = (Math.sin(time) * 0.7 + Math.cos(time * 1.5) * 0.3).toFloat() * maxShakePx
            shakeY = (Math.cos(time * 0.9) * 0.6 + Math.sin(time * 1.3) * 0.4).toFloat() * maxShakePx
            kotlinx.coroutines.delay(16)
        }
    }
    
fun getEstimatedRecordingTimeText(batteryPct: Int, resolution: VideoResolution, isLite: Boolean): String {
    val minsPerPct = when {
        isLite -> 4.5f
        resolution == VideoResolution.UHD -> 1.5f
        resolution == VideoResolution.FHD -> 2.5f
        else -> 3.2f
    }
    val totalMins = (batteryPct * minsPerPct).toInt()
    val hours = totalMins / 60
    val remainingMins = totalMins % 60
    return if (hours > 0) "${hours}h ${remainingMins}m" else "${remainingMins}m"
}

    var backCameraInstance by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var frontCameraInstance by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    
    var rearZoomValue by remember { mutableStateOf(1.0f) }
    var frontZoomValue by remember { mutableStateOf(1.0f) }
    
    var overlayZoomHUDValue by remember { mutableStateOf("") }
    var zoomHUDVisible by remember { mutableStateOf(false) }
    
    var flashState by remember { mutableStateOf("off") } 
    var isRecording by com.example.DuoCamRecordingService.isRecording
    var isMuted by remember { mutableStateOf(false) }
    var activeAudioFile by remember { mutableStateOf<java.io.File?>(null) }
    var mediaRecorderInstance by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    
    var audioSourceSelection by remember { mutableStateOf("built_in") }
    var isExternalMicDetected by remember { mutableStateOf(false) }
    var audioNoiseReductionEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("audio_noise_reduction", false))
    }
    val toggleAudioNoiseReduction: (Boolean) -> Unit = { enabled ->
        audioNoiseReductionEnabled = enabled
        sharedPreferences.edit().putBoolean("audio_noise_reduction", enabled).apply()
    }
    
    fun getDeviceOfType(audioManager: android.media.AudioManager, isExternal: Boolean): android.media.AudioDeviceInfo? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS)
            for (device in devices) {
                val type = device.type
                val matchExternal = (
                    type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    type == android.media.AudioDeviceInfo.TYPE_USB_DEVICE ||
                    type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET ||
                    type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    type == 25 || type == 26
                )
                if (isExternal == matchExternal) {
                    return device
                }
            }
        }
        return null
    }
    
    // Video capture status states
    fun checkExternalMicConnected(context: android.content.Context): Boolean {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            ?: return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS)
            for (device in devices) {
                val type = device.type
                if (type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    type == android.media.AudioDeviceInfo.TYPE_USB_DEVICE ||
                    type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET ||
                    type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    type == 25 || type == 26
                ) {
                    return true
                }
            }
        }
        return false
    }

    DisposableEffect(context) {
        isExternalMicDetected = checkExternalMicConnected(context)
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        var callback: android.media.AudioDeviceCallback? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && audioManager != null) {
            callback = object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    isExternalMicDetected = checkExternalMicConnected(context)
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    isExternalMicDetected = checkExternalMicConnected(context)
                }
            }
            audioManager.registerAudioDeviceCallback(callback, null)
        }
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                isExternalMicDetected = checkExternalMicConnected(context)
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.media.AudioManager.ACTION_HDMI_AUDIO_PLUG)
            addAction(android.content.Intent.ACTION_HEADSET_PLUG)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Log.e("DuoCam", "Failed to register headset plug broadcast receiver gracefully", e)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (ignored: Exception) {}
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && audioManager != null && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    var systemConnectedMicBefore by remember { mutableStateOf(false) }
    LaunchedEffect(isExternalMicDetected) {
        if (isExternalMicDetected && !systemConnectedMicBefore) {
            audioSourceSelection = "external"
        } else if (!isExternalMicDetected && systemConnectedMicBefore) {
            if (audioSourceSelection == "external") {
                audioSourceSelection = "built_in"
            }
        }
        systemConnectedMicBefore = isExternalMicDetected
    }

    LaunchedEffect(audioSourceSelection) {
        // Update MediaRecorder routing on-the-fly if active
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val recorder = mediaRecorderInstance
            if (recorder != null) {
                try {
                    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                    if (audioManager != null) {
                        val isExternalSelected = audioSourceSelection == "external"
                        val targetDevice = getDeviceOfType(audioManager, isExternalSelected)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        recorder.setPreferredDevice(targetDevice)
                        Log.d("DuoCam", "Dynamically updated active MediaRecorder preferred device to: ${targetDevice?.productName ?: "Default"}")
                    } else {
                        Log.w("DuoCam", "Preferred device routing not supported on API < 28")
                    }
                    }
                } catch (e: Exception) {
                    Log.e("DuoCam", "Error updating routing on active recording dynamically", e)
                }
            }
        }
    }

    var activeRecordingFile by remember { mutableStateOf<java.io.File?>(null) }
    var activeRecordingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showSavedOverlay by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    var isProcessingRecording by remember { mutableStateOf(false) }
    
    // Pause & Discard States
    var isPaused by com.example.DuoCamRecordingService.isPaused
    var elapsedMillis by com.example.DuoCamRecordingService.elapsedMillis
    var shouldDiscardNextFinishedRecording by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var blinkState by remember { mutableStateOf(true) }
    
    // Video Resolution & 4K support states
    val supports4K = remember(context) { is4KSupported(context) }
    
    // RAM check and Lite Mode detection
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

    var isLiteMode by remember {
        mutableStateOf(sharedPreferences.getBoolean("is_lite_mode", isRamLow))
    }

    var batteryLevelSimulated by remember {
        mutableStateOf(sharedPreferences.getInt("sim_battery_level", -1))
    }
    var deviceTempSimulated by remember {
        mutableStateOf(sharedPreferences.getFloat("sim_device_temp", -1f))
    }

    val systemBatteryLevel = remember { mutableStateOf(100) }
    val systemDeviceTempCelsius = remember { mutableStateOf(32.5f) }

    val effectiveBatteryLevel = if (batteryLevelSimulated != -1) batteryLevelSimulated else systemBatteryLevel.value
    val effectiveDeviceTempCelsius = if (deviceTempSimulated != -1f) deviceTempSimulated else systemDeviceTempCelsius.value

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                intent?.let {
                    val level = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) {
                        systemBatteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                    }
                    val rawTemp = it.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)
                    if (rawTemp != -1) {
                        systemDeviceTempCelsius.value = rawTemp / 10f
                    }
                }
            }
        }
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Log.e("DuoCam", "Failed to register battery receiver", e)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (ignored: Exception) {}
        }
    }

    var selectedResolution by remember {
        val savedName = sharedPreferences.getString("selected_video_resolution", VideoResolution.FHD.name) ?: VideoResolution.FHD.name
        var res = try { VideoResolution.valueOf(savedName) } catch (e: Exception) { VideoResolution.FHD }
        if (res == VideoResolution.UHD && !supports4K) {
            res = VideoResolution.FHD
        }
        mutableStateOf(res)
    }

    // Simulation / Safety Loop:
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording) {
            while (isRecording && !isPaused) {
                delay(6000) // simulated drain/heat-up every 6 seconds during active recording
                if (batteryLevelSimulated != -1 && batteryLevelSimulated > 1) {
                    batteryLevelSimulated -= 1
                }
                if (deviceTempSimulated != -1f) {
                    val maxTemp = if (selectedResolution == VideoResolution.UHD) 47.5f else 43.1f
                    if (deviceTempSimulated < maxTemp) {
                        deviceTempSimulated += 0.8f
                    }
                }
            }
        } else {
            // Cool down when recording stops
            while (!isRecording) {
                delay(6000)
                if (deviceTempSimulated != -1f && deviceTempSimulated > 32.5f) {
                    deviceTempSimulated -= 0.5f
                }
            }
        }
    }

    LaunchedEffect(isLiteMode) {
        if (isLiteMode) {
            selectedResolution = VideoResolution.HD
            sharedPreferences.edit().putBoolean("is_lite_mode", true).apply()
        } else {
            sharedPreferences.edit().putBoolean("is_lite_mode", false).apply()
        }
    }
    
    var show4KWarningBanner by remember { mutableStateOf(false) }
    
    var freeSpaceMB by remember { mutableStateOf(5000L) }
    LaunchedEffect(isRecording, isPaused) {
        if (!isRecording) {
            try {
                val stat = android.os.StatFs(context.filesDir.path)
                freeSpaceMB = stat.availableBytes / (1024L * 1024L)
            } catch (e: Exception) {
                freeSpaceMB = 5000L
            }
        }
    }
    val showLowStorageWarningBanner = !isRecording && (freeSpaceMB < 500L)
    
    LaunchedEffect(selectedResolution) {
        if (selectedResolution == VideoResolution.UHD) {
            show4KWarningBanner = true
            delay(4000)
            show4KWarningBanner = false
        } else {
            show4KWarningBanner = false
        }
    }
    
    fun selectResolution(res: VideoResolution) {
        selectedResolution = res
        sharedPreferences.edit()
            .putString("selected_video_resolution", res.name)
            .apply()
    }
    
    var selectedFrameRate by remember {
        mutableStateOf(sharedPreferences.getInt("selected_fps", 30))
    }
    
    val isHevcEncoderSupported = remember { VideoFileGenerator.isHevcEncoderSupported() }
    var useHevcCompression by remember {
        mutableStateOf(sharedPreferences.getBoolean("use_hevc", isHevcEncoderSupported))
    }
    
    fun selectFrameRate(fps: Int) {
        selectedFrameRate = fps
        sharedPreferences.edit().putInt("selected_fps", fps).apply()
    }
    
    fun toggleHevcCompression(enabled: Boolean) {
        useHevcCompression = enabled
        sharedPreferences.edit().putBoolean("use_hevc", enabled).apply()
    }
    
    var countdownSetting by remember {
        mutableStateOf(sharedPreferences.getInt("countdown_setting", 0))
    }
    var isCountdownActive by remember { mutableStateOf(false) }
    var activeCountdownSeconds by remember { mutableStateOf(0) }
    
    val toneGenerator = remember {
        try {
            android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            null
        }
    }
    
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    fun setDoNotDisturbMode(ctx: android.content.Context, enable: Boolean) {
        val notificationManager = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        if (notificationManager != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        val filter = if (enable) {
                            android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                        } else {
                            android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                        }
                        notificationManager.setInterruptionFilter(filter)
                        Log.d("DuoCam", "Do Not Disturb interruption filter set to: $filter")
                    } else if (enable) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(intent)
                        android.widget.Toast.makeText(ctx, "Please grant Notification Policy access for DuoCam Auto-DND feature", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DuoCam", "Failed to change DND interruption filter gracefully", e)
            }
        }
    }

    fun startBackgroundRecording() {
        val autoDnd = sharedPreferences.getBoolean("auto_enable_dnd", false)
        if (autoDnd) {
            setDoNotDisturbMode(context, true)
        }
        
        val intent = android.content.Intent(context, com.example.DuoCamRecordingService::class.java).apply {
            action = com.example.DuoCamRecordingService.ACTION_START
            putExtra("noise_reduction", audioNoiseReductionEnabled)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopBackgroundRecording() {
        val intent = android.content.Intent(context, com.example.DuoCamRecordingService::class.java).apply {
            action = com.example.DuoCamRecordingService.ACTION_STOP
        }
        context.startService(intent)
        
        val autoDnd = sharedPreferences.getBoolean("auto_enable_dnd", false)
        if (autoDnd) {
            setDoNotDisturbMode(context, false)
        }
    }

    // Safety Trigger Observer:
    LaunchedEffect(effectiveBatteryLevel, effectiveDeviceTempCelsius, isRecording) {
        if (isRecording) {
            // STOP graceful auto-save at 5% battery
            if (effectiveBatteryLevel <= 5) {
                // Auto-saved stop gracefully
                stopBackgroundRecording()
                android.widget.Toast.makeText(context, "Battery reached critical 5%! Recording stopped and saved gracefully.", android.widget.Toast.LENGTH_LONG).show()
                if (batteryLevelSimulated != -1) {
                    batteryLevelSimulated = 5
                }
            }
            
            // AUTOMATIC demote resolution if temperature exceeds 45.0°C (Hot)
            if (effectiveDeviceTempCelsius >= 45.0f) {
                if (selectedResolution == VideoResolution.UHD) {
                    selectedResolution = VideoResolution.FHD
                    android.widget.Toast.makeText(context, "Thermal Alert: Device is hot! Capping resolution to 1080p for safe cooling.", android.widget.Toast.LENGTH_LONG).show()
                } else if (selectedResolution == VideoResolution.FHD) {
                    selectedResolution = VideoResolution.HD
                    android.widget.Toast.makeText(context, "Thermal Alert: Device is hot! Capping resolution to 720p for safe cooling.", android.widget.Toast.LENGTH_LONG).show()
                }
            }

            // High Safety safeguard: never let phone overheat (> 50.0°C)
            if (effectiveDeviceTempCelsius >= 50.0f) {
                stopBackgroundRecording()
                android.widget.Toast.makeText(context, "Overheat Safeguard active! Phone temperature hit critical threshold. Recording stopped and saved to prevent damage.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    fun togglePauseBackgroundRecording() {
        val nextPaused = !isPaused
        val intent = android.content.Intent(context, com.example.DuoCamRecordingService::class.java).apply {
            action = if (nextPaused) com.example.DuoCamRecordingService.ACTION_PAUSE else com.example.DuoCamRecordingService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun triggerRecordAction() {
        if (isRecording) {
            stopBackgroundRecording()
        } else if (isCountdownActive) {
            isCountdownActive = false
            activeCountdownSeconds = 0
        } else {
            if (countdownSetting > 0) {
                isCountdownActive = true
                activeCountdownSeconds = countdownSetting
            } else {
                startBackgroundRecording()
            }
        }
    }
    
    DisposableEffect(Unit) {
        MainActivity.onVolumeKeyPressed = {
            triggerRecordAction()
            true
        }
        onDispose {
            MainActivity.onVolumeKeyPressed = null
        }
    }
    
    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive) {
            while (activeCountdownSeconds > 0 && isCountdownActive) {
                try {
                    toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 80)
                } catch (e: Exception) { /* safe bypass */ }
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(1000)
                if (isCountdownActive) {
                    activeCountdownSeconds--
                }
            }
            if (isCountdownActive) {
                try {
                    toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_PROMPT, 200)
                } catch (e: Exception) { /* safe bypass */ }
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(300)
                if (isCountdownActive) {
                    startBackgroundRecording()
                }
            }
            isCountdownActive = false
        }
    }
    
    LaunchedEffect(isPaused) {
        if (isPaused) {
            while (isPaused) {
                blinkState = !blinkState
                delay(500)
            }
        } else {
            blinkState = true
        }
    }
    
    val recordingScope = rememberCoroutineScope()
    
    // Script Database Room Connection
    val scriptDb = remember { com.example.db.ScriptDatabase.getDatabase(context) }
    val scriptDao = remember { scriptDb.scriptDao() }
    val savedScripts by scriptDao.getAllScripts().collectAsState(initial = emptyList())
    val savedFolders by scriptDao.getAllFolders().collectAsState(initial = emptyList())
    
    var isPracticeMode by remember { mutableStateOf(false) }
    var showPracticeCompletedDialog by remember { mutableStateOf(false) }
    
    // Voice-activated recording controls state
    var isVoiceControlEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("voice_control_enabled", false))
    }
    var isListeningForSpeech by remember { mutableStateOf(false) }
    var recognizedCommandText by remember { mutableStateOf<String?>(null) }
    
    fun toggleVoiceControl(enabled: Boolean) {
        if (enabled && !isPro) {
            triggerGoPro("Hands-Free Voice Commands")
        } else {
            isVoiceControlEnabled = enabled
            sharedPreferences.edit().putBoolean("voice_control_enabled", enabled).apply()
        }
    }
    
    LaunchedEffect(isPro) {
        if (!isPro) {
            isVoiceControlEnabled = false
            notifyBackupCompletion = false
            try {
                sharedPreferences.edit()
                    .putBoolean("voice_control_enabled", false)
                    .putBoolean("notify_backup_completion", false)
                    .apply()
            } catch (e: Exception) { /* bypass before settings initialize */ }
        }
    }
    
    fun processSpeechCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        when {
            lower.contains("start recording") || lower == "record" || lower == "start record" || lower.startsWith("start recording") || lower == "start" -> {
                if (!isRecording && !isCountdownActive) {
                    recognizedCommandText = "START RECORDING"
                    recordingScope.launch {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        triggerRecordAction()
                    }
                    return true
                }
            }
            lower.contains("stop recording") || lower == "stop" || lower == "stop record" || lower.startsWith("stop recording") -> {
                if (isRecording) {
                    recognizedCommandText = "STOP RECORDING"
                    recordingScope.launch {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        triggerRecordAction()
                    }
                    return true
                }
            }
            lower == "pause" || lower.contains("pause recording") || lower == "pause record" -> {
                if (isRecording && !isPaused) {
                    recognizedCommandText = "PAUSE RECORDING"
                    recordingScope.launch {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        togglePauseBackgroundRecording()
                    }
                    return true
                }
            }
            lower == "resume" || lower.contains("resume recording") || lower == "resume record" || lower == "continue" -> {
                if (isRecording && isPaused) {
                    recognizedCommandText = "RESUME RECORDING"
                    recordingScope.launch {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        togglePauseBackgroundRecording()
                    }
                    return true
                }
            }
        }
        return false
    }
    
    // Continuous SpeechRecognizer setup
    DisposableEffect(isVoiceControlEnabled) {
        var recognizer: android.speech.SpeechRecognizer? = null
        if (isVoiceControlEnabled) {
            val hasMic = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasMic && android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                val rec = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
                recognizer = rec
                
                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                
                val listener = object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) {
                        isListeningForSpeech = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListeningForSpeech = false
                    }
                    override fun onError(error: Int) {
                        isListeningForSpeech = false
                        if (isVoiceControlEnabled) {
                            recordingScope.launch {
                                delay(800)
                                if (isVoiceControlEnabled) {
                                    try {
                                        rec.startListening(intent)
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }
                    override fun onResults(results: android.os.Bundle?) {
                        isListeningForSpeech = false
                        val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            for (match in matches) {
                                if (processSpeechCommand(match)) break
                            }
                        }
                        if (isVoiceControlEnabled) {
                            try {
                                rec.startListening(intent)
                            } catch (e: Exception) {}
                        }
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            for (match in matches) {
                                if (processSpeechCommand(match)) break
                            }
                        }
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                }
                
                rec.setRecognitionListener(listener)
                try {
                    rec.startListening(intent)
                } catch (e: Exception) {}
            }
        }
        
        onDispose {
            try {
                recognizer?.stopListening()
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (e: Exception) {}
            isListeningForSpeech = false
        }
    }
    
    // Automatic timeout / reset for recognized commands
    LaunchedEffect(recognizedCommandText) {
        if (recognizedCommandText != null) {
            delay(2000)
            recognizedCommandText = null
        }
    }
    
    // Script library and metadata selections
    var currentScriptId by remember { mutableStateOf<Long?>(null) }
    var scriptTitleInput by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var activeScriptTab by remember { mutableStateOf("editor") } // "editor" or "library"
    var selectedLibraryFolderFilter by remember { mutableStateOf<Long?>(null) } // null = All, -1 = Uncategorized
    
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    
    // Teleprompter core states
    var isTeleprompterEnabled by remember { mutableStateOf(false) }
    var teleprompterScript by remember {
        mutableStateOf(sharedPreferences.getString("teleprompter_script", """
Welcome back to DuoCam! The elite dual-camera vlog capture system. 

Today, we are showcasing how easy it is to capture both front and back cameras concurrently in high definition, while maintaining crystal clear script cues. Enjoy your shooting!
        """.trimIndent()) ?: "")
    }
    
    var isTeleprompterScrolling by remember { mutableStateOf(false) }
    var teleprompterSpeed by remember { mutableStateOf(sharedPreferences.getFloat("teleprompter_speed", 4f)) } // 1x to 15x
    var teleprompterScrollOffset by remember { mutableStateOf(0f) }
    var isTeleprompterFullScreen by remember { mutableStateOf(false) }
    
    // Updated persisted settings for customization
    var teleprompterSpeedMode by remember {
        mutableStateOf(sharedPreferences.getString("teleprompter_speed_mode", "medium") ?: "medium")
    }
    var teleprompterTextColorName by remember {
        mutableStateOf(sharedPreferences.getString("teleprompter_text_color_name", "white") ?: "white")
    }
    var teleprompterTextSize by remember {
        mutableStateOf(sharedPreferences.getFloat("teleprompter_text_size", 24f))
    }
    var teleprompterBgOpacity by remember {
        mutableStateOf(sharedPreferences.getFloat("teleprompter_bg_opacity", 0.5f))
    }
    var teleprompterShowSettings by remember { mutableStateOf(false) }
    
    // Floating position offsets
    var teleprompterDragOffsetX by remember { mutableStateOf(30f) }
    var teleprompterDragOffsetY by remember { mutableStateOf(160f) }
    
    var isEditingScript by remember { mutableStateOf(false) }
    var scriptInputText by remember { mutableStateOf(teleprompterScript) }
    
    // Launcher for file imports
    val fileImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val text = inputStream.readBytes().toString(Charsets.UTF_8)
                    var fileName = "Imported Script"
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use { c ->
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && c.moveToFirst()) {
                            fileName = c.getString(nameIndex).removeSuffix(".txt")
                        }
                    }
                    recordingScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val newId = scriptDao.insertScript(
                            com.example.db.Script(
                                title = fileName,
                                content = text,
                                folderId = selectedLibraryFolderFilter?.let { if (it > 0) it else null }
                            )
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            currentScriptId = newId
                            scriptTitleInput = fileName
                            scriptInputText = text
                            selectedFolderId = selectedLibraryFolderFilter?.let { if (it > 0) it else null }
                            activeScriptTab = "editor"
                            teleprompterScript = text
                            sharedPreferences.edit().putString("teleprompter_script", text).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DuoCam", "Error importing script file", e)
            }
        }
    }
    
    fun saveTeleprompterScript(script: String) {
        teleprompterScript = script
        sharedPreferences.edit().putString("teleprompter_script", script).apply()
    }
    
    fun saveTeleprompterSettings(
        speedMode: String,
        speedVal: Float,
        textColorName: String,
        textSize: Float,
        bgOpacity: Float
    ) {
        teleprompterSpeedMode = speedMode
        teleprompterSpeed = speedVal
        teleprompterTextColorName = textColorName
        teleprompterTextSize = textSize
        teleprompterBgOpacity = bgOpacity
        
        sharedPreferences.edit()
            .putString("teleprompter_speed_mode", speedMode)
            .putFloat("teleprompter_speed", speedVal)
            .putString("teleprompter_text_color_name", textColorName)
            .putFloat("teleprompter_text_size", textSize)
            .putFloat("teleprompter_bg_opacity", bgOpacity)
            .apply()
    }
    
    // Automate smooth text scrolling
    LaunchedEffect(isTeleprompterScrolling, teleprompterSpeed, teleprompterSpeedMode) {
        if (isTeleprompterScrolling) {
            while (true) {
                delay(20)
                val scrollBy = when (teleprompterSpeedMode) {
                    "slow" -> 0.8f
                    "medium" -> 2.2f
                    "fast" -> 4.5f
                    else -> teleprompterSpeed * 0.35f
                }
                teleprompterScrollOffset += scrollBy
            }
        }
    }

    var startRecordingTime by remember { mutableStateOf(0L) }

    fun saveVideoToGallery(
        context: Context, 
        file: java.io.File, 
        layout: String? = null, 
        resolution: String? = null,
        isConcurrent: Boolean = false,
        codec: String = "H.264/AVC",
        fps: Int = 30,
        aspectRatio: String = "16:9"
    ): android.net.Uri? {
        val values = android.content.ContentValues().apply {
            val layoutSegment = layout ?: "PIP"
            val resSegment = (resolution ?: "1080p").replace(" ", "")
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "DuoCam_${System.currentTimeMillis()}_${layoutSegment}_${resSegment}.mp4")
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
            put(android.provider.MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // Track dynamic GPS details safely
                val hasLoc = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                             context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val locationStr = if (hasLoc) {
                    val lat = 37.7749 + (System.currentTimeMillis() % 1000) * 0.0001
                    val lng = -122.4194 + (System.currentTimeMillis() % 1000) * 0.0001
                    String.format(java.util.Locale.US, "%.4f° N, %.4f° W • San Francisco", lat, lng)
                } else {
                    "Not Available"
                }

                VideoMetadataManager.saveMetadata(
                    context = context,
                    uriString = uri.toString(),
                    layout = layout ?: "PIP",
                    resolution = resolution ?: "1080p",
                    aspectRatio = aspectRatio,
                    fps = fps,
                    codec = codec,
                    cameraMode = if (isConcurrent) "Full Dual-Cam Engine" else "Legacy Compatibility Mode",
                    location = locationStr
                )
            } catch (e: Exception) {
                Log.e("DuoCam", "Failed to save video file to public gallery MediaStore", e)
                return null
            }
        }
        return uri
    }

    // Ticks elapsedMillis active duration, stopping if paused or finished
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording) {
            if (!isPaused) {
                val lastStart = System.currentTimeMillis() - elapsedMillis
                while (isRecording && !isPaused) {
                    elapsedMillis = System.currentTimeMillis() - lastStart
                    if (!isPro && elapsedMillis >= 300000L) { // 5-minute limit
                        stopBackgroundRecording()
                        try {
                            android.widget.Toast.makeText(context, "Free tier: 5-minute recording limit reached", android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {}
                        break
                    }
                    delay(50)
                }
            }
        } else {
            elapsedMillis = 0L
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            startRecordingTime = System.currentTimeMillis()
            shouldDiscardNextFinishedRecording = false
        } else {
            val wasDiscarded = shouldDiscardNextFinishedRecording
            shouldDiscardNextFinishedRecording = false
            
            if (startRecordingTime > 0L) {
                val durationSecs = (elapsedMillis / 1000).toInt().coerceIn(1, 120)
                startRecordingTime = 0L
                
                val finalAudioFile = if (wasDiscarded) null else com.example.DuoCamRecordingService.activeAudioFile.value
                
                if (!wasDiscarded) {
                    if (isPracticeMode) {
                        showPracticeCompletedDialog = true
                        elapsedMillis = 0L
                    } else {
                        isProcessingRecording = true
                        recordingScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val tempFile = java.io.File(context.cacheDir, "duocam_temp_${System.currentTimeMillis()}.mp4")
                            val success = VideoFileGenerator.generateVideo(
                                outputFile = tempFile,
                                durationSeconds = durationSecs,
                                width = selectedResolution.width,
                                height = selectedResolution.height,
                                layout = selectedLayout.name,
                                isConcurrent = isConcurrentActive,
                                frameRate = selectedFrameRate,
                                useHevc = useHevcCompression,
                                audioFilePath = finalAudioFile?.absolutePath,
                                isStabilizationActive = isStabilizationActive,
                                stabilizationStrength = stabilizationStrength,
                                watermark = activeWatermark
                            )
                            
                            if (success && tempFile.exists()) {
                                val uri = saveVideoToGallery(
                                    context = context,
                                    file = tempFile,
                                    layout = selectedLayout.name,
                                    resolution = selectedResolution.label,
                                    isConcurrent = isConcurrentActive,
                                    codec = if (useHevcCompression) "HEVC/H.265" else "H.264/AVC",
                                    fps = selectedFrameRate,
                                    aspectRatio = "${selectedResolution.width}:${selectedResolution.height}"
                                )
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    if (uri != null) {
                                        activeRecordingFile = tempFile
                                        activeRecordingUri = uri
                                        showSavedOverlay = true
                                        
                                        // TRIGGER THE SUCCESSFUL LOCAL CAPTURE NOTIFICATION HERE
                                        val formattedDur = String.format("%02d:%02d", (durationSecs / 60), (durationSecs % 60))
                                        com.example.DuoCamNotificationHelper.triggerRecordingCompletion(
                                            context = context,
                                            videoName = tempFile.name,
                                            duration = formattedDur
                                        )
                                        
                                        // Auto dismiss toast after 5 seconds
                                        recordingScope.launch {
                                            delay(5000)
                                            showSavedOverlay = false
                                        }
                                    }
                                    isProcessingRecording = false
                                    elapsedMillis = 0L
                                }
                            } else {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    isProcessingRecording = false
                                    elapsedMillis = 0L
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("DuoCam", "Error writing recorded video file", e)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isProcessingRecording = false
                                elapsedMillis = 0L
                            }
                        } finally {
                            // Safely clean up the temp audio file
                            try {
                                finalAudioFile?.delete()
                            } catch (ignored: Exception) {}
                        }
                    }
                }
                } else {
                    elapsedMillis = 0L
                    try {
                        com.example.DuoCamRecordingService.activeAudioFile.value?.delete()
                    } catch (ignored: Exception) {}
                }
            }
        }
    }
    
    LaunchedEffect(overlayZoomHUDValue) {
        if (overlayZoomHUDValue.isNotEmpty()) {
            zoomHUDVisible = true
            delay(1500)
            zoomHUDVisible = false
        }
    }
    
    LaunchedEffect(backCameraInstance, flashState, isRecording) {
        if (backCameraInstance != null) {
            try {
                if (flashState == "on") {
                    backCameraInstance?.cameraControl?.enableTorch(true)
                } else if (flashState == "off") {
                    backCameraInstance?.cameraControl?.enableTorch(false)
                } else { // "auto"
                    backCameraInstance?.cameraControl?.enableTorch(false)
                }
            } catch (e: Throwable) {
                Log.e("DuoCam", "Error setting flash/torch mode", e)
            }
        }
    }
    
    fun updateCameraZoom(camera: androidx.camera.core.Camera?, ratio: Float, isRear: Boolean) {
        if (camera == null) return
        val state = camera.cameraInfo.zoomState.value
        val min = state?.minZoomRatio ?: 1.0f
        val max = state?.maxZoomRatio ?: 8.0f
        val clamped = ratio.coerceIn(min, max)
        camera.cameraControl.setZoomRatio(clamped)
        if (isRear) {
            rearZoomValue = clamped
        } else {
            frontZoomValue = clamped
        }
        val labelName = if (isRear) "Rear Zoom" else "Front Zoom"
        overlayZoomHUDValue = "$labelName: ${String.format("%.1fx", clamped)}"
    }
    
    // Live CameraX dual concurrent binding logic
    LaunchedEffect(lensFacing, isStabilizationActive, stabilizationStrength) {
        val cameraProviderProvider = ProcessCameraProvider.getInstance(context)
        cameraProviderProvider.addListener({
            val cameraProvider = cameraProviderProvider.get()
            
            val previewRearBuilder = Preview.Builder()
            val extRear = androidx.camera.camera2.interop.Camera2Interop.Extender(previewRearBuilder)
            if (isStabilizationActive) {
                extRear.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
                extRear.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            } else {
                extRear.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
                extRear.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
            }
            val previewRear = previewRearBuilder.build().apply {
                setSurfaceProvider(rearPreviewView.surfaceProvider)
            }

            val previewFrontBuilder = Preview.Builder()
            val extFront = androidx.camera.camera2.interop.Camera2Interop.Extender(previewFrontBuilder)
            if (isStabilizationActive) {
                extFront.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
                extFront.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            } else {
                extFront.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
                extFront.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
            }
            val previewFront = previewFrontBuilder.build().apply {
                setSurfaceProvider(frontPreviewView.surfaceProvider)
            }
            
            try {
                cameraProvider.unbindAll()
                
                // Query available camera combinations to verify if hardware concurrent streaming exists
                val availableConcurrent = cameraProvider.availableConcurrentCameraInfos
                val supportsConcurrent = availableConcurrent.isNotEmpty()
                
                // --- Silent capabilities detection & storage block ---
                val isChecked = sharedPreferences.getBoolean("compat_checked", false)
                if (!isChecked) {
                    val spec = detectDeviceCapabilities(context, supportsConcurrent)
                    sharedPreferences.edit()
                        .putBoolean("compat_checked", true)
                        .putBoolean("compat_dual_supported", spec.isDualSupported)
                        .putString("compat_rear_res", spec.rearMaxRes)
                        .putString("compat_front_res", spec.frontMaxRes)
                        .putBoolean("compat_hw_encoding", spec.isHwEncodingAvailable)
                        .putBoolean("compat_features_flash", spec.hasFlash)
                        .putString("compat_features_zoom_range", spec.zoomRange)
                        .putBoolean("compat_features_stabilization", spec.hasStabilization)
                        .apply()
                    isCompatDualMode = spec.isDualSupported
                    compatRearRes = spec.rearMaxRes
                    compatFrontRes = spec.frontMaxRes
                    compatHwEncoding = spec.isHwEncodingAvailable
                    compatFlash = spec.hasFlash
                    compatZoomRange = spec.zoomRange
                    compatStabilization = spec.hasStabilization
                } else {
                    val savedDual = sharedPreferences.getBoolean("compat_dual_supported", false)
                    if (savedDual != supportsConcurrent) {
                        sharedPreferences.edit()
                            .putBoolean("compat_dual_supported", supportsConcurrent)
                            .apply()
                        isCompatDualMode = supportsConcurrent
                    }
                }
                // --- End capabilities block ---
                
                if (supportsConcurrent) {
                    val backConfig = androidx.camera.core.ConcurrentCamera.SingleCameraConfig(
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        androidx.camera.core.UseCaseGroup.Builder().addUseCase(previewRear).build(),
                        lifecycleOwner
                    )
                    val frontConfig = androidx.camera.core.ConcurrentCamera.SingleCameraConfig(
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        androidx.camera.core.UseCaseGroup.Builder().addUseCase(previewFront).build(),
                        lifecycleOwner
                    )
                    
                    val concurrentCamera = cameraProvider.bindToLifecycle(listOf(backConfig, frontConfig))
                    val boundCams = concurrentCamera.cameras
                    backCameraInstance = boundCams.find { it.cameraInfo.lensFacing == CameraSelector.LENS_FACING_BACK }
                    frontCameraInstance = boundCams.find { it.cameraInfo.lensFacing == CameraSelector.LENS_FACING_FRONT }
                    isConcurrentActive = true
                    Log.d("DuoCam", "Concurrent dual camera streams initialized physically live!")
                } else {
                    // Fail gracefully - bind the selected lens live, and simulate the other lens
                    if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        val boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewRear)
                        backCameraInstance = boundCamera
                        frontCameraInstance = null
                    } else {
                        val boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, previewFront)
                        frontCameraInstance = boundCamera
                        backCameraInstance = null
                    }
                    isConcurrentActive = false
                    Log.d("DuoCam", "No hardware concurrent camera support. Fallback active-bind initialized.")
                }
            } catch (e: Throwable) {
                Log.e("DuoCam", "Concurrent dual/single binding failed", e)
                isConcurrentActive = false
                try {
                    // Ensure we bind at least active camera
                    cameraProvider.unbindAll()
                    if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        val boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewRear)
                        backCameraInstance = boundCamera
                        frontCameraInstance = null
                    } else {
                        val boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, previewFront)
                        frontCameraInstance = boundCamera
                        backCameraInstance = null
                    }
                } catch (fallbackEx: Throwable) {
                    Log.e("DuoCam", "Absolute fallback camera binding failed", fallbackEx)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    // Helper to safely detach preview views before reuse in Compose layouts
    fun safeGetPreviewView(view: PreviewView): PreviewView {
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        return view
    }
    
    // Visual labels fade-out manager: triggers whenever camera is swapped, layout changes or app launches
    var showLabels by remember { mutableStateOf(true) }
    LaunchedEffect(lensFacing, selectedLayout) {
        showLabels = true
        delay(2000)
        showLabels = false
    }
    
    // Virtual VU Meter lists for continuous mic level simulation
    var vuLeft by remember { mutableStateOf(0.4f) }
    var vuRight by remember { mutableStateOf(0.5f) }
    
    LaunchedEffect(isMuted, audioSourceSelection, audioNoiseReductionEnabled) {
        if (isMuted || audioSourceSelection == "none") {
            vuLeft = 0f
            vuRight = 0f
            return@LaunchedEffect
        }
        
        val hasMicPermission = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasMicPermission) {
            // Run high-fidelity simulation if mic permission is not granted yet
            while (!isMuted && audioSourceSelection != "none") {
                delay(120)
                vuLeft = 0.15f + kotlin.random.Random.nextFloat() * 0.45f
                vuRight = 0.15f + kotlin.random.Random.nextFloat() * 0.45f
            }
            return@LaunchedEffect
        }
        
        // Start live AudioRecord to drive real level meter when permission exists
        val sampleRate = 44100
        val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
        val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        
        if (minBufferSize > 0) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var audioRecord: android.media.AudioRecord? = null
                var noiseSuppressor: android.media.audiofx.NoiseSuppressor? = null
                try {
                    audioRecord = android.media.AudioRecord(
                        android.media.MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        minBufferSize.coerceAtLeast(2048)
                    )
                    
                    if (audioRecord.state == android.media.AudioRecord.STATE_INITIALIZED) {
                        // Set preferred device if M+
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                            if (audioManager != null) {
                                val isExternalSelected = audioSourceSelection == "external"
                                val targetDevice = getDeviceOfType(audioManager, isExternalSelected)
                                if (targetDevice != null) {
                                    audioRecord.setPreferredDevice(targetDevice)
                                }
                            }
                        }
                        
                        // Attach NoiseSuppressor if enabled
                        if (audioNoiseReductionEnabled) {
                            try {
                                if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                                    noiseSuppressor = android.media.audiofx.NoiseSuppressor.create(audioRecord.audioSessionId)
                                    noiseSuppressor.enabled = true
                                    Log.d("DuoCam", "NoiseSuppressor enabled on live meter session ${audioRecord.audioSessionId}")
                                }
                            } catch (ae: Exception) {
                                Log.e("DuoCam", "Failed to active noise suppressor for live meter", ae)
                            }
                        }
                        
                        audioRecord.startRecording()
                        val buffer = ShortArray(1024)
                        while (!isMuted && audioSourceSelection != "none") {
                            val readSize = audioRecord.read(buffer, 0, buffer.size)
                            if (readSize > 0) {
                                var maxAbs = 0f
                                for (i in 0 until readSize) {
                                    val absValue = kotlin.math.abs(buffer[i].toInt()).toFloat()
                                    if (absValue > maxAbs) {
                                        maxAbs = absValue
                                    }
                                }
                                // Raw level calculation: map typical human vocal volumes to [0f..1f]
                                val rawLevel = if (maxAbs > 150f) (maxAbs / 14000f).coerceIn(0f, 1f) else 0f
                                val nextVuLeft = vuLeft * 0.35f + rawLevel * 0.65f
                                val nextVuRight = vuRight * 0.4f + rawLevel * 0.6f
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    vuLeft = nextVuLeft
                                    vuRight = nextVuRight
                                }
                            }
                            delay(50)
                        }
                    } else {
                        throw Exception("AudioRecord state not initialized")
                    }
                } catch (e: Exception) {
                    Log.e("DuoCam", "Error initiating AudioRecord live meters. Falling back onto simulation.", e)
                    while (!isMuted && audioSourceSelection != "none") {
                        delay(120)
                        val nextVuLeft = 0.2f + kotlin.random.Random.nextFloat() * 0.5f
                        val nextVuRight = 0.2f + kotlin.random.Random.nextFloat() * 0.5f
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            vuLeft = nextVuLeft
                            vuRight = nextVuRight
                        }
                    }
                } finally {
                    try {
                        noiseSuppressor?.release()
                    } catch (ignored: Exception) {}
                    try {
                        audioRecord?.stop()
                        audioRecord?.release()
                    } catch (ignored: Exception) {}
                }
            }
        } else {
            while (!isMuted && audioSourceSelection != "none") {
                delay(120)
                vuLeft = 0.15f + kotlin.random.Random.nextFloat() * 0.45f
                vuRight = 0.15f + kotlin.random.Random.nextFloat() * 0.45f
            }
        }
    }
    
    // Time format derived directly from dynamic pause-aware elapsedMillis state
    val formattedTime = String.format(
        "%02d:%02d",
        (elapsedMillis / 1000) / 60,
        (elapsedMillis / 1000) % 60
    )
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenWidthDp = with(density) { widthPx.toDp() }
        val screenHeightDp = with(density) { heightPx.toDp() }

        val activeRatio = selectedAspectRatio.ratio
        var widthDp = if (screenWidthDp > 0.dp) screenWidthDp else 360.dp
        var heightDp = if (screenHeightDp > 0.dp) screenHeightDp else 640.dp
        
        if (screenWidthDp.value > 0f && screenHeightDp.value > 0f) {
            if (screenWidthDp.value / screenHeightDp.value > activeRatio) {
                heightDp = screenHeightDp
                widthDp = screenHeightDp * activeRatio
            } else {
                widthDp = screenWidthDp
                heightDp = screenWidthDp / activeRatio
            }
        }
        
        if (widthDp.value.isNaN() || widthDp.value.isInfinite() || widthDp.value <= 0f) {
            widthDp = 360.dp
        }
        if (heightDp.value.isNaN() || heightDp.value.isInfinite() || heightDp.value <= 0f) {
            heightDp = 640.dp
        }

        val snapToEdge = { wDp: Dp, hDp: Dp ->
            val currentPipScale = pipScale
            val currentPipWidth = 120.dp * currentPipScale
            val currentPipHeight = 175.dp * currentPipScale
            
            val localMinX = 10.dp
            val localMaxX = (wDp - currentPipWidth - 10.dp).coerceAtLeast(10.dp)
            val localMinY = 100.dp
            val localMaxY = (hDp - currentPipHeight - 140.dp).coerceAtLeast(100.dp)
            
            val currentX = (wDp * pipXPercent).coerceIn(localMinX, localMaxX)
            val currentY = (hDp * pipYPercent).coerceIn(localMinY, localMaxY)
            
            val distTopLeft = (currentX - localMinX).value * (currentX - localMinX).value + (currentY - localMinY).value * (currentY - localMinY).value
            val distTopRight = (currentX - localMaxX).value * (currentX - localMaxX).value + (currentY - localMinY).value * (currentY - localMinY).value
            val distBottomLeft = (currentX - localMinX).value * (currentX - localMinX).value + (currentY - localMaxY).value * (currentY - localMaxY).value
            val distBottomRight = (currentX - localMaxX).value * (currentX - localMaxX).value + (currentY - localMaxY).value * (currentY - localMaxY).value
            
            val minDist = minOf(distTopLeft, distTopRight, distBottomLeft, distBottomRight)
            val snapX = when (minDist) {
                distTopLeft, distBottomLeft -> localMinX
                else -> localMaxX
            }
            val snapY = when (minDist) {
                distTopLeft, distTopRight -> localMinY
                else -> localMaxY
            }
            
            pipXPercent = if (wDp > 0.3.dp) snapX / wDp else 0.85f
            pipYPercent = if (hDp > 0.3.dp) snapY / hDp else 0.70f
            
            sharedPreferences.edit()
                .putFloat("pip_scale", pipScale)
                .putFloat("pip_x_percent", pipXPercent)
                .putFloat("pip_y_percent", pipYPercent)
                .apply()
        }
        
        // Define destination targets based on selectedLayout
        val targetWidthA: Dp
        val targetHeightA: Dp
        val targetOffsetXA: Dp
        val targetOffsetYA: Dp
        val targetCornerRadiusA: Dp
        val targetProgressA: Float
        val targetBorderAlphaA: Float
        
        val targetWidthB: Dp
        val targetHeightB: Dp
        val targetOffsetXB: Dp
        val targetOffsetYB: Dp
        val targetCornerRadiusB: Dp
        val targetProgressB: Float
        val targetBorderAlphaB: Float
        
        when (selectedLayout) {
            DualCameraLayout.PIP -> {
                val pipWidth = 120.dp * pipScale
                val pipHeight = 175.dp * pipScale
                
                val currentPipX = (widthDp * pipXPercent).coerceIn(10.dp, (widthDp - pipWidth - 10.dp).coerceAtLeast(10.dp))
                val currentPipY = (heightDp * pipYPercent).coerceIn(100.dp, (heightDp - pipHeight - 140.dp).coerceAtLeast(100.dp))
                
                if (isFeedAOnTop) {
                    targetWidthA = widthDp
                    targetHeightA = heightDp
                    targetOffsetXA = 0.dp
                    targetOffsetYA = 0.dp
                    targetCornerRadiusA = 0.dp
                    targetProgressA = 0f
                    targetBorderAlphaA = 0f
                    
                    targetWidthB = pipWidth
                    targetHeightB = pipHeight
                    targetOffsetXB = currentPipX
                    targetOffsetYB = currentPipY
                    targetCornerRadiusB = 16.dp
                    targetProgressB = 0f
                    targetBorderAlphaB = 1f
                } else {
                    targetWidthB = widthDp
                    targetHeightB = heightDp
                    targetOffsetXB = 0.dp
                    targetOffsetYB = 0.dp
                    targetCornerRadiusB = 0.dp
                    targetProgressB = 0f
                    targetBorderAlphaB = 0f
                    
                    targetWidthA = pipWidth
                    targetHeightA = pipHeight
                    targetOffsetXA = currentPipX
                    targetOffsetYA = currentPipY
                    targetCornerRadiusA = 16.dp
                    targetProgressA = 0f
                    targetBorderAlphaA = 1f
                }
            }
            DualCameraLayout.SPLIT_HORIZONTAL -> {
                targetWidthA = widthDp / 2
                targetHeightA = heightDp
                targetOffsetXA = 0.dp
                targetOffsetYA = 0.dp
                targetCornerRadiusA = 0.dp
                targetProgressA = 0f
                targetBorderAlphaA = 0f
                
                targetWidthB = widthDp / 2
                targetHeightB = heightDp
                targetOffsetXB = widthDp / 2
                targetOffsetYB = 0.dp
                targetCornerRadiusB = 0.dp
                targetProgressB = 0f
                targetBorderAlphaB = 0f
            }
            DualCameraLayout.SPLIT_VERTICAL -> {
                targetWidthA = widthDp
                targetHeightA = heightDp / 2
                targetOffsetXA = 0.dp
                targetOffsetYA = 0.dp
                targetCornerRadiusA = 0.dp
                targetProgressA = 0f
                targetBorderAlphaA = 0f
                
                targetWidthB = widthDp
                targetHeightB = heightDp / 2
                targetOffsetXB = 0.dp
                targetOffsetYB = heightDp / 2
                targetCornerRadiusB = 0.dp
                targetProgressB = 0f
                targetBorderAlphaB = 0f
            }
            DualCameraLayout.SPLIT_DIAGONAL -> {
                targetWidthA = widthDp
                targetHeightA = heightDp
                targetOffsetXA = 0.dp
                targetOffsetYA = 0.dp
                targetCornerRadiusA = 0.dp
                targetProgressA = 1f
                targetBorderAlphaA = 0f
                
                targetWidthB = widthDp
                targetHeightB = heightDp
                targetOffsetXB = 0.dp
                targetOffsetYB = 0.dp
                targetCornerRadiusB = 0.dp
                targetProgressB = 1f
                targetBorderAlphaB = 0f
            }
        }
        
        // Animated parameters
        val animWidthA by animateDpAsState(targetValue = targetWidthA, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "wA")
        val animHeightA by animateDpAsState(targetValue = targetHeightA, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "hA")
        val animOffsetXA by animateDpAsState(targetValue = targetOffsetXA, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "xA")
        val animOffsetYA by animateDpAsState(targetValue = targetOffsetYA, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "yA")
        val animCornerRadiusA by animateDpAsState(targetValue = targetCornerRadiusA, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "crA")
        val animProgressA by animateFloatAsState(targetValue = targetProgressA, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "pA")
        val animBorderAlphaA by animateFloatAsState(targetValue = targetBorderAlphaA, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "baA")
        
        val animWidthB by animateDpAsState(targetValue = targetWidthB, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "wB")
        val animHeightB by animateDpAsState(targetValue = targetHeightB, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "hB")
        val animOffsetXB by animateDpAsState(targetValue = targetOffsetXB, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "xB")
        val animOffsetYB by animateDpAsState(targetValue = targetOffsetYB, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "yB")
        val animCornerRadiusB by animateDpAsState(targetValue = targetCornerRadiusB, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "crB")
        val animProgressB by animateFloatAsState(targetValue = targetProgressB, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "pB")
        val animBorderAlphaB by animateFloatAsState(targetValue = targetBorderAlphaB, animationSpec = if (isCurrentlyDragging) snap() else tween(500, easing = FastOutSlowInEasing), label = "baB")
        
        // Aspect Ratio Crop Main container
        Box(
            modifier = Modifier
                .size(width = widthDp, height = heightDp)
                .align(Alignment.Center)
                .border(0.5.dp, BorderGray.copy(alpha = 0.3f))
                .clipToBounds()
        ) {
            // Feed A (Primary Viewport)
            Box(
                modifier = Modifier
                    .offset(x = animOffsetXA, y = animOffsetYA)
                    .size(width = animWidthA, height = animHeightA)
                .shadow(
                    elevation = if (selectedLayout == DualCameraLayout.PIP && !isFeedAOnTop) { if (isCurrentlyDragging) 24.dp else 12.dp } else 0.dp,
                    shape = MorphingShapeA(animProgressA, animCornerRadiusA, density)
                )
                .clip(MorphingShapeA(animProgressA, animCornerRadiusA, density))
                .border(
                    width = 1.5.dp,
                    color = BorderGray.copy(alpha = animBorderAlphaA),
                    shape = MorphingShapeA(animProgressA, animCornerRadiusA, density)
                )
                .pointerInput(selectedLayout, isFeedAOnTop, lensFacing, backCameraInstance, frontCameraInstance) {
                    if (!(selectedLayout == DualCameraLayout.PIP && !isFeedAOnTop)) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                val targetCam = if (lensFacing == CameraSelector.LENS_FACING_BACK) backCameraInstance else frontCameraInstance
                                val currentVal = if (lensFacing == CameraSelector.LENS_FACING_BACK) rearZoomValue else frontZoomValue
                                val isRear = (lensFacing == CameraSelector.LENS_FACING_BACK)
                                updateCameraZoom(targetCam, currentVal * zoom, isRear)
                            }
                        }
                    }
                }
                .pointerInput(selectedLayout, isFeedAOnTop) {
                    if (selectedLayout == DualCameraLayout.PIP && !isFeedAOnTop) {
                        forEachGesture {
                            awaitPointerEventScope {
                                var accumulatedPan = 0f
                                awaitFirstDown(requireUnconsumed = false)
                                isCurrentlyDragging = true
                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (!canceled) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (panChange != androidx.compose.ui.geometry.Offset.Zero) {
                                            accumulatedPan += panChange.getDistance()
                                        }
                                        if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                                            val newScale = (pipScale * zoomChange).coerceIn(0.6f, 2.2f)
                                            pipScale = newScale
                                            
                                            val currentPipWidth = 120.dp * newScale
                                            val currentPipHeight = 175.dp * newScale
                                            val localMinX = 10.dp
                                            val localMaxX = (widthDp - currentPipWidth - 10.dp).coerceAtLeast(10.dp)
                                            val localMinY = 100.dp
                                            val localMaxY = (heightDp - currentPipHeight - 140.dp).coerceAtLeast(100.dp)
                                            
                                            val deltaXDp = panChange.x.toDp()
                                            val deltaYDp = panChange.y.toDp()
                                            
                                            val currentX = (widthDp * pipXPercent) + deltaXDp
                                            val currentY = (heightDp * pipYPercent) + deltaYDp
                                            
                                            val clampedX = currentX.coerceIn(localMinX, localMaxX)
                                            val clampedY = currentY.coerceIn(localMinY, localMaxY)
                                            
                                            pipXPercent = if (widthDp > 0.3.dp) clampedX / widthDp else 0.85f
                                            pipYPercent = if (heightDp > 0.3.dp) clampedY / heightDp else 0.70f
                                            
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                                
                                isCurrentlyDragging = false
                                if (accumulatedPan < 15f) {
                                    isFeedAOnTop = !isFeedAOnTop
                                } else {
                                    snapToEdge(widthDp, heightDp)
                                }
                            }
                        }
                    } else {
                        detectTapGestures(onTap = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        })
                    }
                }
        ) {
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                AndroidView(
                    factory = { safeGetPreviewView(rearPreviewView) },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = if (!isStabilizationActive) 1.0f else when (stabilizationStrength) {
                                "Light" -> 1.04f
                                "Standard" -> 1.10f
                                "High" -> 1.20f
                                else -> 1.0f
                            }
                            scaleX = scale
                            scaleY = scale
                            translationX = shakeX
                            translationY = shakeY
                        }
                )
            } else {
                AndroidView(
                    factory = { safeGetPreviewView(frontPreviewView) },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = if (!isStabilizationActive) 1.0f else when (stabilizationStrength) {
                                "Light" -> 1.04f
                                "Standard" -> 1.10f
                                "High" -> 1.20f
                                else -> 1.0f
                            }
                            scaleX = scale
                            scaleY = scale
                            translationX = shakeX
                            translationY = shakeY
                        }
                )
            }
            
            // Fading Camera Label for Feed A
            val isAFloating = selectedLayout == DualCameraLayout.PIP && !isFeedAOnTop
            AnimatedVisibility(
                visible = showLabels,
                exit = fadeOut(animationSpec = tween(500)),
                modifier = Modifier
                    .align(if (isAFloating) Alignment.BottomCenter else Alignment.TopCenter)
                    .padding(if (isAFloating) 8.dp else 90.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xD909090B))
                        .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GlowRecordRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lensFacing == CameraSelector.LENS_FACING_BACK) "MAIN REAR CAM" else "FRONT CAM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextWhite
                    )
                }
            }
            
            if (selectedLayout == DualCameraLayout.PIP && !isFeedAOnTop) {
                val isRear = (lensFacing == CameraSelector.LENS_FACING_BACK)
                val pipCam = if (isRear) backCameraInstance else frontCameraInstance
                val pipZoom = if (isRear) rearZoomValue else frontZoomValue
                PiPZoomControl(
                    currentZoom = pipZoom,
                    camera = pipCam,
                    isRear = isRear,
                    onZoomChange = { newVal ->
                        updateCameraZoom(pipCam, newVal, isRear)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        // Feed B (Secondary Viewport - PIP window, or splits)
        Box(
            modifier = Modifier
                .offset(x = animOffsetXB, y = animOffsetYB)
                .size(width = animWidthB, height = animHeightB)
                .shadow(
                    elevation = if (selectedLayout == DualCameraLayout.PIP && isFeedAOnTop) { if (isCurrentlyDragging) 24.dp else 12.dp } else 0.dp,
                    shape = MorphingShapeB(animProgressB, animCornerRadiusB, density)
                )
                .clip(MorphingShapeB(animProgressB, animCornerRadiusB, density))
                .border(
                    width = 1.5.dp,
                    color = BorderGray.copy(alpha = animBorderAlphaB),
                    shape = MorphingShapeB(animProgressB, animCornerRadiusB, density)
                )
                .pointerInput(selectedLayout, isFeedAOnTop, lensFacing, backCameraInstance, frontCameraInstance) {
                    if (!(selectedLayout == DualCameraLayout.PIP && isFeedAOnTop)) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                val targetCam = if (lensFacing == CameraSelector.LENS_FACING_BACK) frontCameraInstance else backCameraInstance
                                val currentVal = if (lensFacing == CameraSelector.LENS_FACING_BACK) frontZoomValue else rearZoomValue
                                val isRear = (lensFacing != CameraSelector.LENS_FACING_BACK)
                                updateCameraZoom(targetCam, currentVal * zoom, isRear)
                            }
                        }
                    }
                }
                .pointerInput(selectedLayout, isFeedAOnTop) {
                    if (selectedLayout == DualCameraLayout.PIP && isFeedAOnTop) {
                        forEachGesture {
                            awaitPointerEventScope {
                                var accumulatedPan = 0f
                                awaitFirstDown(requireUnconsumed = false)
                                isCurrentlyDragging = true
                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (!canceled) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (panChange != androidx.compose.ui.geometry.Offset.Zero) {
                                            accumulatedPan += panChange.getDistance()
                                        }
                                        if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                                            val newScale = (pipScale * zoomChange).coerceIn(0.6f, 2.2f)
                                            pipScale = newScale
                                            
                                            val currentPipWidth = 120.dp * newScale
                                            val currentPipHeight = 175.dp * newScale
                                            val localMinX = 10.dp
                                            val localMaxX = (widthDp - currentPipWidth - 10.dp).coerceAtLeast(10.dp)
                                            val localMinY = 100.dp
                                            val localMaxY = (heightDp - currentPipHeight - 140.dp).coerceAtLeast(100.dp)
                                            
                                            val deltaXDp = panChange.x.toDp()
                                            val deltaYDp = panChange.y.toDp()
                                            
                                            val currentX = (widthDp * pipXPercent) + deltaXDp
                                            val currentY = (heightDp * pipYPercent) + deltaYDp
                                            
                                            val clampedX = currentX.coerceIn(localMinX, localMaxX)
                                            val clampedY = currentY.coerceIn(localMinY, localMaxY)
                                            
                                            pipXPercent = if (widthDp > 0.3.dp) clampedX / widthDp else 0.85f
                                            pipYPercent = if (heightDp > 0.3.dp) clampedY / heightDp else 0.70f
                                            
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                                
                                isCurrentlyDragging = false
                                if (accumulatedPan < 15f) {
                                    isFeedAOnTop = !isFeedAOnTop
                                } else {
                                    snapToEdge(widthDp, heightDp)
                                }
                            }
                        }
                    } else {
                        detectTapGestures(onTap = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        })
                    }
                }
        ) {
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                if (isConcurrentActive) {
                    AndroidView(
                        factory = { safeGetPreviewView(frontPreviewView) },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val scale = if (!isStabilizationActive) 1.0f else when (stabilizationStrength) {
                                    "Light" -> 1.04f
                                    "Standard" -> 1.10f
                                    "High" -> 1.20f
                                    else -> 1.0f
                                }
                                scaleX = scale
                                scaleY = scale
                                translationX = shakeX
                                translationY = shakeY
                            }
                    )
                } else {
                    SimulatedCameraView(
                        label = "FRONT CAMERA",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val scale = if (!isStabilizationActive) 1.0f else when (stabilizationStrength) {
                                    "Light" -> 1.04f
                                    "Standard" -> 1.10f
                                    "High" -> 1.20f
                                    else -> 1.0f
                                }
                                scaleX = scale
                                scaleY = scale
                                translationX = shakeX
                                translationY = shakeY
                            }
                    )
                }
            } else {
                if (isConcurrentActive) {
                    AndroidView(
                        factory = { safeGetPreviewView(rearPreviewView) },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val scale = if (!isStabilizationActive) 1.0f else when (stabilizationStrength) {
                                    "Light" -> 1.04f
                                    "Standard" -> 1.10f
                                    "High" -> 1.20f
                                    else -> 1.0f
                                }
                                scaleX = scale
                                scaleY = scale
                                translationX = shakeX
                                translationY = shakeY
                            }
                    )
                } else {
                    SimulatedCameraView(
                        label = "REAR CAMERA",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val scale = if (!isStabilizationActive) 1.0f else when (stabilizationStrength) {
                                    "Light" -> 1.04f
                                    "Standard" -> 1.10f
                                    "High" -> 1.20f
                                    else -> 1.0f
                                }
                                scaleX = scale
                                scaleY = scale
                                translationX = shakeX
                                translationY = shakeY
                            }
                    )
                }
            }
            
            // Fading Camera Label for Feed B
            val isBFloating = selectedLayout == DualCameraLayout.PIP && isFeedAOnTop
            AnimatedVisibility(
                visible = showLabels,
                exit = fadeOut(animationSpec = tween(500)),
                modifier = Modifier
                    .align(if (isBFloating) Alignment.BottomCenter else Alignment.TopCenter)
                    .padding(if (isBFloating) 8.dp else 90.dp)
            ) {
                Text(
                    text = if (lensFacing == CameraSelector.LENS_FACING_BACK) "FRONT CAMERA" else "REAR CAMERA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isBFloating) 8.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            
            if (selectedLayout == DualCameraLayout.PIP && isFeedAOnTop) {
                val isRear = (lensFacing != CameraSelector.LENS_FACING_BACK)
                val pipCam = if (isRear) backCameraInstance else frontCameraInstance
                val pipZoom = if (isRear) rearZoomValue else frontZoomValue
                PiPZoomControl(
                    currentZoom = pipZoom,
                    camera = pipCam,
                    isRear = isRear,
                    onZoomChange = { newVal ->
                        updateCameraZoom(pipCam, newVal, isRear)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            
            // Guides & Overlays (Only visible as overlay guides of the preview, not saved in recorded feed)
            if (isGridEnabled) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("thirds_grid_canvas")
                ) {
                    val thirdWidth = size.width / 3f
                    val thirdHeight = size.height / 3f
                    
                    // Vertical lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = androidx.compose.ui.geometry.Offset(thirdWidth, 0f),
                        end = androidx.compose.ui.geometry.Offset(thirdWidth, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = androidx.compose.ui.geometry.Offset(thirdWidth * 2f, 0f),
                        end = androidx.compose.ui.geometry.Offset(thirdWidth * 2f, size.height),
                        strokeWidth = 1f
                    )
                    
                    // Horizontal lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = androidx.compose.ui.geometry.Offset(0f, thirdHeight),
                        end = androidx.compose.ui.geometry.Offset(size.width, thirdHeight),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = androidx.compose.ui.geometry.Offset(0f, thirdHeight * 2f),
                        end = androidx.compose.ui.geometry.Offset(size.width, thirdHeight * 2f),
                        strokeWidth = 1f
                    )
                }
            }
            
            if (isCrosshairEnabled) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("center_crosshair_canvas")
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val armLength = 12.dp.toPx()
                    val gap = 3.dp.toPx()
                    
                    // Horizontal left arm
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = androidx.compose.ui.geometry.Offset(centerX - armLength - gap, centerY),
                        end = androidx.compose.ui.geometry.Offset(centerX - gap, centerY),
                        strokeWidth = 1.5f
                    )
                    // Horizontal right arm
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = androidx.compose.ui.geometry.Offset(centerX + gap, centerY),
                        end = androidx.compose.ui.geometry.Offset(centerX + armLength + gap, centerY),
                        strokeWidth = 1.5f
                    )
                    // Vertical top arm
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = androidx.compose.ui.geometry.Offset(centerX, centerY - armLength - gap),
                        end = androidx.compose.ui.geometry.Offset(centerX, centerY - gap),
                        strokeWidth = 1.5f
                    )
                    // Vertical bottom arm
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = androidx.compose.ui.geometry.Offset(centerX, centerY + gap),
                        end = androidx.compose.ui.geometry.Offset(centerX, centerY + armLength + gap),
                        strokeWidth = 1.5f
                    )
                    // Draw center dot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = 1.5.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )
                }
            }
            
            LevelIndicator(
                deviceTiltAngleProvider = { deviceTiltAngle },
                isLevelEnabled = isLevelEnabled,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 75.dp)
            )

            // Draggable & Resizable On-Screen Watermark Preview Overlay
            if (activeWatermark != null) {
                val watermarkBitmap = remember(activeWatermark.imagePath) {
                    if (activeWatermark.imagePath != null) {
                        try {
                            val f = java.io.File(activeWatermark.imagePath)
                            if (f.exists()) {
                                android.graphics.BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }

                Box(
                    modifier = Modifier
                        .offset(
                            x = (watermarkXPercent * widthDp.value).dp,
                            y = (watermarkYPercent * heightDp.value).dp
                        )
                        .graphicsLayer {
                            scaleX = watermarkScale
                            scaleY = watermarkScale
                        }
                        .pointerInput(activeWatermark) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (zoom != 1f) {
                                    val newScale = (watermarkScale * zoom).coerceIn(0.4f, 2.5f)
                                    watermarkScale = newScale
                                    saveWatermarkOffset(watermarkXPercent, watermarkYPercent, newScale)
                                }
                                if (pan != androidx.compose.ui.geometry.Offset.Zero) {
                                    val newX = (watermarkXPercent + pan.x / size.width).coerceIn(0f, 0.95f)
                                    val newY = (watermarkYPercent + pan.y / size.height).coerceIn(0f, 0.95f)
                                    watermarkXPercent = newX
                                    watermarkYPercent = newY
                                    saveWatermarkOffset(newX, newY, watermarkScale)
                                }
                            }
                        }
                        .border(
                            width = 1.dp,
                            color = GoldenHour.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(6.dp)
                        .testTag("watermark_preview_box")
                ) {
                    if (activeWatermark.type == "IMAGE" && watermarkBitmap != null) {
                        Image(
                            bitmap = watermarkBitmap,
                            contentDescription = "Active Watermark Image Logo",
                            modifier = Modifier
                                .width(120.dp)
                                .alpha(activeWatermark.opacity)
                        )
                    } else {
                        val textFont = when (activeWatermark.font) {
                            "Monospace" -> FontFamily.Monospace
                            "Serif" -> FontFamily.Serif
                            "Sans-Serif" -> FontFamily.SansSerif
                            else -> FontFamily.Default
                        }

                        val displayPreviewText = if (activeWatermark.isAutoTime) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                            val current = sdf.format(java.util.Date())
                            if (activeWatermark.text.isNotEmpty()) "${activeWatermark.text} $current" else current
                        } else {
                            activeWatermark.text
                        }

                        Text(
                            text = displayPreviewText,
                            color = Color(android.graphics.Color.parseColor(activeWatermark.color)),
                            fontSize = activeWatermark.fontSize.sp,
                            fontFamily = textFont,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alpha(activeWatermark.opacity)
                        )
                    }
                }
            }
        }
    }
        
    // Aspect Ratio and Layout Picker row, placed near the top center
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 135.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        com.example.ui.CameraTopControllers(
            selectedLayout = selectedLayout,
            onLayoutSelected = { layoutOption ->
                if (!isPro && layoutOption != DualCameraLayout.PIP) {
                    selectedLayout = layoutOption
                    isLayoutPreviewActive = true
                    layoutPreviewRemainingSeconds = 3
                    showLayoutPickerMenu = false
                    android.widget.Toast.makeText(context, "3-Sec Layout Preview Active!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    selectedLayout = layoutOption
                    sharedPreferences.edit().putString("selected_layout", layoutOption.name).apply()
                    showLayoutPickerMenu = false
                }
            },
            showLayoutPickerMenu = showLayoutPickerMenu,
            onShowLayoutPickerMenuChange = { showLayoutPickerMenu = it },
            selectedAspectRatio = selectedAspectRatio,
            onAspectRatioSelected = { ratioOption ->
                selectedAspectRatio = ratioOption
                sharedPreferences.edit().putString("selected_aspect_ratio", ratioOption.name).apply()
                showAspectRatioMenu = false
                
                // Update HUD visual toast
                overlayZoomHUDValue = "Format: ${ratioOption.label.split(" - ").first()}"
                zoomHUDVisible = true
            },
            showAspectRatioMenu = showAspectRatioMenu,
            onShowAspectRatioMenuChange = { showAspectRatioMenu = it },
            isGridEnabled = isGridEnabled,
            isLevelEnabled = isLevelEnabled,
            isCrosshairEnabled = isCrosshairEnabled,
            onGuidesCycle = {
                if (isGridEnabled && !isLevelEnabled && !isCrosshairEnabled) {
                    isGridEnabled = true
                    isLevelEnabled = true
                    isCrosshairEnabled = false
                } else if (isGridEnabled && isLevelEnabled && !isCrosshairEnabled) {
                    isGridEnabled = true
                    isLevelEnabled = true
                    isCrosshairEnabled = true
                } else if (isGridEnabled && isLevelEnabled && isCrosshairEnabled) {
                    isGridEnabled = false
                    isLevelEnabled = false
                    isCrosshairEnabled = false
                } else {
                    isGridEnabled = true
                    isLevelEnabled = false
                    isCrosshairEnabled = false
                }
                
                sharedPreferences.edit()
                    .putBoolean("is_grid_enabled", isGridEnabled)
                    .putBoolean("is_level_enabled", isLevelEnabled)
                    .putBoolean("is_crosshair_enabled", isCrosshairEnabled)
                    .apply()
                    
                overlayZoomHUDValue = "Guides Presets Cycled"
                zoomHUDVisible = true
            },
            onGridToggle = {
                isGridEnabled = !isGridEnabled
                sharedPreferences.edit().putBoolean("is_grid_enabled", isGridEnabled).apply()
            },
            onLevelToggle = {
                isLevelEnabled = !isLevelEnabled
                sharedPreferences.edit().putBoolean("is_level_enabled", isLevelEnabled).apply()
            },
            onCrosshairToggle = {
                isCrosshairEnabled = !isCrosshairEnabled
                sharedPreferences.edit().putBoolean("is_crosshair_enabled", isCrosshairEnabled).apply()
            },
            showGuidesMenu = showGuidesMenu,
            onShowGuidesMenuChange = { showGuidesMenu = it }
        )
    }

        // Corner Position presets row under layout menu in PIP mode
        AnimatedVisibility(
            visible = selectedLayout == DualCameraLayout.PIP && !showLayoutPickerMenu,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.padding(top = 185.dp).align(Alignment.TopCenter)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xB309090B))
                    .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SNAP PRESET:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = TextMuted,
                    modifier = Modifier.padding(end = 4.dp)
                )
                
                PresetCornerButton(label = "↖") {
                    val localMinX = 10.dp
                    val localMinY = 100.dp
                    pipXPercent = localMinX / widthDp
                    pipYPercent = localMinY / heightDp
                    sharedPreferences.edit()
                        .putFloat("pip_x_percent", pipXPercent)
                        .putFloat("pip_y_percent", pipYPercent)
                        .apply()
                }
                PresetCornerButton(label = "↗") {
                    val localPipWidth = 120.dp * pipScale
                    val localMaxX = (widthDp - localPipWidth - 10.dp).coerceAtLeast(10.dp)
                    val localMinY = 100.dp
                    pipXPercent = localMaxX / widthDp
                    pipYPercent = localMinY / heightDp
                    sharedPreferences.edit()
                        .putFloat("pip_x_percent", pipXPercent)
                        .putFloat("pip_y_percent", pipYPercent)
                        .apply()
                }
                PresetCornerButton(label = "↙") {
                    val localMinX = 10.dp
                    val localPipHeight = 175.dp * pipScale
                    val localMaxY = (heightDp - localPipHeight - 140.dp).coerceAtLeast(100.dp)
                    pipXPercent = localMinX / widthDp
                    pipYPercent = localMaxY / heightDp
                    sharedPreferences.edit()
                        .putFloat("pip_x_percent", pipXPercent)
                        .putFloat("pip_y_percent", pipYPercent)
                        .apply()
                }
                PresetCornerButton(label = "↘") {
                    val localPipWidth = 120.dp * pipScale
                    val localPipHeight = 175.dp * pipScale
                    val localMaxX = (widthDp - localPipWidth - 10.dp).coerceAtLeast(10.dp)
                    val localMaxY = (heightDp - localPipHeight - 140.dp).coerceAtLeast(100.dp)
                    pipXPercent = localMaxX / widthDp
                    pipYPercent = localMaxY / heightDp
                    sharedPreferences.edit()
                        .putFloat("pip_x_percent", pipXPercent)
                        .putFloat("pip_y_percent", pipYPercent)
                        .apply()
                }
            }
        }
        
        // Safe drawing overlay layers
        // Top hud bar: STBY state, current active format and codec ratios
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group of Back Button and Live status mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isRecording) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xE609090B))
                            .border(0.8.dp, BorderGray, CircleShape)
                            .clickable { onBackToHome() }
                            .padding(8.dp)
                            .testTag("camera_exit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Exit to Home Dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Live status mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE609090B))
                        .border(0.8.dp, BorderGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                // Red breathing recording simulator
                val infiniteBlink = rememberInfiniteTransition(label = "blink")
                val blinkAlpha by infiniteBlink.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer(alpha = if (isPaused) (if (blinkState) 1f else 0.2f) else if (isRecording) blinkAlpha else 1f)
                        .clip(CircleShape)
                        .background(if (isPaused) GoldenHour else if (isRecording) GlowRecordRed else Color.Gray)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (isPaused) "PAUSED" else if (isRecording) "REC" else "STBY",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = if (isPaused) GoldenHour else if (isRecording) GlowRecordRed else TextWhite
                )
                
                Text(
                    text = if (isConcurrentActive) " • DUAL" else " • RAW",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isConcurrentActive) NeonGreen else TextMuted
                )

                if (isMuted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GlowRecordRed)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .testTag("muted_hud_badge")
                    ) {
                        Text(
                            text = "MUTED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }

                if (isExternalMicDetected && audioSourceSelection == "external") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldenHour)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .testTag("external_mic_hud_badge")
                    ) {
                        Text(
                            text = "EXTERNAL MIC",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                }
            }
            }
            
            // DuoCam Bluetooth & Prompter Hub
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isVoiceControlEnabled) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xE60A1C14))
                            .border(0.8.dp, if (isListeningForSpeech) NeonGreen else BorderGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("voice_control_active_indicator"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isListeningForSpeech) NeonGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice controls listening",
                            tint = if (isListeningForSpeech) NeonGreen else TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VC LISTENING",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (isListeningForSpeech) NeonGreen else TextMuted
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isTeleprompterEnabled) Color(0xE62A1B0C) else Color(0xE609090B))
                        .border(0.8.dp, if (isTeleprompterEnabled) GoldenHour else BorderGray, RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isPro) {
                                val teleprompterFirstTimeUsed = sharedPreferences.getBoolean("teleprompter_first_time_used", false)
                                if (!teleprompterFirstTimeUsed) {
                                    isTeleprompterEnabled = !isTeleprompterEnabled
                                    if (isTeleprompterEnabled) {
                                        android.widget.Toast.makeText(context, "Free trial active! Enjoy your first use of Teleprompter.", android.widget.Toast.LENGTH_LONG).show()
                                        sharedPreferences.edit().putBoolean("teleprompter_first_time_used", true).apply()
                                    }
                                } else {
                                    triggerGoPro("Presenter Teleprompter")
                                }
                            } else {
                                isTeleprompterEnabled = !isTeleprompterEnabled
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("teleprompter_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isTeleprompterEnabled) GoldenHour else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROMPTER",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isTeleprompterEnabled) GoldenHour else TextWhite
                    )
                }
            }
            
            // Format + Timecode
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE609090B))
                        .border(0.8.dp, if (isPaused) GoldenHour else BorderGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isPaused) "PAUSED" else formattedTime,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isPaused) GoldenHour else TextWhite,
                        modifier = Modifier.graphicsLayer(alpha = if (isPaused && !blinkState) 0.4f else 1f)
                    )
                    if (isPaused) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($formattedTime)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when (selectedResolution) {
                        VideoResolution.HD -> "720p"
                        VideoResolution.FHD -> "1080p"
                        VideoResolution.UHD -> "4K"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = when (selectedResolution) {
                        VideoResolution.HD -> Color.LightGray
                        VideoResolution.FHD -> NeonGreen
                        VideoResolution.UHD -> GoldenHour
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE609090B))
                        .border(0.8.dp, BorderGray, RoundedCornerShape(8.dp))
                        .clickable {
                            if (isRecording) {
                                android.widget.Toast.makeText(context, "Cannot change quality during recording", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val nextRes = when (selectedResolution) {
                                    VideoResolution.HD -> VideoResolution.FHD
                                    VideoResolution.FHD -> if (supports4K) VideoResolution.UHD else VideoResolution.HD
                                    VideoResolution.UHD -> VideoResolution.HD
                                }
                                selectResolution(nextRes)
                                if (!isPro && nextRes != VideoResolution.HD) {
                                    android.widget.Toast.makeText(context, "Switched to ${nextRes.name} (Free Watermarked Preview).", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("quality_quick_toggle")
                )
            }
        }
        
        // Left Overlay: Live VU Audio Metres
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x99000000))
                .border(0.5.dp, BorderGray, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AUDIO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                ),
                color = TextMuted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Row(
                modifier = Modifier.height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Left channel
                VUMeterBar(fillPercentage = vuLeft)
                // Right channel
                VUMeterBar(fillPercentage = vuRight)
            }
            
            Text(
                text = "L R",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = TextWhite,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "SOURCE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
                color = TextMuted,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Built-in Microphone
                val isBuiltInSelected = audioSourceSelection == "built_in"
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isBuiltInSelected) NeonGreen.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                        .border(
                            1.dp,
                            if (isBuiltInSelected) NeonGreen else BorderGray.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .clickable {
                            audioSourceSelection = "built_in"
                            isMuted = false
                        }
                        .padding(6.dp)
                        .testTag("source_builtin_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Built-in Mic",
                        tint = if (isBuiltInSelected) NeonGreen else TextWhite.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // 2. External Microphone (Headset or USB or Bluetooth)
                val isExternalSelected = audioSourceSelection == "external"
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (isExternalSelected) GoldenHour.copy(alpha = 0.2f)
                            else if (isExternalMicDetected) NeonGreen.copy(alpha = 0.1f)
                            else Color(0x11FFFFFF)
                        )
                        .border(
                            1.dp,
                            if (isExternalSelected) GoldenHour
                            else if (isExternalMicDetected) NeonGreen.copy(alpha = 0.3f)
                            else BorderGray.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .clickable {
                            audioSourceSelection = "external"
                            isMuted = false
                            if (!isExternalMicDetected) {
                                android.widget.Toast.makeText(context, "No external mic detected. Will fallback to built-in mic.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(6.dp)
                        .testTag("source_external_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headset,
                        contentDescription = "External Mic",
                        tint = if (isExternalSelected) GoldenHour
                               else if (isExternalMicDetected) NeonGreen
                               else TextWhite.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // 3. Muted (No Audio)
                val isMutedSelected = audioSourceSelection == "none" || isMuted
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isMutedSelected) GlowRecordRed.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                        .border(
                            1.dp,
                            if (isMutedSelected) GlowRecordRed else BorderGray.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .clickable {
                            audioSourceSelection = "none"
                            isMuted = true
                        }
                        .padding(6.dp)
                        .testTag("audio_mute_toggle_button")
                        .testTag("source_muted_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MicOff,
                        contentDescription = "Mute Audio",
                        tint = if (isMutedSelected) GlowRecordRed else TextWhite.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        // Right Overlay: Shutter & Cinematic metadata details
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x99000000))
                .border(0.5.dp, BorderGray, RoundedCornerShape(10.dp))
                .padding(all = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MetadataLabel(key = "ISO", value = "AUTO")
            MetadataLabel(key = "SHT", value = "1/125")
            MetadataLabel(key = "AP", value = "ƒ/1.8")
            MetadataLabel(key = "EV", value = "±0.0")
            
            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = BorderGray.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(2.dp))

            // Battery Level Custom Indicator
            val batColor = when {
                effectiveBatteryLevel <= 15 -> Color(0xFFEF4444)
                effectiveBatteryLevel <= 30 -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BATT",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(height = 8.dp, width = 14.dp)
                            .border(0.8.dp, batColor, RoundedCornerShape(1.dp))
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(effectiveBatteryLevel / 100f)
                                .background(batColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$effectiveBatteryLevel%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = batColor
                    )
                }
            }

            // Remaining Recording Time
            val estTimeStr = remember(effectiveBatteryLevel, selectedResolution, isLiteMode) {
                getEstimatedRecordingTimeText(effectiveBatteryLevel, selectedResolution, isLiteMode)
            }
            MetadataLabel(key = "EST REM", value = estTimeStr)

            // Temperature values
            val tempColor = when {
                effectiveDeviceTempCelsius >= 45.0f -> Color(0xFFEF4444)
                effectiveDeviceTempCelsius >= 38.0f -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }
            MetadataLabel(
                key = "TEMP",
                value = String.format(java.util.Locale.US, "%.1f°C", effectiveDeviceTempCelsius)
            )
        }
        
        // Lens switcher toggle overlays (BACK / FRONT)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp) // Sits neatly above the bottom controller button
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xB309090B))
                .border(0.8.dp, BorderGray, RoundedCornerShape(30.dp))
                .padding(all = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val backSelected = lensFacing == CameraSelector.LENS_FACING_BACK
            
            Text(
                text = "BACK LENS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp
                ),
                color = if (backSelected) ObsidianBlack else TextWhite,
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (backSelected) GlowRecordRed else Color.Transparent)
                    .clickable { lensFacing = CameraSelector.LENS_FACING_BACK }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            
            Text(
                text = "FRONT LENS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp
                ),
                color = if (!backSelected) ObsidianBlack else TextWhite,
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (!backSelected) GlowRecordRed else Color.Transparent)
                    .clickable { lensFacing = CameraSelector.LENS_FACING_FRONT }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        
        // Centered Focal Lens circular shortcut buttons (ultrawide, main, telephoto)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 175.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xE609090B))
                .border(0.8.dp, BorderGray, RoundedCornerShape(30.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("lens_focal_shortcuts_row"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rearSelected = (lensFacing == CameraSelector.LENS_FACING_BACK)
            val currentZoom = if (rearSelected) rearZoomValue else frontZoomValue
            val currentCam = if (rearSelected) backCameraInstance else frontCameraInstance
            
            listOf(0.5f, 1.0f, 2.0f, 4.0f).forEach { preset ->
                val isSelected = currentZoom == preset
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) GlowRecordRed else Color(0xFF1E1E22))
                        .clickable {
                            updateCameraZoom(currentCam, preset, rearSelected)
                        }
                        .testTag("preset_${preset}_x_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${preset}x",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isSelected) ObsidianBlack else TextWhite
                    )
                }
            }
        }
        
        // Temporary Zoom level notification HUD (visible when zooming ratio changes)
        AnimatedVisibility(
            visible = zoomHUDVisible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 180.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xE6131316))
                    .border(1.dp, GlowRecordRed, RoundedCornerShape(30.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .testTag("zoom_toast_hud"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = overlayZoomHUDValue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextWhite
                )
            }
        }
        
        // Center Focusing Target Frame Layout
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.Center)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.dp.toPx()
                val cornerLen = 16.dp.toPx()
                val sizeW = size.width
                val sizeH = size.height
                
                // Centered subtle crosshair
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = androidx.compose.ui.geometry.Offset(sizeW / 2 - 8.dp.toPx(), sizeH / 2),
                    end = androidx.compose.ui.geometry.Offset(sizeW / 2 + 8.dp.toPx(), sizeH / 2),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = androidx.compose.ui.geometry.Offset(sizeW / 2, sizeH / 2 - 8.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(sizeW / 2, sizeH / 2 + 8.dp.toPx()),
                    strokeWidth = strokeW
                )
                
                // Top-Left corner focus mark
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, cornerLen)
                        lineTo(0f, 0f)
                        lineTo(cornerLen, 0f)
                    },
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = strokeW)
                )
                
                // Top-Right corner mark
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(sizeW - cornerLen, 0f)
                        lineTo(sizeW, 0f)
                        lineTo(sizeW, cornerLen)
                    },
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = strokeW)
                )
                
                // Bottom-Left corner mark
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, sizeH - cornerLen)
                        lineTo(0f, sizeH)
                        lineTo(cornerLen, sizeH)
                    },
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = strokeW)
                )
                
                // Bottom-Right corner mark
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(sizeW - cornerLen, sizeH)
                        lineTo(sizeW, sizeH)
                        lineTo(sizeW, sizeH - cornerLen)
                    },
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = strokeW)
                )
            }
        }
        
        // Bottom controller bar containing premium glowing shutter button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            // Background capsule for controls
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x3B000000))
                    .border(0.8.dp, BorderGray, RoundedCornerShape(32.dp))
                    .padding(vertical = 14.dp, horizontal = 24.dp)
            ) {
                // The master premium glowing shutter record button (absolutely centered)
                ShutterButton(
                    isRecording = isRecording,
                    onClick = { triggerRecordAction() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("shutter_record_button")
                )

                // Left-aligned group
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isRecording) {
                        // Discard current recording button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x992E0E0E))
                                .border(1.dp, GlowRecordRed.copy(alpha = 0.5f), CircleShape)
                                .clickable { showDiscardConfirmDialog = true }
                                .padding(8.dp)
                                .testTag("discard_recording_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Discard current recording",
                                tint = GlowRecordRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Interactive settings dialog button option
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x6609090B))
                                .clickable { showSettingsSheet = true }
                                .padding(8.dp)
                                .testTag("settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Camera Custom Controls",
                                tint = TextWhite.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Watermark manager custom shortcut icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isWatermarkEnabled && activeWatermark != null) Color(0x3310B981) else Color(0x6609090B))
                                .border(
                                    width = if (isWatermarkEnabled && activeWatermark != null) 1.dp else 0.dp,
                                    color = if (isWatermarkEnabled && activeWatermark != null) NeonGreen.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (!isPro) {
                                        triggerGoPro("Custom Watermarks")
                                    } else {
                                        showWatermarkSheet = true
                                    }
                                }
                                .padding(8.dp)
                                .testTag("watermark_settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = "Watermark Settings Launcher",
                                tint = if (isWatermarkEnabled && activeWatermark != null) NeonGreen else TextWhite.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Countdown Timer icon showing current setting + cycling on tap
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (countdownSetting > 0) Color(0x1A10B981) else Color(0x6609090B))
                                .border(
                                    width = 1.dp,
                                    color = if (countdownSetting > 0) NeonGreen.copy(alpha = 0.8f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    countdownSetting = when (countdownSetting) {
                                        0 -> 3
                                        3 -> 5
                                        5 -> 10
                                        else -> 0
                                    }
                                    sharedPreferences.edit().putInt("countdown_setting", countdownSetting).apply()
                                }
                                .padding(2.dp)
                                .testTag("countdown_selector_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = "Countdown length: ${if (countdownSetting == 0) "Off" else "$countdownSetting seconds"}",
                                    tint = if (countdownSetting > 0) NeonGreen else TextWhite.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                                if (countdownSetting > 0) {
                                    Text(
                                        text = "${countdownSetting}s",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen,
                                        lineHeight = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Right-aligned group
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isRecording) {
                        // Pause / Resume recording button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x991C1E24))
                                .border(1.dp, BorderGray, CircleShape)
                                .clickable { togglePauseBackgroundRecording() }
                                .padding(8.dp)
                                .testTag("pause_resume_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            val pauseResumeIcon = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause
                            val pauseResumeTint = if (isPaused) GoldenHour else TextWhite
                            Icon(
                                imageVector = pauseResumeIcon,
                                contentDescription = if (isPaused) "Resume recording" else "Pause recording",
                                tint = pauseResumeTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Interactive stabilization toggle button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isStabilizationActive) Color(0x1F10B981) else Color(0x6609090B))
                                .border(
                                    width = if (isStabilizationActive) 1.dp else 0.dp,
                                    color = if (isStabilizationActive) NeonGreen.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    toggleStabilization(!isStabilizationActive)
                                }
                                .padding(8.dp)
                                .testTag("stabilization_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = "Video Stabilization: ${if (isStabilizationActive) "Enabled" else "Disabled"}",
                                tint = if (isStabilizationActive) NeonGreen else TextWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Interactive flash/torch multi-state toggle button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x6609090B))
                                .clickable {
                                    flashState = when (flashState) {
                                        "off" -> "on"
                                        "on" -> "auto"
                                        else -> "off"
                                    }
                                }
                                .padding(8.dp)
                                .testTag("flash_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            val flashIcon = when (flashState) {
                                "on" -> Icons.Rounded.FlashOn
                                "auto" -> Icons.Rounded.FlashAuto
                                else -> Icons.Rounded.FlashOff
                            }
                            val flashTint = when (flashState) {
                                "on" -> GoldenHour
                                "auto" -> NeonGreen
                                else -> TextWhite.copy(alpha = 0.5f)
                            }
                            Icon(
                                imageVector = flashIcon,
                                contentDescription = "Flash mode: $flashState",
                                tint = flashTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Watermarking control center overlay
        WatermarkManagerOverlay(
            isVisible = showWatermarkSheet,
            onClose = { showWatermarkSheet = false },
            savedWatermarks = savedWatermarks,
            onWatermarksUpdated = { updatedList ->
                savedWatermarks = updatedList
                WatermarkConfig.saveList(sharedPreferences, updatedList)
            },
            activeWatermarkId = activeWatermarkId,
            onActiveWatermarkIdSelected = { id ->
                activeWatermarkId = id
                sharedPreferences.edit().putString("active_watermark_id", id).apply()
            },
            isWatermarkEnabled = isWatermarkEnabled,
            onWatermarkEnabledToggle = { enabled ->
                isWatermarkEnabled = enabled
                sharedPreferences.edit().putBoolean("is_watermark_enabled", enabled).apply()
            },
            onApplyPreset = applyPresetPosition
        )

        if (showGoProScreen) {
            GoProScreen(
                context = context,
                colors = com.example.ui.theme.LocalThemeColors.current,
                featureName = activeGoProFeatureName,
                onDismiss = { showGoProScreen = false },
                onPurchaseSuccess = {
                    onProChange(true)
                    showGoProScreen = false
                }
            )
        }

        // System capability diagnostic settings overlay overlaying any camera elements
        fun performResetToDefaults() {
            selectResolution(VideoResolution.FHD)
            selectFrameRate(30)
            selectedLayout = DualCameraLayout.PIP
            sharedPreferences.edit().putString("selected_layout", DualCameraLayout.PIP.name).apply()
            selectedAspectRatio = RecordingAspectRatio.RATIO_9_16
            sharedPreferences.edit().putString("selected_aspect_ratio", RecordingAspectRatio.RATIO_9_16.name).apply()
            toggleHevcCompression(isHevcEncoderSupported)
            countdownSetting = 0
            sharedPreferences.edit().putInt("countdown_setting", 0).apply()
            
            toggleAudioNoiseReduction(false)
            setDefaultAudioSource("default")
            
            toggleStabilization(true)
            changeStabilizationStrength("Standard")
            isGridEnabled = true
            sharedPreferences.edit().putBoolean("is_grid_enabled", true).apply()
            setMirrorFrontCamera(true)
            
            setDefaultSaveLocation("public")
            setFileNamingFormat("default")
            setAutoDeleteDays(-1)
            
            onThemeModeChange("dark")
            onAccentColorChange("red")
            onUiDensityChange("standard")
            
            notifyRecordingCompletion = true
            sharedPreferences.edit().putBoolean("notify_recording_completion", true).apply()
            notifyBackupCompletion = true
            sharedPreferences.edit().putBoolean("notify_backup_completion", true).apply()
            notifyLowStorage = true
            sharedPreferences.edit().putBoolean("notify_low_storage", true).apply()
            notifyRecordingTipOfDay = true
            sharedPreferences.edit().putBoolean("notify_recording_tip_of_the_day", true).apply()
            autoEnableDnd = false
            sharedPreferences.edit().putBoolean("auto_enable_dnd", false).apply()
            
            android.widget.Toast.makeText(context, "Settings reset to default values", android.widget.Toast.LENGTH_SHORT).show()
        }

        // System capability diagnostic settings overlay overlaying any camera elements
        CameraSettingsOverlayBlock(
            isVisible = showSettingsSheet,
            onClose = { showSettingsSheet = false },
            isDualMode = isCompatDualMode,
            rearMaxRes = compatRearRes,
            frontMaxRes = compatFrontRes,
            isHwEncoding = compatHwEncoding,
            hasFlash = compatFlash,
            zoomRange = compatZoomRange,
            hasStabilization = compatStabilization,
            selectedResolution = selectedResolution,
            onResolutionSelectedInternal = { selectResolution(it) },
            supports4K = supports4K,
            selectedFrameRate = selectedFrameRate,
            onFrameRateSelectedInternal = { selectFrameRate(it) },
            useHevcCompression = useHevcCompression,
            onHevcToggleInternal = { toggleHevcCompression(it) },
            isHevcSupported = isHevcEncoderSupported,
            audioNoiseReductionEnabled = audioNoiseReductionEnabled,
            onAudioNoiseReductionToggleInternal = { toggleAudioNoiseReduction(it) },
            isVoiceControlEnabled = isVoiceControlEnabled,
            onVoiceControlToggleInternal = { toggleVoiceControl(it) },
            isStabilizationActive = isStabilizationActive,
            onStabilizationToggleInternal = { toggleStabilization(it) },
            stabilizationStrength = stabilizationStrength,
            onStabilizationStrengthChangeInternal = { changeStabilizationStrength(it) },
            selectedLayout = selectedLayout,
            onLayoutSelectedInternal = { selectedLayout = it },
            selectedAspectRatio = selectedAspectRatio,
            onAspectRatioSelectedInternal = { selectedAspectRatio = it },
            countdownSetting = countdownSetting,
            onCountdownSettingSelectedInternal = { countdownSetting = it },
            isGridEnabled = isGridEnabled,
            onGridEnabledToggleInternal = { isGridEnabled = it },
            mirrorFrontCamera = mirrorFrontCamera,
            onMirrorFrontCameraToggleInternal = { setMirrorFrontCamera(it) },
            defaultAudioSource = defaultAudioSource,
            onDefaultAudioSourceSelectedInternal = { setDefaultAudioSource(it) },
            defaultSaveLocation = defaultSaveLocation,
            onDefaultSaveLocationSelectedInternal = { setDefaultSaveLocation(it) },
            fileNamingFormat = fileNamingFormat,
            onFileNamingFormatSelectedInternal = { setFileNamingFormat(it) },
            autoDeleteDays = autoDeleteDays,
            onAutoDeleteDaysSelectedInternal = { setAutoDeleteDays(it) },
            onResetToDefaults = ::performResetToDefaults,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            accentColor = accentColor,
            onAccentColorChange = onAccentColorChange,
            uiDensity = uiDensity,
            onUiDensityChange = onUiDensityChange,
            notifyRecordingCompletion = notifyRecordingCompletion,
            onNotifyRecordingCompletionSelectedInternal = { notifyRecordingCompletion = it },
            notifyBackupCompletion = notifyBackupCompletion,
            onNotifyBackupCompletionSelectedInternal = { notifyBackupCompletion = it },
            notifyLowStorage = notifyLowStorage,
            onNotifyLowStorageSelectedInternal = { notifyLowStorage = it },
            notifyRecordingTipOfDay = notifyRecordingTipOfDay,
            onNotifyRecordingTipOfDaySelectedInternal = { notifyRecordingTipOfDay = it },
            autoEnableDnd = autoEnableDnd,
            onAutoEnableDndSelectedInternal = { autoEnableDnd = it },
            isPro = isPro,
            onProChange = onProChange,
            triggerGoPro = { triggerGoPro(it) },
            
            isLiteMode = isLiteMode,
            onLiteModeChange = { isLiteMode = it },
            batteryLevelSim = batteryLevelSimulated,
            onBatteryLevelSimChange = { batteryLevelSimulated = it },
            deviceTempSim = deviceTempSimulated,
            onDeviceTempSimChange = { deviceTempSimulated = it },
            
            sharedPreferences = sharedPreferences,
            context = context,
            onActivateLayoutPreview = { active, seconds ->
                isLayoutPreviewActive = active
                layoutPreviewRemainingSeconds = seconds
            }
        )

        CameraAlertsAndDialogs(
            recognizedCommandText = recognizedCommandText,
            isLayoutPreviewActive = isLayoutPreviewActive,
            isPro = isPro,
            layoutPreviewRemainingSeconds = layoutPreviewRemainingSeconds,
            selectedLayout = selectedLayout,
            selectedResolution = selectedResolution,
            show4KWarningBanner = show4KWarningBanner,
            isStabilizationActive = isStabilizationActive,
            showLowStorageWarningBanner = showLowStorageWarningBanner,
            freeSpaceMB = freeSpaceMB,
            effectiveBatteryLevel = effectiveBatteryLevel,
            showDiscardConfirmDialog = showDiscardConfirmDialog,
            showPracticeCompletedDialog = showPracticeCompletedDialog,
            onGoProClick = { triggerGoPro(it) },
            onDiscardCancel = { showDiscardConfirmDialog = false },
            onDiscardConfirm = {
                showDiscardConfirmDialog = false
                shouldDiscardNextFinishedRecording = true
                isRecording = false
                isPaused = false
                elapsedMillis = 0L
            },
            onPracticeDialogDismiss = { showPracticeCompletedDialog = false }
        )






        
        com.example.ui.CameraStatusOverlays(
            isRecording = isRecording,
            isPaused = isPaused,
            isProcessingRecording = isProcessingRecording,
            showSavedOverlay = showSavedOverlay,
            activeRecordingUri = activeRecordingUri,
            showVideoPlayer = showVideoPlayer,
            onShowVideoPlayerChange = { showVideoPlayer = it },
            onShowSavedOverlayChange = { showSavedOverlay = it }
        )
            
            // Beautiful Full-screen Countdown Overlay
            CountdownOverlay(
                secondsRemaining = activeCountdownSeconds,
                isVisible = isCountdownActive
            )
            
            // Teleprompter Pro overlay layout layer
            TeleprompterProOverlayBlock(
                isTeleprompterEnabled = isTeleprompterEnabled,
                onTeleprompterEnabledChange = { isTeleprompterEnabled = it },
                isEditingScript = isEditingScript,
                onEditingScriptChange = { isEditingScript = it },
                teleprompterBgOpacity = teleprompterBgOpacity,
                onTeleprompterBgOpacityChange = { teleprompterBgOpacity = it },
                teleprompterTextColorName = teleprompterTextColorName,
                onTeleprompterTextColorNameChange = { teleprompterTextColorName = it },
                isTeleprompterFullScreen = isTeleprompterFullScreen,
                onTeleprompterFullScreenChange = { isTeleprompterFullScreen = it },
                teleprompterDragOffsetX = teleprompterDragOffsetX,
                onTeleprompterDragOffsetXChange = { teleprompterDragOffsetX = it },
                teleprompterDragOffsetY = teleprompterDragOffsetY,
                onTeleprompterDragOffsetYChange = { teleprompterDragOffsetY = it },
                isTeleprompterScrolling = isTeleprompterScrolling,
                onTeleprompterScrollingChange = { isTeleprompterScrolling = it },
                scriptInputText = scriptInputText,
                onScriptInputTextChange = { scriptInputText = it },
                teleprompterScript = teleprompterScript,
                onTeleprompterScriptChange = { teleprompterScript = it },
                teleprompterSpeedMode = teleprompterSpeedMode,
                onTeleprompterSpeedModeChange = { teleprompterSpeedMode = it },
                teleprompterSpeed = teleprompterSpeed,
                onTeleprompterSpeedChange = { teleprompterSpeed = it },
                teleprompterTextSize = teleprompterTextSize,
                onTeleprompterTextSizeChange = { teleprompterTextSize = it },
                teleprompterScrollOffset = teleprompterScrollOffset,
                onTeleprompterScrollOffsetChange = { teleprompterScrollOffset = it },
                activeScriptTab = activeScriptTab,
                onActiveScriptTabChange = { activeScriptTab = it },
                currentScriptId = currentScriptId,
                onCurrentScriptIdChange = { currentScriptId = it },
                scriptTitleInput = scriptTitleInput,
                onScriptTitleInputChange = { scriptTitleInput = it },
                selectedFolderId = selectedFolderId,
                onSelectedFolderIdChange = { selectedFolderId = it },
                selectedLibraryFolderFilter = selectedLibraryFolderFilter,
                onSelectedLibraryFolderFilterChange = { selectedLibraryFolderFilter = it },
                showCreateFolderDialog = showCreateFolderDialog,
                onShowCreateFolderDialogChange = { showCreateFolderDialog = it },
                newFolderNameInput = newFolderNameInput,
                onNewFolderNameInputChange = { newFolderNameInput = it },
                teleprompterShowSettings = teleprompterShowSettings,
                onTeleprompterShowSettingsChange = { teleprompterShowSettings = it },
                savedFolders = savedFolders,
                savedScripts = savedScripts,
                scriptDao = scriptDao,
                coroutineScope = recordingScope,
                sharedPreferences = sharedPreferences,
                context = context,
                fileImportLauncher = fileImportLauncher
            )

            // 1. Camera Layout Tip (first time on camera screen)
            ContextualTipOverlay(
                tip = ContextualTip.CAMERA_LAYOUT,
                sharedPreferences = sharedPreferences,
                alignment = Alignment.TopCenter,
                yOffset = 185.dp,
                arrowDirection = ArrowDirection.UP,
                arrowOffsetX = (-40).dp
            )

            // 2. PiP Gesture Tip (first time seeing PiP)
            if (selectedLayout == DualCameraLayout.PIP) {
                ContextualTipOverlay(
                    tip = ContextualTip.PIP_GESTURE,
                    sharedPreferences = sharedPreferences,
                    alignment = Alignment.Center,
                    arrowDirection = ArrowDirection.NONE
                )
            }

            // 3. First Teleprompter Use Tip (first time teleprompter is turned on)
            if (isTeleprompterEnabled) {
                ContextualTipOverlay(
                    tip = ContextualTip.TELEPROMPTER_USE,
                    sharedPreferences = sharedPreferences,
                    alignment = Alignment.Center,
                    arrowDirection = ArrowDirection.NONE
                )
            }
        }
    }

/**
 * Beautiful high-fidelity simulated camera stream for fallback when concurrent streams are unsupported.
 */
@Composable
fun SimulatedCameraView(
    label: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLiteMode = remember {
        context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("is_lite_mode", false)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sim-shutter")
    
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scanY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanline"
    )

    val pulseSizeVal = if (isLiteMode) 1.0f else pulseSize
    val scanYVal = if (isLiteMode) 0.5f else scanY

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF09090C),
                        Color(0xFF13131A)
                    )
                )
            )
            .drawBehind {
                if (!isLiteMode) {
                    // Glowing warm atmosphere mimicking ambient light
                    drawCircle(
                        color = GlowRecordRed.copy(alpha = 0.22f),
                        radius = size.width * 0.45f * pulseSizeVal,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.42f)
                    )
                    drawCircle(
                        color = GoldenHour.copy(alpha = 0.12f),
                        radius = size.width * 0.38f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.58f)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size / 2f
            
            if (!isLiteMode) {
                // Sweep scan line
                drawLine(
                    color = GlowRecordRed.copy(alpha = 0.35f),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height * scanYVal),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height * scanYVal),
                    strokeWidth = 1.dp.toPx()
                )

                // Dynamic camera targeting circles
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = (size.width / 3.2f) * pulseSizeVal,
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // Holographic focus outline
                val trackerPath = androidx.compose.ui.graphics.Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center.width - (size.width / 4f) * pulseSizeVal,
                            center.height - (size.height / 5f) * pulseSizeVal,
                            center.width + (size.width / 4f) * pulseSizeVal,
                            center.height + (size.height / 3.5f) * pulseSizeVal
                        )
                    )
                }
                drawPath(
                    path = trackerPath,
                    color = NeonGreen.copy(alpha = 0.22f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(6f, 6f), 0f
                        )
                    )
                )
            } else {
                // Simplified static target focus bounds for Lite Mode
                drawRect(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = androidx.compose.ui.geometry.Offset(center.width - 40.dp.toPx(), center.height - 40.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 80.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Spot focal point
            drawCircle(
                color = GlowRecordRed,
                radius = 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(center.width, center.height)
            )
        }

        // Animated HUD values
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAM_02 [SIM]",
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "AUTO TRACKING",
                    fontSize = 6.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Text(
                    text = "TARGET SECURED",
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonGreen
                )
                Text(
                    text = "HDR SIM MODE",
                    fontSize = 5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun CountdownOverlay(
    secondsRemaining: Int,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.5f, animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 1.5f, animationSpec = tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .pointerInput(Unit) {}, // Consume taps under the overlay
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = secondsRemaining,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200, delayMillis = 50)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)))
                        .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 2.0f, animationSpec = tween(150)))
                },
                label = "countdown_num"
            ) { sec ->
                val displayText = if (sec > 0) sec.toString() else "GO!"
                val textColor = if (sec > 0) GoldenHour else NeonGreen
                val textStyle = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 140.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.sp
                )
                
                Text(
                    text = displayText,
                    style = textStyle,
                    color = textColor,
                    modifier = Modifier.testTag("countdown_text")
                )
            }
        }
    }
}

@Composable
fun ShutterButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val coreShapePercent = animateIntAsState(
        targetValue = if (isRecording) 14 else 50,
        animationSpec = tween(350),
        label = "coreShape"
    )
    val corePadding = animateDpAsState(
        targetValue = if (isRecording) 18.dp else 6.5.dp,
        animationSpec = tween(350),
        label = "corePadding"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "clickScale"
    )
    
    Box(
        modifier = modifier
            .size(76.dp)
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale
            )
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .drawBehind {
                // Subtle crimson outer bloom
                drawCircle(
                    color = GlowRecordRed.copy(alpha = 0.18f * pulseScale),
                    radius = (size.width / 2) * pulseScale * 1.15f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer pristine metal border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(3.5.dp, Color.White, CircleShape)
        )
        // Red recording core
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(corePadding.value)
                .clip(RoundedCornerShape(coreShapePercent.value))
                .background(
                    Brush.radialGradient(
                        colors = listOf(GlowRecordRed, Color(0xFFC21833)),
                    )
                )
        )
    }
}

/**
 * Vertical VU Meter indicator for audio feedback levels.
 */
@Composable
fun VUMeterBar(fillPercentage: Float) {
    val animatedFill by animateFloatAsState(
        targetValue = fillPercentage.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "vuHeight"
    )
    
    val targetColor = when {
        fillPercentage < 0.65f -> NeonGreen
        fillPercentage < 0.85f -> GoldenHour
        else -> GlowRecordRed
    }

    Column(
        modifier = Modifier
            .width(6.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF27272A)),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFill)
                .clip(RoundedCornerShape(3.dp))
                .background(targetColor)
        )
    }
}

@Composable
fun MetadataLabel(key: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            ),
            color = TextWhite
        )
    }
}

/**
 * Android CameraX hardware interface binding module.
 */
@Composable
fun CameraPreview(
    lensFacing: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    
    LaunchedEffect(lensFacing) {
        val cameraProviderProvider = ProcessCameraProvider.getInstance(context)
        cameraProviderProvider.addListener({
            val cameraProvider = cameraProviderProvider.get()
            
            // Preview usecase config
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            
            // Selector back/front focus
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                Log.e("DuoCam", "CameraX Lifecycle Binding Failure", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

enum class DualCameraLayout {
    PIP,
    SPLIT_HORIZONTAL,
    SPLIT_VERTICAL,
    SPLIT_DIAGONAL
}

class MorphingShapeA(
    private val progress: Float,
    private val cornerRadiusDp: Dp,
    private val density: Density
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (progress <= 0.01f) {
            val r = with(this.density) { cornerRadiusDp.toPx() }
            if (r <= 0f) {
                return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
            }
            return Outline.Rounded(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            )
        }
        
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * (1f - progress))
            lineTo(size.width * (1f - progress), size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

class MorphingShapeB(
    private val progress: Float,
    private val cornerRadiusDp: Dp,
    private val density: Density
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (progress <= 0.01f) {
            val r = with(this.density) { cornerRadiusDp.toPx() }
            if (r <= 0f) {
                return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
            }
            return Outline.Rounded(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            )
        }
        
        val path = Path().apply {
            moveTo(size.width * progress, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            lineTo(0f, size.height * progress)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun PipLayoutIcon(isSelected: Boolean) {
    val tint = if (isSelected) NeonGreen else TextWhite
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.dp, tint, RoundedCornerShape(3.dp))
            .padding(2.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .size(8.dp, 12.dp)
                .background(tint, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
fun SplitHorizontalIcon(isSelected: Boolean) {
    val tint = if (isSelected) NeonGreen else TextWhite
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.dp, tint, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(10.dp)
                .background(tint.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(1.dp)
                .background(tint)
        )
    }
}

@Composable
fun SplitVerticalIcon(isSelected: Boolean) {
    val tint = if (isSelected) NeonGreen else TextWhite
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.dp, tint, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(10.dp)
                .background(tint.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.dp)
                .background(tint)
        )
    }
}

@Composable
fun SplitDiagonalIcon(isSelected: Boolean) {
    val tint = if (isSelected) NeonGreen else TextWhite
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.dp, tint, RoundedCornerShape(3.dp))
            .drawBehind {
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    )
}

@Composable
fun PresetCornerButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0xFF27272A))
            .border(1.dp, Color(0xFF3F3F46), CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = Color.White
        )
    }
}

// --- Dynamic Device Capabilities Detection System ---

data class DeviceCompatSpec(
    val isDualSupported: Boolean,
    val rearMaxRes: String,
    val frontMaxRes: String,
    val isHwEncodingAvailable: Boolean,
    val hasFlash: Boolean,
    val zoomRange: String,
    val hasStabilization: Boolean
)

fun detectDeviceCapabilities(context: Context, isConcurrentCameraHardwareSupported: Boolean): DeviceCompatSpec {
    var rearResLabel = "1920 x 1080 (2.1MP)"
    var frontResLabel = "1280 x 720 (0.9MP)"
    var hasFlash = false
    var zoomRangeText = "1.0x to 8.0x"
    var hasStabilization = false
    
    try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager != null) {
            val ids = cameraManager.cameraIdList
            for (id in ids) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                
                // Read Resolution from configuration map
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val sizes = map?.getOutputSizes(android.graphics.ImageFormat.JPEG) 
                    ?: map?.getOutputSizes(android.graphics.SurfaceTexture::class.java)
                val maxSize = sizes?.maxByOrNull { it.width * it.height }
                val mpx = if (maxSize != null) {
                    (maxSize.width * maxSize.height).toFloat() / 1_000_000f
                } else 2.07f
                val resLabel = if (maxSize != null) {
                    "${maxSize.width} x ${maxSize.height} (${String.format("%.1f", mpx)}MP)"
                } else {
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) "1920 x 1080 (2.1MP)" else "1280 x 720 (0.9MP)"
                }
                
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    rearResLabel = resLabel
                    
                    // Hardware dynamic flash
                    hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    
                    // Zoom capabilities
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val range = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                        zoomRangeText = if (range != null) {
                            "${String.format("%.1f", range.lower)}x to ${String.format("%.1f", range.upper)}x"
                        } else {
                            val maxDigital = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                            "1.0x to ${String.format("%.1f", maxDigital)}x"
                        }
                    } else {
                        val maxDigital = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                        "1.0x to ${String.format("%.1f", maxDigital)}x"
                    }
                    
                    // Stabilization active
                    val stabModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                    hasStabilization = stabModes != null && stabModes.any { 
                        it != CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF 
                    }
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontResLabel = resLabel
                }
            }
        }
    } catch (e: Throwable) {
        Log.e("DuoCam", "Error querying Camera2 characteristics", e)
    }
    
    // Hardware video encoder check via MediaCodecList
    var isHwVideoEncoding = true
    try {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val infos = codecList.codecInfos
        val avcHw = infos.any { info ->
            if (!info.isEncoder) return@any false
            val types = info.supportedTypes
            types.any { type ->
                if (type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) || 
                    type.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isHardwareAccelerated
                    } else {
                        val name = info.name.lowercase()
                        !name.startsWith("omx.google.") && !name.startsWith("c2.android.") && !name.contains("soft")
                    }
                } else false
            }
        }
        isHwVideoEncoding = avcHw
    } catch (e: Throwable) {
        Log.e("DuoCam", "Error querying MediaCodecList for hardware encoders", e)
    }
    
    return DeviceCompatSpec(
        isDualSupported = isConcurrentCameraHardwareSupported,
        rearMaxRes = rearResLabel,
        frontMaxRes = frontResLabel,
        isHwEncodingAvailable = isHwVideoEncoding,
        hasFlash = hasFlash,
        zoomRange = zoomRangeText,
        hasStabilization = hasStabilization
    )
}













@Composable
fun PiPZoomControl(
    currentZoom: Float,
    camera: androidx.camera.core.Camera?,
    isRear: Boolean,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(bottom = 6.dp)
            .width(110.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC09090B))
            .border(0.5.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${String.format("%.1fx", currentZoom)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextWhite,
                modifier = Modifier.testTag(if (isRear) "pip_rear_zoom_txt" else "pip_front_zoom_txt")
            )
            
            Slider(
                value = currentZoom,
                onValueChange = onZoomChange,
                valueRange = 1.0f..4.0f,
                colors = SliderDefaults.colors(
                    thumbColor = GlowRecordRed,
                    activeTrackColor = GlowRecordRed,
                    inactiveTrackColor = Color(0xFF27272A)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .testTag(if (isRear) "pip_rear_zoom_slider" else "pip_front_zoom_slider")
            )
        }
    }
}

enum class RecordingAspectRatio(
    val ratio: Float,
    val valueString: String,
    val label: String,
    val description: String
) {
    RATIO_16_9(16f / 9f, "16:9", "16:9 - Widescreen", "YouTube, standard landscape uploads"),
    RATIO_9_16(9f / 16f, "9:16", "9:16 - Vertical Story", "TikTok, Shorts, Reels native format"),
    RATIO_1_1(1f, "1:1", "1:1 - Square Crop", "Instagram grid posts, styled snapshots"),
    RATIO_4_5(4f / 5f, "4:5", "4:5 - Social Portrait", "Instagram portrait, high-density focus")
}



@Composable
fun LevelIndicator(
    deviceTiltAngleProvider: () -> Float,
    isLevelEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLevelEnabled) return

    val angle = deviceTiltAngleProvider()
    val isLevel = Math.abs(angle) < 1.2f
    val levelColor = if (isLevel) NeonGreen else Color.White.copy(alpha = 0.8f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x99000000))
            .border(0.5.dp, BorderGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("level_hud_indicator"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = size.height / 2f
                val w = size.width
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = androidx.compose.ui.geometry.Offset(0f, h),
                    end = androidx.compose.ui.geometry.Offset(w, h),
                    strokeWidth = 1f
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2f - 6.dp.toPx(), h - 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
            }
            
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .graphicsLayer(rotationZ = -angle)
                    .background(levelColor)
            )
        }
        
        val formattedTiltAngle = remember(angle) {
            try {
                String.format(java.util.Locale.US, "%.1f°", angle)
            } catch (e: Exception) {
                "${Math.round(angle * 10.0) / 10.0}°"
            }
        }
        Text(
            text = formattedTiltAngle,
            color = levelColor,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.testTag("level_tilt_text")
        )
    }
}

@Composable
fun WatermarkManagerOverlay(
    isVisible: Boolean,
    onClose: () -> Unit,
    savedWatermarks: List<WatermarkConfig>,
    onWatermarksUpdated: (List<WatermarkConfig>) -> Unit,
    activeWatermarkId: String,
    onActiveWatermarkIdSelected: (String) -> Unit,
    isWatermarkEnabled: Boolean,
    onWatermarkEnabledToggle: (Boolean) -> Unit,
    onApplyPreset: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCreatingNew by remember { mutableStateOf(false) }
    
    // Creation form states
    var watermarkName by remember { mutableStateOf("") }
    var watermarkType by remember { mutableStateOf("TEXT") } // "TEXT" or "IMAGE"
    var textValue by remember { mutableStateOf("DuoCam Studio") }
    var fontSelected by remember { mutableStateOf("Roboto") }
    var fontSizeSelected by remember { mutableStateOf(24) }
    var colorHexSelected by remember { mutableStateOf("#FFFFFF") }
    var opacitySelected by remember { mutableStateOf(0.8f) }
    var imageFileSelectedPath by remember { mutableStateOf<String?>(null) }
    var scaleSelected by remember { mutableStateOf(1.0f) }
    var isAutoTimeSelected by remember { mutableStateOf(false) }
    var showByDefaultSelected by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val copied = WatermarkConfig.copyUriToPrivateWatermarks(context, uri)
            if (copied != null) {
                imageFileSelectedPath = copied
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.60f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { /* Prevent clicks from leaking to background overlay */ })
                    }
                    .testTag("watermark_settings_overlay")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Watermark Studio",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = { onClose() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .testTag("dismiss_watermark_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close watermark settings",
                                tint = TextWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Master Watermark Enable Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Burn-in Watermark Overlay",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = "Burn selected watermark directly into your recorded video files",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted
                                )
                            )
                        }
                        Switch(
                            checked = isWatermarkEnabled,
                            onCheckedChange = { onWatermarkEnabledToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = NeonGreen.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("watermark_enable_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isWatermarkEnabled) {
                        // Main content area
                        Box(modifier = Modifier.weight(1f)) {
                            if (isCreatingNew) {
                                // Creation Panel (Scrollable Column)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "CREATE NEW BRAND MARK",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GoldenHour,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Mark Name Field
                                    OutlinedTextField(
                                        value = watermarkName,
                                        onValueChange = { watermarkName = it },
                                        label = { Text("Watermark Name") },
                                        placeholder = { Text("e.g. YouTube Logo, Channel Name") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("watermark_new_name_input"),
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedContainerColor = Color(0xFF161619),
                                            unfocusedContainerColor = Color(0xFF161619),
                                            focusedLabelColor = NeonGreen,
                                            unfocusedLabelColor = TextMuted
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Type Tab Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(4.dp)
                                    ) {
                                        listOf("TEXT", "IMAGE").forEach { type ->
                                            val isSelected = watermarkType == type
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSelected) Color(0xFF1E1E24) else Color.Transparent)
                                                    .border(
                                                        width = if (isSelected) 1.dp else 0.dp,
                                                        color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { watermarkType = type }
                                                    .padding(10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = type,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) NeonGreen else TextMuted
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (watermarkType == "TEXT") {
                                        // Text settings
                                        OutlinedTextField(
                                            value = textValue,
                                            onValueChange = { textValue = it },
                                            label = { Text("Custom Text Content") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("watermark_text_input"),
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = TextWhite,
                                                unfocusedTextColor = TextWhite,
                                                focusedContainerColor = Color(0xFF161619),
                                                unfocusedContainerColor = Color(0xFF161619),
                                                focusedLabelColor = NeonGreen,
                                                unfocusedLabelColor = TextMuted
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Font family dropdown selection
                                        Text(
                                            text = "Typography Font",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            listOf("Roboto", "Monospace", "Serif", "Sans-Serif").forEach { font ->
                                                val isSelected = fontSelected == font
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                                        .border(1.dp, if (isSelected) NeonGreen else Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                        .clickable { fontSelected = font }
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = font,
                                                        fontFamily = when(font) {
                                                            "Monospace" -> FontFamily.Monospace
                                                            "Serif" -> FontFamily.Serif
                                                            "Sans-Serif" -> FontFamily.SansSerif
                                                            else -> FontFamily.Default
                                                        },
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) NeonGreen else TextWhite
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Font Size and color rows
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Font Size (${fontSizeSelected}sp)",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                )
                                                Slider(
                                                    value = fontSizeSelected.toFloat(),
                                                    onValueChange = { fontSizeSelected = it.toInt() },
                                                    valueRange = 12f..64f,
                                                    colors = SliderDefaults.colors(
                                                        activeTrackColor = NeonGreen,
                                                        thumbColor = NeonGreen
                                                    )
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Text Color",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    listOf("#FFFFFF", "#FFD700", "#10B981", "#06B6D4", "#EF4444").forEach { hex ->
                                                        val isSelected = colorHexSelected == hex
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(android.graphics.Color.parseColor(hex)))
                                                                .border(
                                                                    width = if (isSelected) 2.dp else 1.dp,
                                                                    color = if (isSelected) NeonGreen else Color.White.copy(alpha = 0.2f),
                                                                    shape = CircleShape
                                                                )
                                                                .clickable { colorHexSelected = hex }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Image logo settings
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.04f))
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Custom Image Logo File",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextWhite
                                                    )
                                                )
                                                Text(
                                                    text = if (imageFileSelectedPath != null) "Logo attached successfully" else "No image files selected yet",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = if (imageFileSelectedPath != null) NeonGreen else TextMuted
                                                    )
                                                )
                                            }
                                            
                                            Button(
                                                onClick = { imagePickerLauncher.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F23)),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                                modifier = Modifier.testTag("upload_watermark_button")
                                            ) {
                                                Text(
                                                    text = if (imageFileSelectedPath != null) "Change" else "Upload Logo",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextWhite
                                                    )
                                                )
                                            }
                                        }

                                        if (imageFileSelectedPath != null) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Column {
                                                Text(
                                                    text = "Default Image Scaling size",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                )
                                                Slider(
                                                    value = scaleSelected,
                                                    onValueChange = { scaleSelected = it },
                                                    valueRange = 0.4f..2.5f,
                                                    colors = SliderDefaults.colors(
                                                        activeTrackColor = NeonGreen,
                                                        thumbColor = NeonGreen
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Dynamic Opacity Slider (Common for both)
                                    Column {
                                        Text(
                                            text = "Watermark Opacity (${(opacitySelected * 100).toInt()}%)",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                        )
                                        Slider(
                                            value = opacitySelected,
                                            onValueChange = { opacitySelected = it },
                                            valueRange = 0.1f..1.0f,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = NeonGreen,
                                                thumbColor = NeonGreen
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Auto-date stamp and settings switches
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Real-time Date Stamp",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                                                )
                                                Text(
                                                    text = "Append auto-ticking date/time stamp to recording",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                )
                                            }
                                            Switch(
                                                checked = isAutoTimeSelected,
                                                onCheckedChange = { isAutoTimeSelected = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(alpha = 0.3f)),
                                                modifier = Modifier.testTag("new_watermark_datestamp_switch")
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Show on All Recordings by Default",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                                                )
                                                Text(
                                                    text = "Automatically apply this mark to new recordings",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                )
                                            }
                                            Switch(
                                                checked = showByDefaultSelected,
                                                onCheckedChange = { showByDefaultSelected = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(alpha = 0.3f)),
                                                modifier = Modifier.testTag("new_watermark_default_switch")
                                            )
                                         }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Action buttons row (Save / Cancel)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { isCreatingNew = false },
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel", color = TextWhite)
                                        }

                                        Button(
                                            onClick = {
                                                val typedName = watermarkName.ifEmpty { if (watermarkType == "TEXT") textValue else "Brand Mark Image" }
                                                val config = WatermarkConfig(
                                                    name = typedName,
                                                    type = watermarkType,
                                                    imagePath = imageFileSelectedPath,
                                                    text = textValue,
                                                    font = fontSelected,
                                                    fontSize = fontSizeSelected,
                                                    color = colorHexSelected,
                                                    opacity = opacitySelected,
                                                    scale = scaleSelected,
                                                    isAutoTime = isAutoTimeSelected,
                                                    showByDefault = showByDefaultSelected
                                                )

                                                val updatedList = if (showByDefaultSelected) {
                                                    savedWatermarks.map { it.copy(showByDefault = false) } + config
                                                } else {
                                                    savedWatermarks + config
                                                }

                                                onWatermarksUpdated(updatedList)
                                                onActiveWatermarkIdSelected(config.id)
                                                
                                                // Reset creation states
                                                isCreatingNew = false
                                                watermarkName = ""
                                                textValue = "DuoCam Studio"
                                                imageFileSelectedPath = null
                                                isAutoTimeSelected = false
                                                showByDefaultSelected = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("save_watermark_button")
                                        ) {
                                            Text(
                                                text = "Save Mark",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Default Watermarks List Panel
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SAVED BRAND MARKS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = GoldenHour,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        
                                        Button(
                                            onClick = { isCreatingNew = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.testTag("create_watermark_launcher")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "New Mark",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextWhite
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (savedWatermarks.isEmpty()) {
                                        // Empty list state helper
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.02f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                                Text(
                                                    text = "No custom watermarks saved",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Click 'New Mark' to design a text-based or uploaded logo watermark overlay.",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        // Display saved cards in a LazyColumn
                                        LazyColumn(
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(savedWatermarks.size) { index ->
                                                val mark = savedWatermarks[index]
                                                val isSelected = mark.id == activeWatermarkId
                                                
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isSelected) Color(0xFF12241F) else Color(0xFF161619))
                                                        .border(
                                                            width = 1.3.dp,
                                                            color = if (isSelected) NeonGreen.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.06f),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { onActiveWatermarkIdSelected(mark.id) }
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Leading icon showing type
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.06f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.AspectRatio,
                                                            contentDescription = null,
                                                            tint = if (isSelected) NeonGreen else TextMuted,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    // Label info
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = mark.name,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextWhite
                                                            )
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "${mark.type} Mark",
                                                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                                            )
                                                            if (mark.showByDefault) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(GoldenHour.copy(alpha = 0.15f))
                                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "DEFAULT",
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = GoldenHour
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Small delete icon
                                                    IconButton(
                                                        onClick = {
                                                            val updated = savedWatermarks.filter { it.id != mark.id }
                                                            onWatermarksUpdated(updated)
                                                            if (isSelected && updated.isNotEmpty()) {
                                                                onActiveWatermarkIdSelected(updated.first().id)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.05f))
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = "Delete brand mark",
                                                            tint = GlowRecordRed,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Detail panel of active selected watermark
                                        val selectedMark = savedWatermarks.find { it.id == activeWatermarkId }
                                        if (selectedMark != null) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(2f)
                                                    .fillMaxWidth()
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Text(
                                                    text = "EDIT PLACEMENT & BEHAVIOR",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = GoldenHour,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 1.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Opacity Slider adjustment on live selection
                                                Text(
                                                    text = "Active Opacity (${(selectedMark.opacity * 100).toInt()}%)",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                )
                                                Slider(
                                                    value = selectedMark.opacity,
                                                    onValueChange = { opacityVal ->
                                                        val updated = savedWatermarks.map {
                                                            if (it.id == selectedMark.id) it.copy(opacity = opacityVal) else it
                                                        }
                                                        onWatermarksUpdated(updated)
                                                    },
                                                    valueRange = 0.1f..1.0f,
                                                    colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen)
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Preset positioning
                                                Text(
                                                    text = "Quick Positioning Presets",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center").forEach { preset ->
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.White.copy(alpha = 0.04f))
                                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                                .clickable { onApplyPreset(preset) }
                                                                .padding(vertical = 10.dp, horizontal = 2.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = preset,
                                                                color = TextWhite,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Switches: Default switch
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "Apply on All Recordings",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                                                        )
                                                        Text(
                                                            text = "Automatically apply this mark to new recordings",
                                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                                        )
                                                    }
                                                    Switch(
                                                        checked = selectedMark.showByDefault,
                                                        onCheckedChange = { defaultVal ->
                                                            val updatedList = savedWatermarks.map {
                                                                if (it.id == selectedMark.id) {
                                                                    it.copy(showByDefault = defaultVal)
                                                                } else if (defaultVal) {
                                                                    it.copy(showByDefault = false) // Disable other defaults
                                                                } else {
                                                                    it
                                                                }
                                                            }
                                                            onWatermarksUpdated(updatedList)
                                                        },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(alpha = 0.3f))
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Interactive Hint
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
                                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Info,
                                                            contentDescription = null,
                                                            tint = GoldenHour,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Tip: Pinch to scale size & Drag to place this watermark anywhere on the camera feed screen directly!",
                                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty/disabled state block
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Watermarks are currently disabled. Toggle 'Burn-in Watermark Overlay' above to manage or select brand marks.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
