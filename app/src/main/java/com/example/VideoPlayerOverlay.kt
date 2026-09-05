package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerOverlay(
    recording: DuoRecording,
    playlist: List<DuoRecording> = emptyList(),
    onEditVideo: (DuoRecording) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Resolve full playlist fallback
    val actualPlaylist = remember(playlist, recording) {
        if (playlist.contains(recording)) playlist else listOf(recording)
    }
    
    // Track current index in resolved list
    var currentIndex by remember(actualPlaylist, recording) {
        val idx = actualPlaylist.indexOf(recording)
        mutableStateOf(if (idx != -1) idx else 0)
    }
    
    val currentVideo = remember(currentIndex, actualPlaylist) {
        if (currentIndex in actualPlaylist.indices) actualPlaylist[currentIndex] else recording
    }
    
    // State of Playback views
    var isPlayingState by remember { mutableStateOf(true) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    
    // Playback Progress states
    var currentPlaybackPosition by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(0L) }
    
    // Speed control (0.25x - 2x)
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    
    // Metadata Info panel display
    var showInfoDrawer by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSpeedPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showThumbnailCreator by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var friendlyNameInput by remember(currentVideo.uri) { mutableStateOf(VideoMetadataManager.getDisplayName(context, currentVideo).removeSuffix(".mp4")) }
    var videoThumbnail by remember(currentVideo.uri) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(currentVideo.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, currentVideo.uri)
                val bmp = retriever.getFrameAtTime(0)
                retriever.release()
                videoThumbnail = bmp
            } catch (e: Exception) {
                Log.e("VideoPlayerOverlay", "Error fetching thumbnail for info dialog", e)
            }
        }
    }
    
    // Slide / drag thumb preview frame
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionProgress by remember { mutableFloatStateOf(0f) }
    
    // Local memory thumbnail cache to eliminate drag seek layout delay
    val thumbnailCache = remember { mutableMapOf<Long, Bitmap>() }
    
    // Shutter flash effect
    var showCameraFlash by remember { mutableStateOf(false) }
    
    // Synchronize slider progress view during steady playing states
    LaunchedEffect(currentPlaybackPosition, totalDurationMs, isDraggingSlider) {
        if (!isDraggingSlider && totalDurationMs > 0) {
            val startMs = VideoMetadataManager.getTrimStart(context, currentVideo.uri)
            dragPositionProgress = ((currentPlaybackPosition - startMs).toFloat() / totalDurationMs).coerceIn(0f, 1f)
        }
    }
    
    // Continuous playback progress update loop
    LaunchedEffect(isPlayingState, currentVideo) {
        if (isPlayingState) {
            val startMs = VideoMetadataManager.getTrimStart(context, currentVideo.uri)
            val endMs = VideoMetadataManager.getTrimEnd(context, currentVideo.uri, currentVideo.durationMs)
            while (true) {
                videoViewInstance?.let { view ->
                    if (view.isPlaying) {
                        val current = view.currentPosition.toLong()
                        if (endMs > startMs) {
                            if (current < startMs) {
                                view.seekTo(startMs.toInt())
                                currentPlaybackPosition = startMs
                            } else if (current >= endMs) {
                                view.seekTo(startMs.toInt())
                                currentPlaybackPosition = startMs
                            } else {
                                currentPlaybackPosition = current
                            }
                        } else {
                            currentPlaybackPosition = current
                        }
                    }
                }
                delay(200)
            }
        }
    }
    
    // Apply speed parameter adjustments immediately to physical player instance
    LaunchedEffect(currentSpeed, currentVideo, mediaPlayerRef) {
        mediaPlayerRef?.let { mp ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = mp.playbackParams
                    params.speed = currentSpeed
                    mp.playbackParams = params
                }
            } catch (e: Exception) {
                Log.e("VideoPlayerOverlay", "Error adjusting speed settings", e)
            }
        }
    }
    
    // Capture background sweep delta variables
    var dX by remember { mutableStateOf(0f) }
    val swipeThreshold = 140f
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_overlay")
    ) {
        // Safe interactive Viewport layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(actualPlaylist, currentIndex) {
                    if (actualPlaylist.size > 1) {
                        detectDragGestures(
                            onDragStart = { dX = 0f },
                            onDragEnd = {
                                if (dX > swipeThreshold) {
                                    // Swipe right -> Go to previous video
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        isPlayingState = true
                                    } else {
                                        Toast.makeText(context, "First video", Toast.LENGTH_SHORT).show()
                                    }
                                } else if (dX < -swipeThreshold) {
                                    // Swipe left -> Go to next video
                                    if (currentIndex < actualPlaylist.size - 1) {
                                        currentIndex++
                                        isPlayingState = true
                                    } else {
                                        Toast.makeText(context, "Last video", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dX += dragAmount.x
                            }
                        )
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tap video to toggle play status
                    videoViewInstance?.let { view ->
                        if (view.isPlaying) {
                            view.pause()
                            isPlayingState = false
                        } else {
                            view.start()
                            isPlayingState = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Hot swap native view wrapper with key URI binding
            key(currentVideo.uri) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(currentVideo.uri)
                            setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                mp.isLooping = true
                                // Apply speed parameter directly on prepared callback
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        val params = mp.playbackParams
                                        params.speed = currentSpeed
                                        mp.playbackParams = params
                                    } catch (ignored: Exception) {}
                                }
                                val startMs = VideoMetadataManager.getTrimStart(context, currentVideo.uri)
                                val endMs = VideoMetadataManager.getTrimEnd(context, currentVideo.uri, duration.toLong())
                                totalDurationMs = if (endMs > startMs) (endMs - startMs) else duration.toLong()
                                currentPlaybackPosition = startMs
                                seekTo(startMs.toInt())
                                start()
                                isPlayingState = true
                            }
                            setOnCompletionListener {
                                isPlayingState = false
                            }
                            videoViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f),
                    update = { view ->
                        videoViewInstance = view
                    }
                )
            }
        }
        
        // 1. Shutter camera snapshot visual flash overlay
        if (showCameraFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
        
        // 2. Play/Pause HUD state indicators centered
        if (!isPlayingState) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, BorderGray.copy(alpha = 0.2f), CircleShape)
                    .align(Alignment.Center)
                    .clickable {
                        videoViewInstance?.start()
                        isPlayingState = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Resume Video Action",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        // 3. Floating Seek thumbnail drag preview card placement
        if (isDraggingSlider) {
            val dragPositionMs = (dragPositionProgress * totalDurationMs).toLong()
            val dragSec = dragPositionMs / 1000
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp)
                    .width(180.dp)
                    .height(115.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, GlowRecordRed, RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
                
                LaunchedEffect(currentVideo.uri, dragSec) {
                    if (thumbnailCache.containsKey(dragSec)) {
                        previewBmp = thumbnailCache[dragSec]
                    } else {
                        withContext(Dispatchers.IO) {
                            var retriever: MediaMetadataRetriever? = null
                            try {
                                retriever = MediaMetadataRetriever()
                                retriever.setDataSource(context, currentVideo.uri)
                                val bmp = retriever.getFrameAtTime(dragSec * 1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                if (bmp != null) {
                                    val scaled = Bitmap.createScaledBitmap(bmp, 180, 115, false)
                                    thumbnailCache[dragSec] = scaled
                                    withContext(Dispatchers.Main) {
                                        previewBmp = scaled
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("VideoPlayerOverlay", "Fail pulling drag frame $dragSec", e)
                            } finally {
                                try {
                                    retriever?.release()
                                } catch (ignored: Exception) {}
                            }
                        }
                    }
                }
                
                if (previewBmp != null) {
                    Image(
                        bitmap = previewBmp!!.asImageBitmap(),
                        contentDescription = "Seek Drag Preview Frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = GlowRecordRed, modifier = Modifier.size(24.dp))
                }
                
                // Absolute exact seeking timing caption inside thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(dragPositionMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextWhite
                    )
                }
            }
        }
        
        // 4. Header metadata & Close buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GlowRecordRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = getRecordingLayout(currentVideo),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp
                            ),
                            color = GlowRecordRed
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DuoCam Player",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = TextWhite
                    )
                }
                Text(
                    text = VideoMetadataManager.getDisplayName(context, currentVideo),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Speed controller pill indicator button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, BorderGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { showSpeedPicker = !showSpeedPicker }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "Speed Option Selector",
                        tint = NeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentSpeed}x",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = TextWhite
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // Closure button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, BorderGray.copy(alpha = 0.3f), CircleShape)
                    .clickable { onDismiss() }
                    .testTag("close_player_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close player viewport",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // 5. Dynamic Speed picker dialog selector row overlay
        AnimatedVisibility(
            visible = showSpeedPicker,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PLAYBACK VELOCITY",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { speedOption ->
                            val isChosen = currentSpeed == speedOption
                            val optionBg = if (isChosen) NeonGreen.copy(alpha = 0.18f) else Color.Transparent
                            val optionBorder = if (isChosen) NeonGreen else BorderGray.copy(alpha = 0.2f)
                            val optionTextStr = if (speedOption == 1.0f) "1.0x Normal" else "${speedOption}x"
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, optionBorder, RoundedCornerShape(8.dp))
                                    .background(optionBg)
                                    .clickable {
                                        currentSpeed = speedOption
                                        showSpeedPicker = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = optionTextStr,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isChosen) NeonGreen else TextWhite
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 6. Bottom controller hub & action drawers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                    )
                )
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seek bar + Time Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val startMs = VideoMetadataManager.getTrimStart(context, currentVideo.uri)
                Text(
                    text = formatDuration((currentPlaybackPosition - startMs).coerceAtLeast(0L)),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = TextWhite
                )
                
                Slider(
                    value = dragPositionProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        dragPositionProgress = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        val startMs = VideoMetadataManager.getTrimStart(context, currentVideo.uri)
                        val targetMs = (startMs + dragPositionProgress * totalDurationMs).toInt()
                        videoViewInstance?.seekTo(targetMs)
                        currentPlaybackPosition = targetMs.toLong()
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = GlowRecordRed,
                        activeTrackColor = GlowRecordRed,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                        activeTickColor = GlowRecordRed
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_seek_slider")
                )
                
                Text(
                    text = formatDuration(totalDurationMs),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = TextMuted
                )
            }
            
            // Media playback center buttons (Play/Pause, Skips, Screen Capture)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Screenshot / Screen Capture Button
                IconButton(
                    onClick = {
                        showCameraFlash = true
                        coroutineScope.launch {
                            delay(100)
                            showCameraFlash = false
                        }
                        // Capture standard frame
                        val capturePosMs = currentPlaybackPosition
                        coroutineScope.launch(Dispatchers.IO) {
                            var retriever: MediaMetadataRetriever? = null
                            try {
                                retriever = MediaMetadataRetriever()
                                retriever.setDataSource(context, currentVideo.uri)
                                val b = retriever.getFrameAtTime(capturePosMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    ?: retriever.getFrameAtTime(capturePosMs * 1000)
                                if (b != null) {
                                    val contentValues = android.content.ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, "DuoCam_Shot_${System.currentTimeMillis()}.jpg")
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DuoCam")
                                        }
                                    }
                                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                    if (uri != null) {
                                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                                            b.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                                        }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Frame saved to Photos!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed parsing frame image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Screenshot Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                try {
                                    retriever?.release()
                                } catch (ignored: Exception) {}
                            }
                        }
                    },
                    modifier = Modifier.testTag("player_shutter_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Capture Shot Frame",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Skip backward -10s
                IconButton(
                    onClick = {
                        videoViewInstance?.let { view ->
                            val p = (view.currentPosition - 10000).coerceAtLeast(0)
                            view.seekTo(p)
                            currentPlaybackPosition = p.toLong()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Rewind -10 seconds",
                        tint = TextWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                // Absolute Center Core Play/Pause Toggle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GlowRecordRed.copy(alpha = 0.2f))
                        .border(1.dp, GlowRecordRed, CircleShape)
                        .clickable {
                            videoViewInstance?.let { view ->
                                if (view.isPlaying) {
                                    view.pause()
                                    isPlayingState = false
                                } else {
                                    view.start()
                                    isPlayingState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlayingState) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play Control Toggle",
                        tint = GlowRecordRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Skip forward +10s
                IconButton(
                    onClick = {
                        videoViewInstance?.let { view ->
                            val p = (view.currentPosition + 10000).coerceAtMost(view.duration)
                            view.seekTo(p)
                            currentPlaybackPosition = p.toLong()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Skip forward +10 seconds",
                        tint = TextWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                // Info toggler
                IconButton(
                    onClick = { showInfoDrawer = !showInfoDrawer },
                    modifier = Modifier.testTag("player_info_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Toggle Video Info Drawer",
                        tint = if (showInfoDrawer) GlowRecordRed else TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Horizontal row divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderGray.copy(alpha = 0.25f))
            )
            
            // Bottom Action buttons: Share, Edit, Delete, Info labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SHARE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showShareSheet = true }
                        .padding(8.dp)
                        .testTag("action_share")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share item",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SHARE",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = TextWhite
                    )
                }
                
                // THUMBNAIL CREATOR BUTTON
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showThumbnailCreator = true }
                        .padding(8.dp)
                        .testTag("player_thumbnail_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = "Create Thumbnail",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "THUMBNAIL",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = TextWhite
                    )
                }
                
                // EDIT video action triggering editor suite
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            onEditVideo(currentVideo)
                        }
                        .padding(8.dp)
                        .testTag("player_edit_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit items",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EDIT",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = TextWhite
                    )
                }
                
                // DELETE item with confirm
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showDeleteConfirmation = true }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete item",
                        tint = GlowRecordRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DELETE",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = GlowRecordRed
                    )
                }
            }
        }
        
        // 7. Frosted/detailed metadata sliding sheet overlay
        AnimatedVisibility(
            visible = showInfoDrawer,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.98f)),
                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CINEMATIC LAYER INFO",
                            color = NeonGreen,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                        IconButton(onClick = { showInfoDrawer = false }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close details drawer",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Large thumbnail at the top
                    videoThumbnail?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Video layout preview render",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, BorderGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Videocam, contentDescription = "Video icon", tint = TextMuted, modifier = Modifier.size(36.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Tap filename to edit / inline rename
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRenameDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Rename Video",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FILE NAME (TAP TO EDIT)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = NeonGreen
                                )
                                Text(
                                    text = VideoMetadataManager.getDisplayName(context, currentVideo),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Technical detailed specs list
                    listOf(
                        "Resolution" to VideoMetadataManager.getResolution(context, currentVideo),
                        "Frame Rate" to "${VideoMetadataManager.getFps(context, currentVideo)} FPS",
                        "Duration" to formatDuration(currentVideo.durationMs),
                        "File Size" to formatSize(currentVideo.sizeBytes),
                        "Codec Track" to VideoMetadataManager.getCodec(context, currentVideo),
                        "Layout Pattern" to VideoMetadataManager.getLayout(context, currentVideo),
                        "Aspect Ratio" to VideoMetadataManager.getAspectRatio(context, currentVideo),
                        "Camera Mode" to VideoMetadataManager.getCameraMode(context, currentVideo),
                        "File Path" to "/storage/emulated/0/Movies/DuoCam/${currentVideo.name}"
                    ).forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                            )
                        }
                    }

                    // Optional GPS Location Overlay Map
                    val hasLocPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                           context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasLocPermission) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                for (col in 1..4) {
                                    val x = (w / 5) * col
                                    drawLine(Color.White.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, h))
                                }
                                for (row in 1..3) {
                                    val y = (h / 4) * row
                                    drawLine(Color.White.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y))
                                }
                                drawCircle(NeonGreen.copy(alpha = 0.08f), radius = 35.dp.toPx())
                                drawCircle(NeonGreen.copy(alpha = 0.15f), radius = 15.dp.toPx())
                                drawLine(NeonGreen.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(w/2 - 15, h/2), end = androidx.compose.ui.geometry.Offset(w/2 + 15, h/2))
                                drawLine(NeonGreen.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(w/2, h/2 - 15), end = androidx.compose.ui.geometry.Offset(w/2, h/2 + 15))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = "GPS Lock", tint = GlowRecordRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GPS COORD TARGET ACTIVE", color = NeonGreen, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 9.sp))
                                }
                                Text(
                                    text = VideoMetadataManager.getLocation(context, currentVideo),
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Sharing History Header
                    Text(
                        text = "SHARING HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = NeonGreen
                    )
                    
                    val shares = VideoMetadataManager.getShares(context, currentVideo)
                    if (shares.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            shares.forEach { platform ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonGreen.copy(alpha = 0.15f))
                                        .border(0.5.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = when (platform) {
                                            "Instagram" -> Icons.Rounded.CameraAlt
                                            "TikTok" -> Icons.Rounded.MusicNote
                                            "YouTube" -> Icons.Rounded.PlayCircle
                                            else -> Icons.Rounded.Chat
                                        },
                                        contentDescription = platform,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(platform, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = NeonGreen)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No sharing history recorded for this video clip.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Share Quick Action Triggers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Instagram", "TikTok", "YouTube", "WhatsApp").forEach { platform ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DeepCharcoal)
                                    .border(1.dp, BorderGray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        VideoMetadataManager.addShare(context, currentVideo, platform)
                                        val intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_STREAM, currentVideo.uri)
                                            type = "video/mp4"
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(intent, "Share to $platform"))
                                        } catch (e: Exception) {}
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = platform,
                                    color = TextWhite,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                )
                            }
                        }
                    }

                    // Folder Assignment option
                    var showFolderSelectorDialog by remember { mutableStateOf(false) }
                    val currentFolderOfVideo = VideoMetadataManager.getVideoFolder(context, currentVideo) ?: "None"
                    
                    Text(
                        text = "FOLDER ORGANIZER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = NeonGreen,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, BorderGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { showFolderSelectorDialog = true }
                            .padding(12.dp)
                            .testTag("video_folder_selector_btn"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentFolderOfVideo == "None") Icons.Rounded.FolderOff else Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (currentFolderOfVideo == "None") TextMuted else NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentFolderOfVideo,
                                color = TextWhite,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Change Folder",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showFolderSelectorDialog) {
                        val folders = VideoMetadataManager.getCustomFolders(context)
                        AlertDialog(
                            onDismissRequest = { showFolderSelectorDialog = false },
                            title = {
                                Text(
                                    text = "Assign to Folder",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Select a folder category to organize this cinematic clip:",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    // None option
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                VideoMetadataManager.moveVideoToFolder(context, currentVideo, null)
                                                showFolderSelectorDialog = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.FolderOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("None (Remove from Folder)", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Custom folders options
                                    folders.forEach { fName ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    VideoMetadataManager.moveVideoToFolder(context, currentVideo, fName)
                                                    showFolderSelectorDialog = false
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(fName, color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showFolderSelectorDialog = false }) {
                                    Text("Close", color = TextWhite)
                                }
                            },
                            containerColor = DeepCharcoal,
                            tonalElevation = 6.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Duplicate Video Action
                    Button(
                        onClick = {
                            val done = VideoMetadataManager.duplicateVideo(context, currentVideo)
                            if (done) {
                                Toast.makeText(context, "DuoCam Video Duplicated!", Toast.LENGTH_SHORT).show()
                                showInfoDrawer = false
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed to create duplicate copy", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenHour),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("duplicate_video_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Duplicate original video",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DUPLICATE ORIGINAL COPY",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = Color.Black
                        )
                    }
                }
            }
        }
        
         // 8. Delete confirmations
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Entry?", color = TextWhite, fontFamily = FontFamily.Monospace) },
                text = { Text("Are you sure you want to permanently erase this dual-cam snapshot file?", color = TextWhite.copy(alpha = 0.8f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                            val toDelete = currentVideo
                            try {
                                context.contentResolver.delete(toDelete.uri, null, null)
                                Toast.makeText(context, "Video Deleted", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("VideoPlayerOverlay", "Error erasing files", e)
                            }
                            // Call onDismiss to reload lists
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowRecordRed)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel", color = TextWhite)
                    }
                },
                containerColor = DeepCharcoal,
                tonalElevation = 6.dp
            )
        }

        // 9. Rename Dialog overlay
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { 
                    Text(
                        "Rename DuoCam Entry", 
                        color = Color.White, 
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                text = {
                    Column {
                        Text(
                            text = "Choose a friendly, recognizable title for your cinematic layout:",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = friendlyNameInput,
                            onValueChange = { friendlyNameInput = it },
                            placeholder = { Text("e.g. Beach Vlog Day 2", color = TextMuted) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextWhite),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = BorderGray,
                                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pattern = friendlyNameInput.trim()
                            if (pattern.isNotEmpty()) {
                                val success = VideoMetadataManager.renameVideo(context, currentVideo, pattern)
                                if (success) {
                                    Toast.makeText(context, "Entry Renamed!", Toast.LENGTH_SHORT).show()
                                    showRenameDialog = false
                                    onDismiss() // reloads gallery!
                                } else {
                                    Toast.makeText(context, "Could not rename video", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel", color = TextWhite)
                    }
                },
                containerColor = DeepCharcoal,
                tonalElevation = 6.dp
            )
        }

        // 10. Thumbnail Creator Overlay Dialog
        if (showThumbnailCreator) {
            androidx.compose.ui.window.Dialog(
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { showThumbnailCreator = false }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ThumbnailCreatorScreen(
                        video = currentVideo,
                        onDismiss = { showThumbnailCreator = false },
                        onThumbnailSaved = {
                            onDismiss() // reloads gallery!
                        }
                    )
                }
            }
        }

        // 11. Custom Quick & Queued Share Sheet Dialog
        if (showShareSheet) {
            androidx.compose.ui.window.Dialog(
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { 
                    if (!ShareManager.isQueueActive) {
                        showShareSheet = false 
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = !ShareManager.isQueueActive) { showShareSheet = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .clickable(enabled = false) {} // prevent dismissing click propagation
                            .testTag("share_sheet_card"),
                        colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
                        border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        ) {
                            // Header row with title & close button
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Share DuoCam Snapshot",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                }
                                
                                if (!ShareManager.isQueueActive) {
                                    IconButton(
                                        onClick = { showShareSheet = false },
                                        modifier = Modifier.testTag("share_close_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close share sheet",
                                            tint = TextMuted
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = BorderGray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))
                            
                            if (ShareManager.isQueueActive) {
                                // 11A. SHARE QUEUE PROGRESS SCREEN
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { ShareManager.activeShareProgress },
                                            color = NeonGreen,
                                            trackColor = Color.DarkGray,
                                            strokeWidth = 4.dp,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.RocketLaunch,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    Text(
                                        text = "Processing Share Queue...",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                    
                                    Text(
                                        text = ShareManager.logMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // Process status list
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ShareManager.activeQueue.forEachIndexed { idx, qi ->
                                            val isCurrent = (ShareManager.currentQueueIndex == idx)
                                            val statusColor = when (qi.status) {
                                                "formatting" -> GoldenHour
                                                "ready" -> Color(0xFF0EA5E9)
                                                "shared" -> NeonGreen
                                                "failed" -> Color.Red
                                                else -> TextMuted
                                            }
                                            
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isCurrent) DeepCharcoal else Color.Black.copy(alpha = 0.2f)
                                                ),
                                                border = BorderStroke(1.dp, if (isCurrent) NeonGreen.copy(alpha = 0.4f) else BorderGray.copy(alpha = 0.1f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(qi.platform.brandColor))
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = qi.platform.name,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = TextWhite
                                                        )
                                                    }
                                                    
                                                    Text(
                                                        text = qi.status.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = statusColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Button(
                                        onClick = { ShareManager.cancelQueue() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, Color.Red),
                                        modifier = Modifier.testTag("share_cancel_queue_btn")
                                    ) {
                                        Text("CANCEL SHARE QUEUE", color = Color.Red, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            } else {
                                // 11B. SELECTION & CONFIGURATION SCREEN
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "QUICK DIRECT SHARE (SOURE PRESETS AUTO-APPLIED)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = NeonGreen
                                    )
                                    
                                    // Sorting platforms by Share Count frequency (Most Used first)
                                    val sortedPlatforms = ShareManager.getSortedPlatforms(context)
                                    
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(sortedPlatforms) { p ->
                                            val isQueued = ShareManager.selectedForQueue[p.id] == true
                                            
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(82.dp)
                                                    .clickable {
                                                        // Tap platform to transcode & share directly
                                                        ShareManager.selectedForQueue.keys.forEach { ShareManager.selectedForQueue[it] = false }
                                                        ShareManager.selectedForQueue[p.id] = true
                                                        ShareManager.startQueueProcessing(context, currentVideo)
                                                    }
                                                    .testTag("share_direct_${p.id}"),
                                                colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.6f)),
                                                border = BorderStroke(1.dp, if (isQueued) Color(p.brandColor) else BorderGray.copy(alpha = 0.2f))
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    // Individual checkbox to build multi-share queues
                                                    Checkbox(
                                                        checked = isQueued,
                                                        onCheckedChange = { checked ->
                                                            ShareManager.selectedForQueue[p.id] = checked
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = Color(p.brandColor),
                                                            checkmarkColor = Color.Black
                                                        ),
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .scale(0.7f)
                                                            .testTag("share_checkbox_${p.id}")
                                                    )
                                                    
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(8.dp),
                                                        verticalArrangement = Arrangement.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(p.brandColor).copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = when (p.id) {
                                                                    "instagram" -> Icons.Rounded.CameraAlt
                                                                    "tiktok" -> Icons.Rounded.MusicNote
                                                                    "youtube" -> Icons.Rounded.PlayCircle
                                                                    "whatsapp" -> Icons.Rounded.Chat
                                                                    "snapchat" -> Icons.Rounded.AutoAwesome
                                                                    else -> Icons.Rounded.Share
                                                                },
                                                                contentDescription = null,
                                                                tint = Color(p.brandColor),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        
                                                        Text(
                                                            text = p.name,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = TextWhite,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Queue Dispatch Buttons
                                    val selectedPlatformsCount = ShareManager.selectedForQueue.values.count { it }
                                    if (selectedPlatformsCount > 0) {
                                        Button(
                                            onClick = { ShareManager.startQueueProcessing(context, currentVideo) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().testTag("share_dispatch_queue_btn")
                                        ) {
                                            Text(
                                                text = "DISPATCH SHARE QUEUE ($selectedPlatformsCount PLATFORMS)",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(color = BorderGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    // Utility actions
                                    Text(
                                        text = "UTILITY & OFFLINE PRESETS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = NeonGreen
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Save to files
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clickable {
                                                    val saved = ShareManager.saveToFiles(context, currentVideo.uri)
                                                    if (saved) {
                                                        Toast.makeText(context, "Saved to public Downloads folder!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .testTag("share_save_to_files"),
                                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.4f)),
                                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.15f))
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(imageVector = Icons.Rounded.Download, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Save to Files", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                                            }
                                        }
                                        
                                        // 2. Copy to clipboard
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clickable {
                                                    ShareManager.copyToClipboard(context, currentVideo.uri)
                                                }
                                                .testTag("share_copy_to_clipboard"),
                                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.4f)),
                                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.15f))
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Copy Clip", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                                            }
                                        }
                                        
                                        // 3. System Share Sheet (More...)
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clickable {
                                                    val intent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_STREAM, currentVideo.uri)
                                                        type = "video/mp4"
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    try {
                                                        context.startActivity(Intent.createChooser(intent, "Share Dual-Cam Clip"))
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "System share failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .testTag("share_more"),
                                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.4f)),
                                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.15f))
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(imageVector = Icons.Rounded.OpenInNew, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("More...", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                                            }
                                        }
                                    }
                                    
                                    // 4. Copy Drive Link (Conditional, if backed up!)
                                    val isBackedUp = BackupManager.isBackedUp(currentVideo.uri)
                                    val driveShareableLink = BackupManager.getShareableLink(currentVideo.uri)
                                    
                                    if (isBackedUp && driveShareableLink != null) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("Google Drive Link", driveShareableLink)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Cloud Google Drive shareable link copied!", Toast.LENGTH_SHORT).show()
                                                }
                                                .testTag("share_copy_cloud_link"),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0EA5E9).copy(alpha = 0.12f)),
                                            border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Rounded.CloudQueue, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Copy Google Drive Share Link",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFF0EA5E9)
                                                    )
                                                }
                                                Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
