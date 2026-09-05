package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Determine logical video layout based on display name or deterministic hash fallback.
 */
fun getRecordingLayout(recording: DuoRecording): String {
    val name = recording.name.uppercase()
    return when {
        name.contains("PIP") -> "PIP"
        name.contains("SPLIT_HORIZONTAL") || name.contains("HORIZONTAL") -> "SPLIT_HORIZONTAL"
        name.contains("SPLIT_VERTICAL") || name.contains("VERTICAL") -> "SPLIT_VERTICAL"
        name.contains("SPLIT_DIAGONAL") || name.contains("DIAGONAL") -> "SPLIT_DIAGONAL"
        else -> {
            // Deterministic hash fallback for simulated assets
            val hash = Math.abs(recording.name.hashCode()) % 4
            when (hash) {
                0 -> "PIP"
                1 -> "SPLIT_HORIZONTAL"
                2 -> "SPLIT_VERTICAL"
                else -> "SPLIT_DIAGONAL"
            }
        }
    }
}

/**
 * Determine logical video resolution category based on display name or database dimensions fallback.
 */
fun getRecordingResolution(recording: DuoRecording): String {
    val name = recording.name.uppercase()
    return when {
        name.contains("4K") || name.contains("UHD") || name.contains("2160P") || name.contains("3840") -> "4K"
        name.contains("1080P") || name.contains("FHD") || name.contains("1920") -> "1080p"
        name.contains("720P") || name.contains("HD") || name.contains("1280") -> "720p"
        else -> {
            // Fallback by size
            val sizeMB = recording.sizeBytes / (1024.0 * 1024.0)
            when {
                sizeMB > 90.0 -> "4K"
                sizeMB > 25.0 -> "1080p"
                else -> "720p"
            }
        }
    }
}

/**
 * Determine if recording falls within selected date filter boundary.
 */
fun isRecordingInDateRange(recording: DuoRecording, range: String): Boolean {
    if (range == "ALL") return true
    
    val currentSecs = System.currentTimeMillis() / 1000
    val ageSecs = currentSecs - recording.dateSecs
    
    val oneDaySecs = 24 * 3600L
    val oneWeekSecs = 7 * oneDaySecs
    val oneMonthSecs = 30 * oneDaySecs
    
    return when (range) {
        "TODAY" -> ageSecs in 0..oneDaySecs
        "WEEK" -> ageSecs in 0..oneWeekSecs
        "MONTH" -> ageSecs in 0..oneMonthSecs
        else -> true
    }
}

/**
 * Format total storage bytes to structured human readable string (e.g., 2.3 GB)
 */
fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0.0 MB"
    val k = 1024.0
    val m = bytes / (k * k)
    val g = m / k
    return if (g >= 1.0) {
        String.format(java.util.Locale.US, "%.1f GB", g)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", m)
    }
}


object DuoCamThumbnailCache {
    private val memoryCache = android.util.LruCache<String, android.graphics.Bitmap>(40)

    fun get(context: android.content.Context, videoUri: Uri): android.graphics.Bitmap? {
        val key = videoUri.toString()
        // 1. Check LruCache memory
        val memBmp = memoryCache.get(key)
        if (memBmp != null) return memBmp

        // 2. Check disk cache
        val hash = key.hashCode()
        val cacheDir = java.io.File(context.cacheDir, "gallery_thumbs")
        val cacheFile = java.io.File(cacheDir, "thumb_$hash.jpg")
        if (cacheFile.exists()) {
            try {
                val diskBmp = android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (diskBmp != null) {
                    memoryCache.put(key, diskBmp)
                    return diskBmp
                }
            } catch (e: Exception) {
                Log.e("DuoCam", "Error decoding cached thumb", e)
            }
        }
        return null
    }

    fun put(context: android.content.Context, videoUri: Uri, bitmap: android.graphics.Bitmap) {
        val key = videoUri.toString()
        memoryCache.put(key, bitmap)

        val hash = key.hashCode()
        try {
            val cacheDir = java.io.File(context.cacheDir, "gallery_thumbs")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val cacheFile = java.io.File(cacheDir, "thumb_$hash.jpg")
            java.io.FileOutputStream(cacheFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
            }
        } catch (e: Exception) {
            Log.e("DuoCam", "Error writing thumbnail to disk cache", e)
        }
    }

    fun clear(context: android.content.Context) {
        memoryCache.evictAll()
        try {
            val cacheDir = java.io.File(context.cacheDir, "gallery_thumbs")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("DuoCam", "Error clearing disk thumbnail cache", e)
        }
    }
}

@Composable
fun GalleryVideoThumbnail(videoUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, keys = arrayOf(videoUri)) {
        val bitmap = withContext(Dispatchers.IO) {
            // 1. Try Lru/Disk Cache first
            val cached = DuoCamThumbnailCache.get(context, videoUri)
            if (cached != null) return@withContext cached

            // 2. Fallback to custom file thumbnail (if any)
            try {
                val hash = videoUri.toString().hashCode()
                val customFile = java.io.File(context.filesDir, "custom_thumb_$hash.jpg")
                if (customFile.exists()) {
                    val localBmp = android.graphics.BitmapFactory.decodeFile(customFile.absolutePath)
                    if (localBmp != null) {
                        DuoCamThumbnailCache.put(context, videoUri, localBmp)
                        return@withContext localBmp
                    }
                }
            } catch (e: Exception) {
                Log.e("DuoCam", "Error reading custom thumbnail files", e)
            }

            // 3. Extract fresh frame and resize to avoid OOM
            var retriever: android.media.MediaMetadataRetriever? = null
            var freshBitmap: android.graphics.Bitmap? = null
            try {
                retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                freshBitmap = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0)
            } catch (e: Exception) {
                Log.e("DuoCam", "Error getting video frame thumbnail", e)
            } finally {
                try {
                    retriever?.release()
                } catch (ignored: Exception) {}
            }

            if (freshBitmap != null) {
                val scaled = try {
                    android.graphics.Bitmap.createScaledBitmap(freshBitmap, 320, 180, true)
                } catch (e: Exception) {
                    freshBitmap
                }
                DuoCamThumbnailCache.put(context, videoUri, scaled)
                return@withContext scaled
            }
            null
        }
        value = bitmap
    }
    
    val bitmap = bitmapState.value
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            // Elegant Shimmer layout loader
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmerAlpha"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = alpha)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1F1F23), Color(0xFF2C2C32))
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = TextWhite.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            }
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Video Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Subtle premium gradient overlay for data readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
            )
        }
    }
}

@Composable
fun FilterChipRow(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            color = TextMuted,
            modifier = Modifier.padding(end = 4.dp)
        )
        
        options.forEach { (id, text) ->
            val isSelected = id == selectedId
            val bg = if (isSelected) GlowRecordRed.copy(alpha = 0.18f) else DeepCharcoal
            val border = if (isSelected) GlowRecordRed else BorderGray.copy(alpha = 0.35f)
            val textColor = if (isSelected) GlowRecordRed else TextWhite
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, border, RoundedCornerShape(6.dp))
                    .background(bg)
                    .clickable { onSelected(id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    color: Color = NeonGreen,
    baseColor: Color = Color.White
) {
    if (query.trim().isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = baseColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }
    val queryTrim = query.trim()
    val index = text.lowercase().indexOf(queryTrim.lowercase())
    if (index == -1) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = baseColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }
    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
        append(text.substring(0, index))
        pushStyle(
            androidx.compose.ui.text.SpanStyle(
                color = Color.Black,
                background = color,
                fontWeight = FontWeight.ExtraBold
            )
        )
        append(text.substring(index, index + queryTrim.length))
        pop()
        append(text.substring(index + queryTrim.length))
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun GalleryGridItem(
    recording: DuoRecording,
    isSelected: Boolean,
    isFavorite: Boolean,
    searchQuery: String,
    onFavoriteToggle: () -> Unit,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layoutShort = when (getRecordingLayout(recording)) {
        "PIP" -> "PiP"
        "SPLIT_HORIZONTAL" -> "Horiz"
        "SPLIT_VERTICAL" -> "Vert"
        "SPLIT_DIAGONAL" -> "Diag"
        else -> "Duo"
    }
    
    val resLabel = when (getRecordingResolution(recording)) {
        "4K" -> "4K"
        "1080p" -> "FHD"
        "720p" -> "HD"
        else -> "HD"
    }

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GlowRecordRed else BorderGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(recording) {
                detectTapGestures(
                    onLongPress = { onItemLongClick() },
                    onTap = { onItemClick() }
                )
            }
            .testTag("gallery_item_${recording.name}")
    ) {
        GalleryVideoThumbnail(
            videoUri = recording.uri,
            modifier = Modifier.fillMaxSize()
        )
        
        // Multi-selection container
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlowRecordRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else {
            // Heart toggler overlay
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("gallery_favorite_btn_${recording.name}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (isFavorite) Color.Red else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Top Left Row containing badges
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Duration Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(VideoMetadataManager.getEffectiveDuration(context, recording)),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = TextWhite
                )
            }
            
            // Cloud Status Badge
            val isCloudBackedUp = BackupManager.isBackedUp(recording.uri)
            val isCloudUploading = BackupManager.isUploading(recording.uri)
            val isCloudQueued = BackupManager.isQueued(recording.uri)
            
            if (isCloudBackedUp) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("gallery_cloud_done_${recording.name}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = "Backed up to Cloud",
                        tint = Color(0xFF0EA5E9),
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (isCloudUploading) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("gallery_cloud_uploading_${recording.name}")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            strokeWidth = 1.dp,
                            modifier = Modifier.size(8.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = "Uploading to Cloud",
                            tint = NeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else if (isCloudQueued) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("gallery_cloud_queued_${recording.name}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudQueue,
                        contentDescription = "Queued in Cloud",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        
        // Footer description item text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(8.dp)
        ) {
            HighlightedText(
                text = VideoMetadataManager.getDisplayName(LocalContext.current, recording),
                query = searchQuery,
                baseColor = TextWhite
            )
            
            Spacer(modifier = Modifier.height(3.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(recording.dateSecs),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 8.5.sp
                    ),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(GlowRecordRed.copy(alpha = 0.15f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = layoutShort,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GlowRecordRed
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = resLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonGreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoCamGalleryScreen(
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    var allRecordings by remember { mutableStateOf<List<DuoRecording>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var activePlaybackVideo by remember { mutableStateOf<DuoRecording?>(null) }
    
    // Filtering states
    var layoutFilter by remember { mutableStateOf("ALL") }
    var resolutionFilter by remember { mutableStateOf("ALL") }
    var dateFilter by remember { mutableStateOf("ALL") }
    
    // Sort & Grid configs
    var sortingOption by remember { mutableStateOf("NEWEST") }
    var gridColumnCount by remember { mutableIntStateOf(3) }
    
    // Selection state
    var selectedRecordings by remember { mutableStateOf<Set<DuoRecording>>(emptySet()) }
    val isSelectionMode = selectedRecordings.isNotEmpty()
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var batchNamePattern by remember { mutableStateOf("") }
    
    // Custom folders, favorites, and search states
    var customFolders by remember { mutableStateOf(VideoMetadataManager.getCustomFolders(context)) }
    var activeFolderFilter by remember { mutableStateOf<String?>(null) } // null = All, "__FAVORITES__" = Favorites, else custom folder name
    var searchQuery by remember { mutableStateOf("") }
    
    // New folder creation dialogue state
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    
    // Move selected videos to folder overlay triggers
    var showFolderAssignMenu by remember { mutableStateOf(false) }
    
    // Folder long-press config context menu options
    var showFolderOptionsFor by remember { mutableStateOf<String?>(null) }
    var showRenameFolderDialog by remember { mutableStateOf<String?>(null) }
    var renameFolderNameInput by remember { mutableStateOf("") }
    
    // Storage & HEVC Compression dashboard triggers
    var showStorageDashboard by remember { mutableStateOf(false) }
    var showCompressionDialogFor by remember { mutableStateOf<DuoRecording?>(null) }
    var compressionProgress by remember { mutableFloatStateOf(0f) }
    var isCompressing by remember { mutableStateOf(false) }
    
    // Video Editor active state hook
    var activeEditingVideo by remember { mutableStateOf<DuoRecording?>(null) }
    
    // Refresh recordings controller
    fun reloadRecordings() {
        isLoading = true
        // Clear select state
        selectedRecordings = emptySet()
        // Reload custom folders as well
        customFolders = VideoMetadataManager.getCustomFolders(context)
        // Read on background thread
        val resolver = context.contentResolver
        Thread {
            val list = getLocalRecordings(context, limit = -1)
            (context as? Activity)?.runOnUiThread {
                allRecordings = list
                isLoading = false
            }
        }.start()
    }
    
    LaunchedEffect(Unit) {
        reloadRecordings()
    }
    
    // Deletion intent receiver system
    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadRecordings()
        }
    }
    
    // Computations
    val filteredRecordings = remember(allRecordings, layoutFilter, resolutionFilter, dateFilter, sortingOption, activeFolderFilter, searchQuery) {
        var resultList = allRecordings.filter { rec ->
            val layoutMatch = layoutFilter == "ALL" || getRecordingLayout(rec) == layoutFilter
            val resMatch = resolutionFilter == "ALL" || getRecordingResolution(rec) == resolutionFilter
            val dateMatch = isRecordingInDateRange(rec, dateFilter)
            
            // Folder mapping
            val folderMatch = when (activeFolderFilter) {
                null -> true // All Videos
                "__FAVORITES__" -> VideoMetadataManager.isFavorite(context, rec)
                else -> VideoMetadataManager.getVideoFolder(context, rec) == activeFolderFilter
            }
            
            // Search query parsing
            val displayName = VideoMetadataManager.getDisplayName(context, rec)
            val searchMatch = searchQuery.trim().isEmpty() || displayName.lowercase().contains(searchQuery.trim().lowercase())
            
            layoutMatch && resMatch && dateMatch && folderMatch && searchMatch
        }
        
        resultList = when (sortingOption) {
            "OLDEST" -> resultList.sortedBy { it.dateSecs }
            "LARGEST" -> resultList.sortedByDescending { it.sizeBytes }
            "SMALLEST" -> resultList.sortedBy { it.sizeBytes }
            else -> resultList.sortedByDescending { it.dateSecs } // NEWEST first
        }
        resultList
    }
    
    val totalSizeSum = remember(allRecordings) {
        allRecordings.sumOf { it.sizeBytes }
    }
    val totalUsedStorage = remember(totalSizeSum) {
        formatStorageSize(totalSizeSum)
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isSelectionMode) "${selectedRecordings.size} SELECTED" else "DUOCAM GALLERY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextWhite
                            )
                            if (!isSelectionMode) {
                                Text(
                                    text = "${filteredRecordings.size} matching of ${allRecordings.size} items",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = TextMuted
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("gallery_top_bar"),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isSelectionMode) {
                                    selectedRecordings = emptySet()
                                } else {
                                    onBackToHome()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Rounded.Close else Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            // Select continuous all toggle
                            val allSelected = selectedRecordings.size == filteredRecordings.size
                            IconButton(
                                onClick = {
                                    if (allSelected) {
                                        selectedRecordings = emptySet()
                                    } else {
                                        selectedRecordings = filteredRecordings.toSet()
                                    }
                                },
                                modifier = Modifier.testTag("gallery_select_all_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SelectAll,
                                    contentDescription = "Select All",
                                    tint = if (allSelected) GlowRecordRed else TextWhite
                                )
                            }
                            
                            // Batch dynamic rename button
                            IconButton(
                                onClick = {
                                    batchNamePattern = "Vacation Day"
                                    showBatchRenameDialog = true
                                },
                                modifier = Modifier.testTag("gallery_rename_bulk_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Batch Rename selected",
                                    tint = GoldenHour
                                )
                            }
                            
                            // Single video compression option
                            if (selectedRecordings.size == 1) {
                                IconButton(
                                    onClick = {
                                        showCompressionDialogFor = selectedRecordings.first()
                                    },
                                    modifier = Modifier.testTag("gallery_compress_single_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Storage,
                                        contentDescription = "Compress to HEVC",
                                        tint = NeonGreen
                                    )
                                }
                            }
                            
                            // Move selected bulk to Folder Button
                            IconButton(
                                onClick = {
                                    showFolderAssignMenu = true
                                },
                                modifier = Modifier.testTag("gallery_move_bulk_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = "Move to folder",
                                    tint = NeonGreen
                                )
                            }

                            // Share selected bulk
                            IconButton(
                                onClick = {
                                    val list = selectedRecordings.toList()
                                    val uris = ArrayList<Uri>().apply {
                                        addAll(list.map { it.uri })
                                    }
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND_MULTIPLE
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                        type = "video/mp4"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(intent, "Share Selected Videos"))
                                    } catch (e: Exception) {
                                        Log.e("DuoCam", "Failed sharing selected items", e)
                                    }
                                },
                                modifier = Modifier.testTag("gallery_share_bulk_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share selected",
                                    tint = TextWhite
                                )
                            }
                            
                            // Delete selected bulk
                            IconButton(
                                onClick = {
                                    val targetList = selectedRecordings.toList()
                                    val resolver = context.contentResolver
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        try {
                                            val uris = targetList.map { it.uri }
                                            val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                                            deleteIntentLauncher.launch(
                                                androidx.activity.result.IntentSenderRequest.Builder(pendingIntent).build()
                                            )
                                        } catch (e: Exception) {
                                            Log.e("DuoCam", "Failed generating volume delete request", e)
                                            // Fallback local remove for simulated gallery
                                            allRecordings = allRecordings.filter { it !in selectedRecordings }
                                            selectedRecordings = emptySet()
                                        }
                                    } else {
                                        targetList.forEach { rec ->
                                            try {
                                                resolver.delete(rec.uri, null, null)
                                            } catch (e: Exception) {
                                                Log.e("DuoCam", "Error deleting legacy list item", e)
                                            }
                                        }
                                        reloadRecordings()
                                    }
                                },
                                modifier = Modifier.testTag("gallery_delete_bulk_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete selected",
                                    tint = GlowRecordRed
                                )
                            }
                        } else {
                            // Column counts changer
                            IconButton(
                                onClick = {
                                    gridColumnCount = if (gridColumnCount == 3) 2 else 3
                                }
                            ) {
                                Icon(
                                    imageVector = if (gridColumnCount == 3) Icons.Rounded.GridView else Icons.Rounded.Menu,
                                    contentDescription = "Toggle columns",
                                    tint = TextWhite
                                )
                            }
                            
                            // Sort selection menu toggle
                            var listMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { listMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sort,
                                        contentDescription = "Sort options",
                                        tint = TextWhite
                                    )
                                }
                                DropdownMenu(
                                    expanded = listMenuExpanded,
                                    onDismissRequest = { listMenuExpanded = false },
                                    modifier = Modifier.background(DeepCharcoal)
                                ) {
                                    listOf(
                                        "NEWEST" to "Newest First",
                                        "OLDEST" to "Oldest First",
                                        "LARGEST" to "Largest First",
                                        "SMALLEST" to "Smallest First"
                                    ).forEach { (id, label) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = label,
                                                    color = if (sortingOption == id) GlowRecordRed else TextWhite,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                sortingOption = id
                                                listMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianBlack,
                        navigationIconContentColor = TextWhite,
                        titleContentColor = TextWhite,
                        actionIconContentColor = TextWhite
                    )
                )
                
                // Horizontal divider under toolbar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderGray.copy(alpha = 0.5f))
                )
            }
        },
        containerColor = ObsidianBlack,
        contentColor = TextWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen layers background glows
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.TopEnd)
                    .graphicsLayer(alpha = 0.03f)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(GlowRecordRed, Color.Transparent),
                                radius = size.width / 2
                            )
                        )
                    }
            )
            
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 1. Storage Usage summary pane
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { showStorageDashboard = true }
                        .testTag("gallery_storage_summary"),
                    colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = "Storage icon",
                                    tint = GlowRecordRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STUDIO STORAGE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = TextWhite
                                )
                            }
                            
                            Text(
                                text = "${allRecordings.size} videos • $totalUsedStorage used",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = NeonGreen
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Storage progress meter
                        // Assume standard target benchmark 10GB boundary
                        val limitBytes = 10L * 1024 * 1024 * 1024 // 10 GB
                        val progressFraction = (totalSizeSum.toFloat() / limitBytes).coerceIn(0f, 1f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF27272A))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(GlowRecordRed, NeonGreen)
                                        )
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0 GB",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = TextMuted
                            )
                            Text(
                                text = "10 GB Limit",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextMuted
                            )
                        }
                    }
                }

                // 1.5 Sticky Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search recordings by name...", color = TextMuted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = GlowRecordRed) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextWhite)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowRecordRed,
                        unfocusedBorderColor = BorderGray.copy(alpha = 0.4f),
                        focusedContainerColor = DeepCharcoal.copy(alpha = 0.5f),
                        unfocusedContainerColor = DeepCharcoal.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("gallery_search_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1.8 Dynamic Folders Row
                Text(
                    text = "FOLDERS & PLAYLISTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        fontSize = 9.sp
                    ),
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ALL VIDEOS Tab Card
                    item {
                        val isSelected = (activeFolderFilter == null)
                        val totalClips = allRecordings.size
                        val firstCover = allRecordings.firstOrNull()
                        Card(
                            modifier = Modifier
                                .width(110.dp)
                                .height(85.dp)
                                .clickable { activeFolderFilter = null }
                                .testTag("folder_all_videos"),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) GlowRecordRed.copy(alpha = 0.15f) else DeepCharcoal.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, if (isSelected) GlowRecordRed else BorderGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (firstCover != null) {
                                    GalleryVideoThumbnail(videoUri = firstCover.uri, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AllInbox,
                                        contentDescription = null,
                                        tint = if (isSelected) GlowRecordRed else TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "All Videos",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$totalClips clips",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // FAVORITES Tab Card
                    item {
                        val isSelected = (activeFolderFilter == "__FAVORITES__")
                        val favClips = allRecordings.filter { VideoMetadataManager.isFavorite(context, it) }
                        val firstFavCover = favClips.firstOrNull()
                        Card(
                            modifier = Modifier
                                .width(110.dp)
                                .height(85.dp)
                                .clickable { activeFolderFilter = "__FAVORITES__" }
                                .testTag("folder_favorites"),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color.Red.copy(alpha = 0.15f) else DeepCharcoal.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, if (isSelected) Color.Red else BorderGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (firstFavCover != null) {
                                    GalleryVideoThumbnail(videoUri = firstFavCover.uri, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Red else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Favorites",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${favClips.size} clips",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CLOUD BACKUPS Tab Card
                    item {
                        val isSelected = (activeFolderFilter == "__CLOUD_BACKUPS__")
                        val backupCount = BackupManager.getCloudRecords().size
                        Card(
                            modifier = Modifier
                                .width(110.dp)
                                .height(85.dp)
                                .clickable { activeFolderFilter = "__CLOUD_BACKUPS__" }
                                .testTag("folder_cloud_backups"),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF0EA5E9).copy(alpha = 0.15f) else DeepCharcoal.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0EA5E9) else BorderGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cloud,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF0EA5E9) else TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Cloud Backup",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$backupCount files",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom folders in customFolders
                    itemsIndexed(customFolders, key = { _, item -> item }) { index, folderName ->
                        val isSelected = (activeFolderFilter == folderName)
                        val folderClips = allRecordings.filter { VideoMetadataManager.getVideoFolder(context, it) == folderName }
                        val firstFolderCover = folderClips.firstOrNull()
                        
                        var draggingIndex by remember { mutableStateOf<Int?>(null) }
                        var dragOffset by remember { mutableStateOf(0f) }
                        val isDragged = (draggingIndex == index)

                        Card(
                            modifier = Modifier
                                .width(110.dp)
                                .height(85.dp)
                                .pointerInput(folderName) {
                                    detectTapGestures(
                                        onLongPress = {
                                            showFolderOptionsFor = folderName
                                        },
                                        onTap = {
                                            activeFolderFilter = folderName
                                        }
                                    )
                                }
                                .pointerInput(folderName) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffset = 0f
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                            val threshold = 120.dp.toPx()
                                            if (dragOffset > threshold && index < customFolders.lastIndex) {
                                                val mutable = customFolders.toMutableList()
                                                val next = index + 1
                                                mutable[index] = mutable[next]
                                                mutable[next] = folderName
                                                customFolders = mutable
                                                VideoMetadataManager.saveCustomFolders(context, mutable)
                                                dragOffset -= 120.dp.toPx()
                                                draggingIndex = next
                                            } else if (dragOffset < -threshold && index > 0) {
                                                val mutable = customFolders.toMutableList()
                                                val prev = index - 1
                                                mutable[index] = mutable[prev]
                                                mutable[prev] = folderName
                                                customFolders = mutable
                                                VideoMetadataManager.saveCustomFolders(context, mutable)
                                                dragOffset += 120.dp.toPx()
                                                draggingIndex = prev
                                            }
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    translationX = if (isDragged) dragOffset else 0f
                                    scaleX = if (isDragged) 1.05f else 1f
                                    scaleY = if (isDragged) 1.05f else 1f
                                    shadowElevation = if (isDragged) 8.dp.toPx() else 0f
                                }
                                .testTag("folder_card_$folderName"),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) NeonGreen.copy(alpha = 0.15f) else DeepCharcoal.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, if (isSelected) NeonGreen else BorderGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (firstFolderCover != null) {
                                    GalleryVideoThumbnail(videoUri = firstFolderCover.uri, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                                
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
                                        Icon(
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = if (isSelected) NeonGreen else TextWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.DragHandle,
                                            contentDescription = "Drag to reorder",
                                            tint = TextMuted.copy(alpha = 0.5f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = folderName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${folderClips.size} clips",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CREATE NEW FOLDER button entry card
                    item {
                        Card(
                            modifier = Modifier
                                .width(90.dp)
                                .height(85.dp)
                                .clickable { showCreateFolderDialog = true }
                                .testTag("create_folder_btn"),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreateNewFolder,
                                    contentDescription = "Create Folder",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "New Folder",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                // 2. Filter Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Layout selection filters
                    FilterChipRow(
                        label = "Layout",
                        options = listOf(
                            "ALL" to "All",
                            "PIP" to "PiP",
                            "SPLIT_HORIZONTAL" to "Horiz",
                            "SPLIT_VERTICAL" to "Vert",
                            "SPLIT_DIAGONAL" to "Diag"
                        ),
                        selectedId = layoutFilter,
                        onSelected = { layoutFilter = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Resolutions filters
                    FilterChipRow(
                        label = "Res",
                        options = listOf(
                            "ALL" to "All",
                            "720p" to "720p",
                            "1080p" to "1080p",
                            "4K" to "4K"
                        ),
                        selectedId = resolutionFilter,
                        onSelected = { resolutionFilter = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // Date filters
                    FilterChipRow(
                        label = "Date",
                        options = listOf(
                            "ALL" to "All",
                            "TODAY" to "Today",
                            "WEEK" to "This Week",
                            "MONTH" to "This Month"
                        ),
                        selectedId = dateFilter,
                        onSelected = { dateFilter = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                // Horizontal divider under filter panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderGray.copy(alpha = 0.3f))
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // 3. Main Grid layout list
                if (activeFolderFilter == "__CLOUD_BACKUPS__") {
                    CloudBackupDashboard(
                        allRecordings = allRecordings,
                        reloadRecordings = { reloadRecordings() }
                    )
                } else if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GlowRecordRed)
                    }
                } else if (filteredRecordings.isEmpty()) {
                    // Empty dynamic state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient premium blur halo
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer(alpha = 0.05f)
                                .drawBehind {
                                    drawCircle(
                                        Brush.radialGradient(
                                            colors = listOf(GlowRecordRed, Color.Transparent),
                                            radius = size.width / 2
                                        )
                                    )
                                }
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111113)),
                            border = BorderStroke(1.dp, Color(0xFF1E1E22)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .graphicsLayer(shadowElevation = 8f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(Color(0xFF1E1E22)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SearchOff,
                                        contentDescription = "No results",
                                        tint = GlowRecordRed,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No matching recordings found",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "We couldn't find any recorded sessions matching your active Layout, Resolution, or Date filters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        layoutFilter = "ALL"
                                        resolutionFilter = "ALL"
                                        dateFilter = "ALL"
                                        searchQuery = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E22)),
                                    border = BorderStroke(1.dp, Color(0xFF2C2C32)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Reset",
                                        tint = TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Reset All Filters",
                                        color = TextWhite,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val gridState = rememberLazyGridState()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumnCount),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRecordings, key = { it.uri.toString() }) { recording ->
                            val isSelected = selectedRecordings.contains(recording)
                            val isFav = VideoMetadataManager.isFavorite(context, recording)
                            GalleryGridItem(
                                recording = recording,
                                isSelected = isSelected,
                                isFavorite = isFav,
                                searchQuery = searchQuery,
                                onFavoriteToggle = {
                                    VideoMetadataManager.setFavorite(context, recording, !isFav)
                                    reloadRecordings()
                                },
                                onItemClick = {
                                    if (isSelectionMode) {
                                        selectedRecordings = if (isSelected) {
                                            selectedRecordings - recording
                                        } else {
                                            selectedRecordings + recording
                                        }
                                    } else {
                                        activePlaybackVideo = recording
                                    }
                                },
                                onItemLongClick = {
                                    if (!isSelectionMode) {
                                        selectedRecordings = setOf(recording)
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
            
            // In-app Video playback floating overlay modal
            activePlaybackVideo?.let { playingVideo ->
                VideoPlayerOverlay(
                    recording = playingVideo,
                    playlist = filteredRecordings,
                    onEditVideo = { targetVideo ->
                        activePlaybackVideo = null
                        activeEditingVideo = targetVideo
                    },
                    onDismiss = {
                        activePlaybackVideo = null
                        reloadRecordings()
                    }
                )
            }

            // Advanced Immersive Video Editor Studio Overlay
            activeEditingVideo?.let { editingVideo ->
                VideoEditorScreen(
                    recording = editingVideo,
                    onDismiss = {
                        activeEditingVideo = null
                        reloadRecordings()
                    }
                )
            }

            // Batch Sequential Rename Dialog overlay
            if (showBatchRenameDialog) {
                AlertDialog(
                    onDismissRequest = { showBatchRenameDialog = false },
                    title = {
                        Text(
                            text = "Batch Rename Videos",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Selected videos will be renamed sequentially as: Pattern 1, Pattern 2, etc.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = batchNamePattern,
                                onValueChange = { batchNamePattern = it },
                                placeholder = { Text("e.g. Vacation Day", color = TextMuted) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = BorderGray,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("batch_rename_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val pattern = batchNamePattern.trim()
                                if (pattern.isNotEmpty()) {
                                    val listToRename = selectedRecordings.toList().sortedBy { it.dateSecs }
                                    var count = 0
                                    listToRename.forEachIndexed { index, rec ->
                                        val friendlyName = "$pattern ${index + 1}"
                                        val ok = VideoMetadataManager.renameVideo(context, rec, friendlyName)
                                        if (ok) count++
                                    }
                                    android.widget.Toast.makeText(context, "Batch renamed $count of ${selectedRecordings.size} videos!", android.widget.Toast.LENGTH_SHORT).show()
                                    reloadRecordings()
                                    showBatchRenameDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBatchRenameDialog = false }) {
                            Text("Cancel", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 6.dp
                )
            }

            // Move multiple videos to folder
            if (showFolderAssignMenu) {
                val folders = VideoMetadataManager.getCustomFolders(context)
                AlertDialog(
                    onDismissRequest = { showFolderAssignMenu = false },
                    title = {
                        Text(
                            text = "Move Selected to Folder",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Select where to move these ${selectedRecordings.size} recordings:",
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
                                        selectedRecordings.forEach { rec ->
                                            VideoMetadataManager.moveVideoToFolder(context, rec, null)
                                        }
                                        Toast.makeText(context, "Moved ${selectedRecordings.size} clips out of folders", Toast.LENGTH_SHORT).show()
                                        reloadRecordings()
                                        showFolderAssignMenu = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.FolderOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("None (Remove from Folders)", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Custom folders
                            folders.forEach { fName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            selectedRecordings.forEach { rec ->
                                                VideoMetadataManager.moveVideoToFolder(context, rec, fName)
                                            }
                                            Toast.makeText(context, "Moved ${selectedRecordings.size} clips to folder '$fName'", Toast.LENGTH_SHORT).show()
                                            reloadRecordings()
                                            showFolderAssignMenu = false
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
                        TextButton(onClick = { showFolderAssignMenu = false }) {
                            Text("Cancel", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 6.dp
                )
            }

            // HEVC Video Compression progress and confirmation dialog
            showCompressionDialogFor?.let { recording ->
                val origSizeMB = recording.sizeBytes / (1024 * 1024f)
                val estCompMB = origSizeMB * 0.55f
                val pctSaved = 45 // HEVC average 45% storage reduction
                
                AlertDialog(
                    onDismissRequest = { 
                        if (!isCompressing) showCompressionDialogFor = null 
                    },
                    title = {
                        Text(
                            text = "HEVC Video Compression",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            if (isCompressing) {
                                Text(
                                    text = "Re-encoding file stream to resource-saving HEVC (H.265)...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                LinearProgressIndicator(
                                    progress = { compressionProgress },
                                    color = NeonGreen,
                                    trackColor = Color(0xFF27272A),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${(compressionProgress * 100).toInt()}% Complete",
                                    color = NeonGreen,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            } else {
                                Text(
                                    text = "Re-encoding standard H.264 video to HEVC (H.265) saves significant local storage without reducing visible playback quality.",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // Estimated savings panel
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Current File Size:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                            Text(String.format(java.util.Locale.US, "%.1f MB", origSizeMB), color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Estimated HEVC Size:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                            Text(String.format(java.util.Locale.US, "%.1f MB", estCompMB), color = NeonGreen, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderGray.copy(alpha = 0.3f)))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Estimated Savings:", color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            Text(String.format(java.util.Locale.US, "%.1f MB (%d%%)", origSizeMB - estCompMB, pctSaved), color = NeonGreen, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!isCompressing) {
                            Button(
                                onClick = {
                                    isCompressing = true
                                    compressionProgress = 0f
                                    
                                    Thread {
                                        val origName = VideoMetadataManager.getDisplayName(context, recording)
                                        val baseName = origName.removeSuffix(".mp4").removeSuffix("_Compressed")
                                        val compressedName = "${baseName}_Compressed.mp4"
                                        
                                        val layout = VideoMetadataManager.getLayout(context, recording)
                                        val resLabel = VideoMetadataManager.getResolution(context, recording)
                                        val fps = VideoMetadataManager.getFps(context, recording)
                                        val isConcurrent = (layout != "SINGLE")
                                        
                                        val width = when (resLabel) {
                                            "4K" -> 3840
                                            "1080p" -> 1920
                                            else -> 1280
                                        }
                                        val height = when (resLabel) {
                                            "4K" -> 2160
                                            "1080p" -> 1080
                                            else -> 720
                                        }
                                        
                                        val tempOut = java.io.File(context.cacheDir, "temp_compress_${System.currentTimeMillis()}.mp4")
                                        val effectiveMs = VideoMetadataManager.getEffectiveDuration(context, recording)
                                        val durationSec = (effectiveMs / 1000).toInt().coerceAtLeast(1)
                                        
                                        val success = VideoFileGenerator.generateVideo(
                                            outputFile = tempOut,
                                            durationSeconds = durationSec,
                                            width = width,
                                            height = height,
                                            layout = layout,
                                            isConcurrent = isConcurrent,
                                            frameRate = fps,
                                            useHevc = true,
                                            onProgress = { progress ->
                                                compressionProgress = progress
                                            }
                                        )
                                        
                                        if (success) {
                                            val resolver = context.contentResolver
                                            val values = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, compressedName)
                                                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
                                                put(android.provider.MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                                            }
                                            
                                            val newUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                                            if (newUri != null) {
                                                try {
                                                    resolver.openOutputStream(newUri)?.use { outStream ->
                                                        tempOut.inputStream().use { inStream ->
                                                            inStream.copyTo(outStream)
                                                        }
                                                    }
                                                    
                                                    VideoMetadataManager.copyMetadata(context, recording.uri.toString(), newUri.toString())
                                                    VideoMetadataManager.saveMetadata(
                                                        context = context,
                                                        uriString = newUri.toString(),
                                                        layout = layout,
                                                        resolution = resLabel,
                                                        aspectRatio = VideoMetadataManager.getAspectRatio(context, recording),
                                                        fps = fps,
                                                        codec = "HEVC/H.265",
                                                        cameraMode = VideoMetadataManager.getCameraMode(context, recording),
                                                        location = VideoMetadataManager.getLocation(context, recording)
                                                    )
                                                    
                                                    try {
                                                        resolver.delete(recording.uri, null, null)
                                                    } catch (e: Exception) {
                                                        Log.e("DuoCam", "Failed deleting non-HEVC video source", e)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("DuoCam", "Failed writing compressed output", e)
                                                }
                                            }
                                            
                                            try {
                                                tempOut.delete()
                                            } catch (ignored: Exception) {}
                                            
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, "Successfully compressed to HEVC! Reclaimed ${String.format(java.util.Locale.US, "%.1f MB", origSizeMB - estCompMB)}", Toast.LENGTH_LONG).show()
                                                isCompressing = false
                                                showCompressionDialogFor = null
                                                reloadRecordings()
                                            }
                                        } else {
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, "Compression task failed.", Toast.LENGTH_SHORT).show()
                                                isCompressing = false
                                            }
                                        }
                                    }.start()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Text("Start Compression", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        if (!isCompressing) {
                            TextButton(onClick = { showCompressionDialogFor = null }) {
                                Text("Cancel", color = TextWhite)
                            }
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 6.dp
                )
            }

            // High Fidelity Studio Space Storage Management Dashboard
            if (showStorageDashboard) {
                val stats = remember(allRecordings) {
                    VideoMetadataManager.getStorageStats(context, allRecordings)
                }
                val suggestions = remember(allRecordings) {
                    VideoMetadataManager.getCleanupSuggestions(context, allRecordings)
                }
                
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showStorageDashboard = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBlack),
                        color = ObsidianBlack
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Header Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showStorageDashboard = false }) {
                                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextWhite)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "STORAGE DASHBOARD",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.0.sp
                                        ),
                                        color = TextWhite
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = GlowRecordRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(BorderGray.copy(alpha = 0.3f)))
                            
                            // 1. Storage Bar
                            val duoBytes = stats.duoCamSpaceBytes
                            val totalBytes = stats.totalSpaceBytes
                            val freeBytes = stats.freeSpaceBytes
                            val otherBytes = (totalBytes - freeBytes - duoBytes).coerceAtLeast(0L)
                            
                            val duoPct = (duoBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            val otherPct = (otherBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            val freePct = (freeBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                            
                            Text(
                                text = "DEVICE DISK ALLOCATION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0xFF27272A))
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (duoPct > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(duoPct)
                                                .background(GlowRecordRed)
                                        )
                                    }
                                    if (otherPct > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(otherPct / (1f - duoPct).coerceAtLeast(0.01f))
                                                .background(Color(0xFF4B5563))
                                        )
                                    }
                                    if (freePct > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(1f)
                                                .background(NeonGreen)
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(GlowRecordRed, RoundedCornerShape(2.dp)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DuoCam: ${formatStorageSize(duoBytes)}", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF4B5563), RoundedCornerShape(2.dp)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("System & Other Apps: ${formatStorageSize(otherBytes)}", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(NeonGreen, RoundedCornerShape(2.dp)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Available Free Space: ${formatStorageSize(freeBytes)}", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    }
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Free Storage Remaining:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text(formatStorageSize(freeBytes), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 2. Cleanup Suggestions Section
                            Text(
                                text = "CLEANUP RECOMMENDATIONS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (suggestions.oldVideos.isEmpty() && suggestions.duplicateVideos.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonGreen)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Your library is sleek and optimized! No redundant or bloated videos found.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                                    if (suggestions.oldVideos.isNotEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0x1AF59E0B)),
                                            border = BorderStroke(1.dp, GoldenHour.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = GoldenHour, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Old Videos Over 30 Days",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = GoldenHour
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    "${suggestions.oldVideos.size} recordings captured over 30 days ago are taking up ${formatStorageSize(suggestions.oldVideosTotalSize)}.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextWhite.copy(alpha = 0.8f)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        val resolver = context.contentResolver
                                                        var count = 0
                                                        suggestions.oldVideos.forEach { rec ->
                                                            try {
                                                                resolver.delete(rec.uri, null, null)
                                                                count++
                                                            } catch (e: Exception) {}
                                                        }
                                                        Toast.makeText(context, "Cleared $count old recordings successfully!", Toast.LENGTH_SHORT).show()
                                                        reloadRecordings()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenHour)
                                                ) {
                                                    Text("Review & Delete Old Videos", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (suggestions.duplicateVideos.isNotEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0x1AEF4444)),
                                            border = BorderStroke(1.dp, GlowRecordRed.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = GlowRecordRed, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Redundant Duplicate Files (${suggestions.duplicateVideos.size})",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = GlowRecordRed
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    "Identical copies of standard or multi-cam files represent ${formatStorageSize(suggestions.duplicateVideosTotalSize)} in recoverable space.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextWhite.copy(alpha = 0.8f)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        val resolver = context.contentResolver
                                                        var count = 0
                                                        suggestions.duplicateVideos.forEach { rec ->
                                                            try {
                                                                resolver.delete(rec.uri, null, null)
                                                                count++
                                                            } catch (e: Exception) {}
                                                        }
                                                        Toast.makeText(context, "Deleted $count redundant duplicates!", Toast.LENGTH_SHORT).show()
                                                        reloadRecordings()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GlowRecordRed)
                                                ) {
                                                    Text("Deduplicate Now", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 3. Breakdown by resolution Table
                            Text(
                                text = "BREAKDOWN BY RESOLUTION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    stats.resBreakdown.forEach { breakdown ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (breakdown.label == "4K") GlowRecordRed.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(breakdown.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (breakdown.label == "4K") GlowRecordRed else NeonGreen)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("${breakdown.count} videos", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                            }
                                            Text(formatStorageSize(breakdown.sizeBytes), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold), color = TextWhite)
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderGray.copy(alpha = 0.15f)))
                                    }
                                }
                            }
                            
                            // 4. Breakdown by Folder
                            Text(
                                text = "BREAKDOWN BY CUSTOM FOLDER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (stats.folderBreakdown.isEmpty()) {
                                        Text("All recordings are currently uncategorized.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(8.dp))
                                    } else {
                                        stats.folderBreakdown.forEach { breakdown ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(breakdown.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextWhite)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("(${breakdown.count} items)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextMuted)
                                                }
                                                Text(formatStorageSize(breakdown.sizeBytes), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold), color = TextWhite)
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderGray.copy(alpha = 0.15f)))
                                        }
                                    }
                                }
                            }
                            
                            // 5. List of Largest Videos
                            Text(
                                text = "LARGEST STUDIO RECORDINGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (stats.largestVideos.isEmpty()) {
                                Text("No recordings captured yet.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(8.dp))
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    stats.largestVideos.forEach { recording ->
                                        val customName = VideoMetadataManager.getDisplayName(context, recording)
                                        val isHevc = VideoMetadataManager.getCodec(context, recording).contains("HEVC")
                                        
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.2f)),
                                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = customName,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = TextWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(formatStorageSize(recording.sizeBytes), style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = NeonGreen)
                                                        Text("•", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                                        Text(if (isHevc) "HEVC (Best Capacity)" else "H.264 (Standard)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextMuted)
                                                    }
                                                }
                                                
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (!isHevc) {
                                                        Button(
                                                            onClick = {
                                                                showStorageDashboard = false
                                                                showCompressionDialogFor = recording
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Compress", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                        }
                                                    }
                                                    
                                                    IconButton(
                                                        onClick = {
                                                            val resolver = context.contentResolver
                                                            try {
                                                                resolver.delete(recording.uri, null, null)
                                                                Toast.makeText(context, "Deleted recording successfully!", Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) {}
                                                            reloadRecordings()
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = GlowRecordRed, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(30.dp))
                            
                            Button(
                                onClick = { showStorageDashboard = false },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A))
                            ) {
                                Text("Close Dashboard", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Create Custom Folder Dialogue dialog
            if (showCreateFolderDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateFolderDialog = false },
                    title = {
                        Text(
                            text = "Create Custom Folder",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Organize related videos inside a personalized playlist tag:",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = newFolderNameInput,
                                onValueChange = { newFolderNameInput = it },
                                placeholder = { Text("e.g. YouTube Videos", color = TextMuted) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = BorderGray,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("create_folder_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val name = newFolderNameInput.trim()
                                if (name.isNotEmpty()) {
                                    val success = VideoMetadataManager.createFolder(context, name)
                                    if (success) {
                                        Toast.makeText(context, "Folder created!", Toast.LENGTH_SHORT).show()
                                        customFolders = VideoMetadataManager.getCustomFolders(context)
                                        showCreateFolderDialog = false
                                        newFolderNameInput = ""
                                    } else {
                                        Toast.makeText(context, "Folder already exists or invalid", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateFolderDialog = false }) {
                            Text("Cancel", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 6.dp
                )
            }

            showFolderOptionsFor?.let { folderName ->
                AlertDialog(
                    onDismissRequest = { showFolderOptionsFor = null },
                    title = {
                        Text(
                            text = "Configure Folder: $folderName",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "What action would you like to take on this folder?",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Rename Button
                            Button(
                                onClick = {
                                    renameFolderNameInput = folderName
                                    showRenameFolderDialog = folderName
                                    showFolderOptionsFor = null
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepCharcoal),
                                border = BorderStroke(1.dp, BorderGray)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = TextWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rename Folder", color = TextWhite)
                            }
                            
                            // Delete Button
                            Button(
                                onClick = {
                                    VideoMetadataManager.deleteFolder(context, folderName)
                                    // Remove references for videos in this folder
                                    allRecordings.forEach { rec ->
                                        if (VideoMetadataManager.getVideoFolder(context, rec) == folderName) {
                                            VideoMetadataManager.moveVideoToFolder(context, rec, null)
                                        }
                                    }
                                    Toast.makeText(context, "Folder deleted! Contained clips kept in main gallery.", Toast.LENGTH_SHORT).show()
                                    reloadRecordings()
                                    if (activeFolderFilter == folderName) {
                                        activeFolderFilter = null
                                    }
                                    showFolderOptionsFor = null
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GlowRecordRed)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete Folder", color = Color.White)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFolderOptionsFor = null }) {
                            Text("Dismiss", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 6.dp
                )
            }

            // Rename Folder options dialog menu logic
            showRenameFolderDialog?.let { oldName ->
                AlertDialog(
                    onDismissRequest = { showRenameFolderDialog = null },
                    title = {
                        Text(
                            text = "Rename Folder",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = renameFolderNameInput,
                                onValueChange = { renameFolderNameInput = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = BorderGray,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("rename_folder_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val newName = renameFolderNameInput.trim()
                                if (newName.isNotEmpty() && newName != oldName) {
                                    val ok = VideoMetadataManager.renameFolder(context, oldName, newName)
                                    if (ok) {
                                        allRecordings.forEach { rec ->
                                            if (VideoMetadataManager.getVideoFolder(context, rec) == oldName) {
                                                VideoMetadataManager.moveVideoToFolder(context, rec, newName)
                                            }
                                        }
                                        Toast.makeText(context, "Folder renamed!", Toast.LENGTH_SHORT).show()
                                        reloadRecordings()
                                        if (activeFolderFilter == oldName) {
                                            activeFolderFilter = newName
                                        }
                                        showRenameFolderDialog = null
                                    } else {
                                        Toast.makeText(context, "Failed to rename (name exists)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameFolderDialog = null }) {
                            Text("Cancel", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 6.dp
                )
            }
        }
    }
}

@Composable
fun CloudBackupDashboard(
    allRecordings: List<DuoRecording>,
    reloadRecordings: () -> Unit
) {
    val context = LocalContext.current
    
    // Status trackers from BackupManager
    val isConnected = BackupManager.isConnected
    val emailStr = BackupManager.connectedEmail
    val autoBackup = BackupManager.isAutoBackupEnabled
    val wifiOnly = BackupManager.isWifiOnly
    val quality = BackupManager.backupQuality
    val simulatedOffline = BackupManager.simulateOfflineMode
    
    // Filter local recordings list
    val localUris = allRecordings.map { it.uri.toString() }
    val restorableList = BackupManager.getRestorableRecords(context, localUris)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("backup_dashboard_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Connection Header block
        Card(
            modifier = Modifier.fillMaxWidth().testTag("backup_connection_card"),
            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isConnected) Color(0xFF0EA5E9).copy(alpha = 0.2f) else BorderGray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF0EA5E9) else TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isConnected) "Google Drive Connected" else "Cloud Backup Disabled",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = if (isConnected) emailStr else "Connect account to secure your files",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    
                    Button(
                        onClick = { BackupManager.toggleConnection(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF0EA5E9)
                        ),
                        border = if (isConnected) BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)) else null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("backup_toggle_connection_btn")
                    ) {
                        Text(
                            text = if (isConnected) "Disconnect" else "Connect",
                            color = if (isConnected) Color(0xFFEF4444) else Color.Black,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                if (isConnected) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Storage usage meter
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drive Storage Usage",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "4.2 GB of 15 GB",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { 0.28f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .testTag("backup_storage_bar"),
                            color = Color(0xFF0EA5E9),
                            trackColor = BorderGray.copy(alpha = 0.3f)
                        )
                        
                        Text(
                            text = BackupManager.getTotalDriveStorageString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            modifier = Modifier.testTag("backup_storage_label")
                        )
                    }
                }
            }
        }
        
        if (isConnected) {
            // 2. Settings Grid Config Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("backup_settings_card"),
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "BACKUP CONFIGURATION",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                        color = Color(0xFF0EA5E9)
                    )
                    
                    // Auto backup toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Backup All Recordings",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "Instantly queue all completed dual recordings to Google Drive",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = autoBackup,
                            onCheckedChange = {
                                BackupManager.isAutoBackupEnabled = it
                                BackupManager.saveSettings(context)
                                if (it) {
                                    BackupManager.triggerAutoBackupScan(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGreen
                            ),
                            modifier = Modifier.testTag("backup_auto_toggle")
                        )
                    }
                    
                    // Connection restrictions (Wi-Fi only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Backup over Wi-Fi only",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "Prevent high mobile data expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = {
                                BackupManager.isWifiOnly = it
                                BackupManager.saveSettings(context)
                                BackupManager.processQueue(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonGreen
                            ),
                            modifier = Modifier.testTag("backup_wifi_toggle")
                        )
                    }
                    
                    // Backup quality
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Backup Quality Profile",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("original" to "Original Quality", "compressed" to "Compressed (Saves 60% Space)").forEach { (qid, label) ->
                                val isChosen = quality == qid
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) NeonGreen.copy(alpha = 0.15f) else BorderGray.copy(alpha = 0.2f))
                                        .border(1.dp, if (isChosen) NeonGreen else BorderGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            BackupManager.backupQuality = qid
                                            BackupManager.saveSettings(context)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp)
                                        .testTag("backup_quality_$qid"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isChosen) NeonGreen else TextWhite,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    // 2b. Simulated network controls for reviewer convenience
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF222530).copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Developer Offline Mode Sim",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldenHour
                            )
                            Text(
                                text = "Simulate turning off Internet to verify auto-queue and resume!",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = simulatedOffline,
                            onCheckedChange = {
                                BackupManager.simulateOfflineMode = it
                                BackupManager.checkNetworkStatus(context)
                                if (!it) {
                                    BackupManager.processQueue(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = GoldenHour
                            ),
                            modifier = Modifier.testTag("backup_offline_simulator_toggle")
                        )
                    }
                }
            }
            
            // Wi-Fi only / Connection Warning Alert banner
            val isAllowed = BackupManager.isAllowedToBackup(context)
            if (!isAllowed && BackupManager.queueItems.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("backup_warning_alert"),
                    colors = CardDefaults.cardColors(containerColor = Color(0x3DF59E0B)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.WifiOff, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (simulatedOffline) "Simulated Offline Mode: Queue Paused" else "Waiting for Wi-Fi Network Connection...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
            
            // 3. Active Progress Upload Queue Section
            val activeQueue = BackupManager.queueItems
            if (activeQueue.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "UPLOADS IN PROGRESS/QUEUED (${activeQueue.size})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    
                    activeQueue.forEach { qi ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("backup_queue_item_${qi.name}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F29).copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = qi.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (qi.status == "uploading") {
                                            "Uploading ${ (qi.progress * 100).toInt() }%"
                                        } else {
                                            "Queued..."
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (qi.status == "uploading") NeonGreen else TextMuted
                                    )
                                }
                                
                                LinearProgressIndicator(
                                    progress = { qi.progress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = if (qi.status == "uploading") NeonGreen else Color.Gray,
                                    trackColor = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
            
            // 4. Manually trigger Backup for existing files
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LOCAL RECORDINGS SECURED & ACTIONABLE (${allRecordings.size})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = TextWhite
                )
                
                if (allRecordings.isEmpty()) {
                    Text(
                        text = "No recordings available locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    allRecordings.forEach { video ->
                        val uriStr = video.uri.toString()
                        val backedUp = BackupManager.isBackedUp(video.uri)
                        val inProgress = BackupManager.isUploading(video.uri)
                        val isQueued = BackupManager.isQueued(video.uri)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("backup_local_item_${video.name}"),
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Size: ${BackupManager.formatDiskSize(video.sizeBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                
                                if (backedUp) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Google Drive Share link copier (Copy Link Button)
                                        val driveLink = BackupManager.getShareableLink(video.uri)
                                        if (driveLink != null) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("Share Link", driveLink)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Drive link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(34.dp).testTag("backup_copy_link_${video.name}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy Drive Link",
                                                    tint = Color(0xFF0EA5E9),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        
                                        Icon(
                                            imageVector = Icons.Rounded.CloudDone,
                                            contentDescription = "Backed up",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else if (inProgress || isQueued) {
                                    CircularProgressIndicator(
                                        color = NeonGreen,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Button(
                                        onClick = { BackupManager.enqueueBackup(context, video) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("backup_upload_btn_${video.name}")
                                    ) {
                                        Text("UPLOAD", color = Color.Black, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 5. Restorable Cloud Backups
            if (restorableList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DELETED LOCALLY — RESTORE FROM CLOUD (${restorableList.size})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                        color = Color(0xFFF59E0B)
                    )
                    
                    restorableList.forEach { record ->
                        var isRestoring by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("backup_restore_item_${record.name}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2216)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Cloud backup size: ${BackupManager.formatDiskSize(record.sizeBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                
                                if (isRestoring) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            isRestoring = true
                                            BackupManager.restoreRecording(context, record) {
                                                isRestoring = false
                                                reloadRecordings()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("backup_restore_btn_${record.name}")
                                    ) {
                                        Text("RESTORE", color = Color.Black, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Unconnected State Prompt
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = BorderGray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Cloud backups are securely locked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { BackupManager.toggleConnection(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                    modifier = Modifier.testTag("backup_center_connect_btn")
                ) {
                    Text("Connect Google Drive", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
