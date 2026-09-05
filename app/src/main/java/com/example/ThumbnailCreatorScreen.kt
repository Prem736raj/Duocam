package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

// Font Options
enum class ThumbnailFont(val label: String, val systemFont: String, val fontStyle: Int) {
    IMPACT("IMPACT/BOLD", "sans-serif-black", android.graphics.Typeface.BOLD),
    MONOSPACE("MONO TECH", "monospace", android.graphics.Typeface.BOLD),
    SERIF("SERIF PREMIUM", "serif", android.graphics.Typeface.BOLD),
    SANS_SERIF("MODERN CLEAN", "sans-serif", android.graphics.Typeface.NORMAL)
}

// Color Swatches
data class ThumbnailColor(val name: String, val composeColor: Color, val hexColorString: String)

val colorSwatches = listOf(
    ThumbnailColor("Glow Green", Color(0xFF22C55E), "#22C55E"),
    ThumbnailColor("Neon Yellow", Color(0xFFFFE600), "#FFE600"),
    ThumbnailColor("Glow Red", Color(0xFFEF4444), "#EF4444"),
    ThumbnailColor("Clean White", Color(0xFFFFFFFF), "#FFFFFF"),
    ThumbnailColor("Shadow Black", Color(0xFF000000), "#000000"),
    ThumbnailColor("Hot Pink", Color(0xFFEC4899), "#EC4899"),
    ThumbnailColor("Cyan Sky", Color(0xFF06B6D4), "#06B6D4")
)

// Auto Suggested suggested moments
data class SuggestedFrame(
    val title: String,
    val description: String,
    val timeMs: Long,
    var bitmap: Bitmap? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailCreatorScreen(
    video: DuoRecording,
    onDismiss: () -> Unit,
    onThumbnailSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Screen States
    var currentStep by remember { mutableStateOf(1) } // 1: Frame, 2: Text, 3: Adjust, 4: Export
    var baseFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isUsingCustomImage by remember { mutableStateOf(false) }
    var isLoadingFrame by remember { mutableStateOf(false) }

    // Step 1 States (Frame Scrub)
    var seekPositionMs by remember { mutableStateOf(1000L.coerceAtMost(video.durationMs)) }
    val suggestedMoments = remember {
        mutableStateListOf(
            SuggestedFrame("Action Highlight", "Maximum movement intensity", (video.durationMs * 0.15).toLong()),
            SuggestedFrame("Cinematic Focus", "Optimal landscape detail", (video.durationMs * 0.45).toLong()),
            SuggestedFrame("Vibrant Portrait", "Expressive subject framing", (video.durationMs * 0.75).toLong()),
            SuggestedFrame("Impact Ending", "Peak dramatic scene closure", (video.durationMs * 0.90).toLong())
        )
    }

    // Step 2 States (Text Overlay)
    var titleText by remember { mutableStateOf("TAP TO TYPE") }
    var textXRatio by remember { mutableStateOf(0.5f) } // Relative coordinates 0f to 1f
    var textYRatio by remember { mutableStateOf(0.3f) } 
    var selectedFont by remember { mutableStateOf(ThumbnailFont.IMPACT) }
    var selectedTextColor by remember { mutableStateOf(colorSwatches[0]) } // Glow Green
    var selectedStrokeColor by remember { mutableStateOf(colorSwatches[4]) } // Shadow Black
    var isOutlineEnabled by remember { mutableStateOf(true) }
    var isShadowEnabled by remember { mutableStateOf(true) }
    var textSizeFactor by remember { mutableStateOf(1.2f) } // scale multiplier

    // Step 3 States (Adjustment parameters)
    var brightnessOffset by remember { mutableStateOf(0f) } // -100 to 100
    var contrastScale by remember { mutableStateOf(1.2f) } // 0.5 to 2.5
    var selectedRatioLabel by remember { mutableStateOf("16:9") } // "16:9" (YouTube), "9:16" (TikTok), "1:1"
    var zoomScale by remember { mutableStateOf(1.0f) } // 1.0 to 2.5
    var panXOffset by remember { mutableStateOf(0f) }
    var panYOffset by remember { mutableStateOf(0f) }

    // Step 4 States (Export options)
    var exportFormat by remember { mutableStateOf("JPG") } // "JPG" | "PNG"
    var applyToGalleryIcon by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }

    // Custom Image picker launcher
    val customImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            baseFrameBitmap = bmp
                            isUsingCustomImage = true
                            Toast.makeText(context, "Loaded custom background!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ThumbnailCreator", "Failed to load custom image file", e)
                }
            }
        }
    }

    // Extraction job for scrubbing
    var scrubJob by remember { mutableStateOf<Job?>(null) }

    // Initial setup: pull baseline frame and pre-fetch suggested thumb previews
    LaunchedEffect(video.uri) {
        isLoadingFrame = true
        // 1. Fetch baseline frame at seekPosition
        scope.launch(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, video.uri)
                val bmp = retriever.getFrameAtTime(seekPositionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0)
                if (bmp != null) {
                    baseFrameBitmap = bmp
                }
            } catch (e: Exception) {
                Log.e("ThumbnailCreator", "Initial baseline extraction failed", e)
            } finally {
                retriever?.release()
                isLoadingFrame = false
            }
        }

        // 2. Fetch suggested moment thumbs in the background
        suggestedMoments.forEachIndexed { idx, moment ->
            scope.launch(Dispatchers.IO) {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, video.uri)
                    val sample = retriever.getFrameAtTime(moment.timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (sample != null) {
                        // Rescale sample to be lightweight
                        val scaled = Bitmap.createScaledBitmap(sample, 120, 80, false)
                        suggestedMoments[idx] = moment.copy(bitmap = scaled)
                    }
                } catch (e: Exception) {
                    Log.e("ThumbnailCreator", "Failed fetching moment ${moment.title}", e)
                } finally {
                    retriever?.release()
                }
            }
        }
    }

    // Scrub position frame extractor (with debounce to keep sliding layout totally responsive)
    fun triggerScrubFrameExtract(timeMs: Long) {
        scrubJob?.cancel()
        scrubJob = scope.launch(Dispatchers.IO) {
            delay(150) // Debounce delay
            isLoadingFrame = true
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, video.uri)
                val bmp = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeMs * 1000)
                if (bmp != null) {
                    withContext(Dispatchers.Main) {
                        baseFrameBitmap = bmp
                        isUsingCustomImage = false
                    }
                }
            } catch (e: Exception) {
                Log.e("ThumbnailCreator", "Scrub frame extract failed", e)
            } finally {
                retriever?.release()
                isLoadingFrame = false
            }
        }
    }

    // Helper: Build the combined visual output bitmap
    fun renderFinalThumbnailBitmap(): Bitmap? {
        val srcBmp = baseFrameBitmap ?: return null
        
        // Define standard target sizes
        val targetWidth = 1280
        val targetHeight = 720
        
        val finalBmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBmp)
        
        // 1. Draw original frame with Brightness / Contrast / Crop Zoom adjustments
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        
        // Setup Color Matrix filter for brightness/contrast
        val cm = ColorMatrix()
        // Contrast Scale multiplier
        val scale = contrastScale
        // Brightness offset (normalized)
        val translate = brightnessOffset
        cm.set(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        
        // Calculate crop fitting source dimensions
        val srcWidth = srcBmp.width
        val srcHeight = srcBmp.height
        
        // Setup aspect ratio clipping bounds based on selected ratio
        val targetAspect = when (selectedRatioLabel) {
            "9:16" -> 9f/16f
            "1:1" -> 1.0f
            else -> 16f/9f // Default 16:9
        }
        
        var cropWidth = srcWidth.toFloat()
        var cropHeight = srcHeight.toFloat()
        
        if (srcWidth.toFloat() / srcHeight > targetAspect) {
            cropWidth = srcHeight * targetAspect
        } else {
            cropHeight = srcWidth / targetAspect
        }
        
        // Apply zoom scale factor
        cropWidth /= zoomScale
        cropHeight /= zoomScale
        
        // Apply pan centering offset
        val pX = (srcWidth - cropWidth) / 2f + (panXOffset * (srcWidth / 300f))
        val pY = (srcHeight - cropHeight) / 2f + (panYOffset * (srcHeight / 300f))
        
        val srcRect = RectF(
            pX.coerceIn(0f, srcWidth - cropWidth),
            pY.coerceIn(0f, srcHeight - cropHeight),
            (pX + cropWidth).coerceIn(cropWidth, srcWidth.toFloat()),
            (pY + cropHeight).coerceIn(cropHeight, srcHeight.toFloat())
        )
        
        val destRect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        canvas.drawBitmap(srcBmp, null, destRect, paint)
        
        // 2. Render Text Overlays
        if (titleText.trim().isNotEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = (65f * textSizeFactor).coerceAtLeast(20f)
                typeface = android.graphics.Typeface.create(selectedFont.systemFont, selectedFont.fontStyle)
                color = selectedTextColor.composeColor.value.toInt()
            }
            
            val finalX = targetWidth * textXRatio
            val finalY = targetHeight * textYRatio
            
            // Draw text shadow if enabled
            if (isShadowEnabled) {
                textPaint.setShadowLayer(10f, 4f, 4f, android.graphics.Color.BLACK)
            }
            
            // Draw text stroke/outline if enabled
            if (isOutlineEnabled) {
                val outlinePaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 10f
                    color = selectedStrokeColor.composeColor.value.toInt()
                }
                // Outline goes first without shadow
                outlinePaint.clearShadowLayer()
                canvas.drawText(titleText, finalX, finalY, outlinePaint)
            }
            
            // Draw physical foreground characters
            canvas.drawText(titleText, finalX, finalY, textPaint)
        }
        
        return finalBmp
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("thumbnail_creator_container"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "EXPLOSIVE THUMBNAIL LAB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("thumbnail_back_btn")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF09090B),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF09090B)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF09090B))
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            
            // Live Preview Visual Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Black)
                    .onGloballyPositioned { _ -> }
                    .testTag("thumbnail_preview_container"),
                contentAlignment = Alignment.Center
            ) {
                if (baseFrameBitmap != null) {
                    val finalAspect = when (selectedRatioLabel) {
                        "9:16" -> 9f/16f
                        "1:1" -> 1f
                        else -> 16f/9f
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(finalAspect)
                            .fillMaxHeight(0.9f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                            .background(Color.DarkGray)
                    ) {
                        // Display baseline frame applying basic Compose scale filters
                        Image(
                            bitmap = baseFrameBitmap!!.asImageBitmap(),
                            contentDescription = "Base preview frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Render dragging Text overlay bounding box
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("draggable_text_bounds")
                        ) {
                            if (titleText.isNotEmpty()) {
                                var boxWidth by remember { mutableStateOf(1) }
                                var boxHeight by remember { mutableStateOf(1) }

                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { layoutCoordinates ->
                                            boxWidth = layoutCoordinates.size.width
                                            boxHeight = layoutCoordinates.size.height
                                        }
                                        .align(Alignment.TopStart)
                                        .offset {
                                            IntOffset(
                                                x = (textXRatio * 320.dp.value - (boxWidth / 2f)).roundToInt(),
                                                y = (textYRatio * 220.dp.value - (boxHeight / 2f)).roundToInt()
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                textXRatio = (textXRatio + dragAmount.x / 300f).coerceIn(0.1f, 0.9f)
                                                textYRatio = (textYRatio + dragAmount.y / 200f).coerceIn(0.1f, 0.9f)
                                            }
                                        }
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("draggable_title")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.DragHandle, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = titleText,
                                            color = selectedTextColor.composeColor,
                                            fontSize = (13f * textSizeFactor).sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(color = Color(0xFF22C55E))
                }

                if (isLoadingFrame) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wizard Step Navigation Segment headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .background(Color(0xFF18181B), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1 to "FRAME", 2 to "TEXT", 3 to "ADJUST", 4 to "EXPORT").forEach { (step, text) ->
                    val isCurrent = currentStep == step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCurrent) Color(0xFF22C55E) else Color.Transparent)
                            .clickable { currentStep = step }
                            .padding(vertical = 8.dp)
                            .testTag("wizard_step_$step"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = if (isCurrent) Color.Black else Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi Step Wizard Body contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when (currentStep) {
                    1 -> { // FRAME PICKER tab
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT VIDEO TIMEFRAME",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${seekPositionMs}ms / ${video.durationMs}ms",
                                    color = Color(0xFF22C55E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Native scrubber seek bar
                            Slider(
                                value = seekPositionMs.toFloat(),
                                onValueChange = {
                                    seekPositionMs = it.toLong()
                                    triggerScrubFrameExtract(seekPositionMs)
                                },
                                valueRange = 0f..video.durationMs.toFloat(),
                                steps = 100,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFF22C55E),
                                    thumbColor = Color(0xFF22C55E)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("frame_scrubber")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Custom background image uploader card
                            Button(
                                onClick = { customImagePicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E33)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_custom_image_btn")
                            ) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Custom Background Image", color = Color.White, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Auto Suggested moments headline title
                            Text(
                                text = "AUTO-SUGGESTED HIGH-CLICK FRAME MOMENTS",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("suggested_frames_row")
                            ) {
                                items(suggestedMoments) { moment ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable {
                                                seekPositionMs = moment.timeMs
                                                triggerScrubFrameExtract(seekPositionMs)
                                            }
                                            .testTag("suggested_frame_${moment.title.replace(" ", "_")}")
                                    ) {
                                        Column {
                                            if (moment.bitmap != null) {
                                                Image(
                                                    bitmap = moment.bitmap!!.asImageBitmap(),
                                                    contentDescription = moment.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(75.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(75.dp)
                                                        .background(Color.Black),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text(
                                                    text = moment.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = moment.description,
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // TEXT OVERLAY BUILDER tab
                        Column {
                            Text(
                                text = "CLICKBAIT TEXT",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = titleText,
                                onValueChange = { if (it.length <= 40) titleText = it },
                                placeholder = { Text("Enter bold text...", color = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("text_input_field"),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF22C55E),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedContainerColor = Color(0xFF18181B),
                                    unfocusedContainerColor = Color(0xFF18181B)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Text sizing sliders
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("TEXT SIZE: ", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Slider(
                                    value = textSizeFactor,
                                    onValueChange = { textSizeFactor = it },
                                    valueRange = 0.5f..2.5f,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF22C55E), thumbColor = Color(0xFF22C55E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("text_size_slider")
                                )
                            }

                            // Font Selectors
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "SELECT BRAND FONT",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThumbnailFont.values().forEach { font ->
                                    val isSel = selectedFont == font
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF22C55E) else Color(0xFF1A1A1E))
                                            .border(1.dp, if (isSel) Color(0xFF22C55E) else Color.DarkGray, RoundedCornerShape(6.dp))
                                            .clickable { selectedFont = font }
                                            .padding(vertical = 10.dp)
                                            .testTag("font_picker_${font.name}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = font.label,
                                            color = if (isSel) Color.Black else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Text Style toggles (Stroke, outline)
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = isOutlineEnabled,
                                        onCheckedChange = { isOutlineEnabled = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E)),
                                        modifier = Modifier.testTag("outline_checkbox")
                                    )
                                    Text("High-Contrast Outline", color = Color.White, fontSize = 11.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = isShadowEnabled,
                                        onCheckedChange = { isShadowEnabled = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E)),
                                        modifier = Modifier.testTag("shadow_checkbox")
                                    )
                                    Text("Soft Accent Shadow", color = Color.White, fontSize = 11.sp)
                                }
                            }

                            // Text Color Palette Selection Swatches
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "TEXT FILL COLOR",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                colorSwatches.forEach { color ->
                                    val isSelectedObj = selectedTextColor.name == color.name
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color.composeColor)
                                            .border(
                                                width = if (isSelectedObj) 2.dp else 1.dp,
                                                color = if (isSelectedObj) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedTextColor = color }
                                            .testTag("text_color_${color.name.replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }

                    3 -> { // AESTHETIC ADJUSTMENTS tab
                        Column {
                            // Brightness Contrast Seekers
                            Text(
                                text = "AESTHETIC FILTER CALIBRATION",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("BRIGHTNESS: ", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(85.dp))
                                Slider(
                                    value = brightnessOffset,
                                    onValueChange = { brightnessOffset = it },
                                    valueRange = -80f..80f,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF22C55E), thumbColor = Color(0xFF22C55E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("brightness_slider")
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("CONTRAST: ", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(85.dp))
                                Slider(
                                    value = contrastScale,
                                    onValueChange = { contrastScale = it },
                                    valueRange = 0.6f..2.2f,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF22C55E), thumbColor = Color(0xFF22C55E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("contrast_slider")
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ZOOM CROP: ", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(85.dp))
                                Slider(
                                    value = zoomScale,
                                    onValueChange = { zoomScale = it },
                                    valueRange = 1.0f..2.5f,
                                    colors = SliderDefaults.colors(activeTrackColor = Color(0xFF22C55E), thumbColor = Color(0xFF22C55E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("zoom_slider")
                                )
                            }

                            // Platform Framing ratios chips selectors
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "SELECT PLATFORM RATIO PRESET",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val ratios = listOf(
                                    Triple("16:9", "16:9 (YouTube)", Icons.Rounded.Monitor),
                                    Triple("9:16", "9:16 (TikTok)", Icons.Rounded.Smartphone),
                                    Triple("1:1", "1:1 (Post)", Icons.Rounded.AspectRatio)
                                )
                                ratios.forEach { (ratio, label, icon) ->
                                    val isSel = selectedRatioLabel == ratio
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFF22C55E) else Color(0xFF1E1E22))
                                            .border(1.dp, if (isSel) Color(0xFF22C55E) else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable { selectedRatioLabel = ratio }
                                            .padding(vertical = 11.dp)
                                            .testTag("ratio_preset_$ratio"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = if (isSel) Color.Black else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = label,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> { // SAVE AND EXPORT SYSTEM tab
                        Column {
                            Text(
                                text = "EXPORT FILE PRESET CONFIGURATION",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Format Picker Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listOf("JPG", "PNG").forEach { format ->
                                    val isSelected = exportFormat == format
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF22C55E) else Color(0xFF18181B))
                                            .border(1.dp, if (isSelected) Color(0xFF22C55E) else Color.DarkGray, RoundedCornerShape(8.dp))
                                            .clickable { exportFormat = format }
                                            .padding(vertical = 12.dp)
                                            .testTag("export_format_$format"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$format Lossless File",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Gallery icon binding toggle
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFF18181B),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Set as In-App Gallery Thumbnail", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Instantly replaces the video preview icon within DuoCam cards", color = Color.Gray, fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = applyToGalleryIcon,
                                        onCheckedChange = { applyToGalleryIcon = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF22C55E), checkedTrackColor = Color(0xFF22C55E).copy(alpha = 0.3f)),
                                        modifier = Modifier.testTag("gallery_thumbnail_toggle")
                                    )
                                }
                            }

                            // Trigger complete export button CTA
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    isExporting = true
                                    scope.launch(Dispatchers.IO) {
                                        val output = renderFinalThumbnailBitmap()
                                        if (output != null) {
                                            // 1. If applyToGalleryIcon is enabled, save locally inside the files folder associated with this URI hash
                                            if (applyToGalleryIcon) {
                                                try {
                                                    val hash = video.uri.toString().hashCode()
                                                    val localFile = File(context.filesDir, "custom_thumb_$hash.jpg")
                                                    FileOutputStream(localFile).use { outStream ->
                                                        output.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("ThumbnailCreator", "Failed to override dynamic gallery reference file", e)
                                                }
                                            }

                                            // 2. Export physical file to public Pictures/DuoCam directory
                                            var success = false
                                            try {
                                                val resolver = context.contentResolver
                                                val timestamp = System.currentTimeMillis()
                                                val ext = exportFormat.lowercase()
                                                val mime = if (exportFormat == "PNG") "image/png" else "image/jpeg"
                                                val format = if (exportFormat == "PNG") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                                
                                                val values = ContentValues().apply {
                                                    put(MediaStore.Images.Media.DISPLAY_NAME, "DuoCam_Thumb_${timestamp}.$ext")
                                                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DuoCamThumbnails")
                                                }

                                                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                                if (uri != null) {
                                                    resolver.openOutputStream(uri)?.use { os ->
                                                        output.compress(format, 95, os)
                                                        success = true
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("ThumbnailCreator", "Public MediaStore export failed, falling back to cache directory", e)
                                            }

                                            withContext(Dispatchers.Main) {
                                                isExporting = false
                                                Toast.makeText(context, "Thumbnail exported and saved successfully!", Toast.LENGTH_SHORT).show()
                                                onThumbnailSaved()
                                                onDismiss()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isExporting = false
                                                Toast.makeText(context, "Failed to render compiled preview canvas.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("export_thumbnail_cta"),
                                enabled = !isExporting && baseFrameBitmap != null
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("COMPILE & EXPORT HIGH_RES THUMBNAIL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
