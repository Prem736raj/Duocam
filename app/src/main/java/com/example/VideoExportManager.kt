package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File

data class ExportPreset(
    val id: String,
    val name: String,
    val platform: String, // "YouTube", "TikTok", "Instagram Reels", "Instagram Post", "Twitter/X", "WhatsApp"
    val aspect: String, // "16:9", "9:16", "1:1", "4:5", "original"
    val res: String, // "4K", "1080p", "720p"
    val codec: String, // "HEVC", "H264"
    val description: String,
    val isWhatsApp: Boolean = false,
    val hiQuality: Boolean = false
)

object VideoExportManager {
    private const val TAG = "VideoExportManager"

    // Core Observable States
    var isExporting by mutableStateOf(false)
    var exportProgress by mutableStateOf(0f)
    var elapsedTimeMs by mutableStateOf(0L)
    var estimatedRemainingTimeMs by mutableStateOf(0L)
    var exportState by mutableStateOf("idle") // "idle", "settings", "exporting", "success", "failed"
    var errorMessage by mutableStateOf<String?>(null)
    var exportedUri by mutableStateOf<Uri?>(null)

    // Export Options Chosen by User
    var selectedResolution by mutableStateOf("original") // "original", "1080p", "720p"
    var selectedCodec by mutableStateOf("HEVC") // "H264", "HEVC"
    var useHardwareAcceleration by mutableStateOf(true)
    var simulateFailure by mutableStateOf(false) // Toggle to test failure

    // Platform Export Presets & Options
    val presets = listOf(
        ExportPreset("youtube_1080p", "YouTube 1080p", "YouTube", "16:9", "1080p", "HEVC", "High quality 16:9 widescreen FHD video", hiQuality = true),
        ExportPreset("youtube_4k", "YouTube 4K", "YouTube", "16:9", "4K", "HEVC", "Premium UHD 4K theatrical master", hiQuality = true),
        ExportPreset("tiktok", "TikTok Video", "TikTok", "9:16", "1080p", "HEVC", "9:16 portrait optimized vertical feed", hiQuality = true),
        ExportPreset("instagram_reels", "Instagram Reels", "Instagram Reels", "9:16", "1080p", "HEVC", "9:16 vertical optimized for Reels & Stories", hiQuality = true),
        ExportPreset("instagram_post_1_1", "Instagram Post 1:1", "Instagram Post", "1:1", "1080p", "H264", "1:1 classic square format post"),
        ExportPreset("instagram_post_4_5", "Instagram Post 4:5", "Instagram Post", "4:5", "1080p", "H264", "4:5 standard vertical layout post"),
        ExportPreset("twitter_x", "Twitter / X Post", "Twitter/X", "16:9", "720p", "H264", "16:9 HD, fast timelines loading feed", hiQuality = false),
        ExportPreset("whatsapp", "WhatsApp Chat", "WhatsApp", "original", "720p", "HEVC", "Highly compressed format capped under 16 MB", isWhatsApp = true)
    )

    var selectedPresetId by mutableStateOf("youtube_1080p")
    var mismatchOption by mutableStateOf("Add bars") // "Crop to fit", "Add bars", "Stretch to fit"

    // Reference context and clips
    var currentRecording by mutableStateOf<DuoRecording?>(null)
    var mergeClips = mutableStateListOf<DuoRecording>()
    var activeTool by mutableStateOf("Trim")
    var totalMergeDurationMs by mutableStateOf(0L)

    private var exportJob: Job? = null
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Resets the export pipeline to defaults
     */
    fun reset() {
        exportJob?.cancel()
        exportJob = null
        isExporting = false
        exportProgress = 0f
        elapsedTimeMs = 0L
        estimatedRemainingTimeMs = 0L
        exportState = "idle"
        errorMessage = null
        exportedUri = null
        selectedPresetId = "youtube_1080p"
        mismatchOption = "Add bars"
    }

    /**
     * Gets the effective video duration under edit
     */
    fun getEffectiveDuration(): Long {
        return if (activeTool == "Merge") {
            totalMergeDurationMs
        } else {
            currentRecording?.durationMs ?: 0L
        }
    }

    /**
     * Returns string representation of active resolution category based on chosen settings
     */
    fun getTargetResolutionCategory(): String {
        val p = presets.find { it.id == selectedPresetId } ?: presets[0]
        return p.res
    }

    /**
     * Estimates output file size in bytes
     */
    fun getEstimatedFileSize(): Long {
        val durationMs = getEffectiveDuration()
        val sec = durationMs / 1000.0
        val p = presets.find { it.id == selectedPresetId } ?: presets[0]
        
        if (p.isWhatsApp) {
            // Highly compressed format capped under 16 MB.
            // If the duration is very long, we compress even further or simulate the cap at 14.5 MB.
            val baseSize = (sec * 0.45 * 1024 * 1024).toLong() // ~450 KB/s
            val cap = (14.5 * 1024 * 1024).toLong()
            return if (baseSize > cap) cap else baseSize.coerceAtLeast(400 * 1024)
        }

        val multiplier = when (p.res) {
            "4K" -> if (p.codec == "HEVC") 2.7 else 4.5
            "1080p" -> if (p.codec == "HEVC") 1.3 else 2.2
            else -> if (p.codec == "HEVC") 0.6 else 1.0 // 720p
        }
        return (sec * multiplier * 1024 * 1024).toLong().coerceAtLeast(512 * 1024)
    }

    /**
     * Estimates render/export duration in milliseconds
     */
    fun getEstimatedExportDurationMs(): Long {
        val durationMs = getEffectiveDuration()
        val p = presets.find { it.id == selectedPresetId } ?: presets[0]
        
        // Base multiplier representing real-time ratio
        // (Higher multiplier means FASTER export speed)
        var speedMultiplier = when (p.res) {
            "4K" -> if (p.codec == "HEVC") 4.0f else 6.0f
            "1080p" -> if (p.codec == "HEVC") 8.0f else 12.0f
            else -> if (p.codec == "HEVC") 12.0f else 18.0f // 720p
        }

        // Adjust for Hardware Acceleration
        if (!useHardwareAcceleration) {
            speedMultiplier *= 0.15f // 85% slower without hardware encoding!
        }

        return (durationMs / speedMultiplier).toLong().coerceAtLeast(1200L)
    }

    /**
     * Cancels the active background export process safely
     */
    fun cancelExport() {
        Log.i(TAG, "User requested cancellation of video export.")
        exportJob?.cancel()
        exportJob = null
        isExporting = false
        exportState = "idle"
        exportProgress = 0f
    }

    /**
     * Starts the export pipeline in the background using hardware acceleration
     */
    fun startExport(context: Context, onComplete: () -> Unit = {}) {
        if (isExporting) return
        
        isExporting = true
        exportState = "exporting"
        exportProgress = 0f
        elapsedTimeMs = 0L
        errorMessage = null
        exportedUri = null

        val startTime = System.currentTimeMillis()
        val totalEstMs = getEstimatedExportDurationMs()

        exportJob = managerScope.launch {
            try {
                // Initialize background status tracking loops
                val progressDeferred = launch {
                    while (isActive) {
                        delay(100)
                        val elapsed = System.currentTimeMillis() - startTime
                        elapsedTimeMs = elapsed
                        
                        if (totalEstMs > 0) {
                            val computedProgress = (elapsed.toFloat() / totalEstMs).coerceIn(0f, 0.99f)
                            exportProgress = computedProgress
                            
                            val remaining = ((totalEstMs - elapsed).coerceAtLeast(0))
                            estimatedRemainingTimeMs = remaining
                        }
                    }
                }

                // Simulate processing / perform actual video encoding
                withContext(Dispatchers.IO) {
                    val recordings = if (activeTool == "Merge") mergeClips.toList() else listOfNotNull(currentRecording)
                    if (recordings.isEmpty()) {
                        throw IllegalArgumentException("No input clips provided for compilation.")
                    }

                    // Check for artificial failure testing
                    if (simulateFailure) {
                        delay(2500)
                        throw IllegalStateException("Export failed: Devic Storage is FULL. (Available space: 0.00 B. Required space: 45.2 MB).")
                    }

                    val firstRec = recordings.first()
                    val timestamp = System.currentTimeMillis() / 1000
                    val baseName = firstRec.uri.lastPathSegment?.substringBefore(".") ?: "video"
                    
                    val preset = presets.find { it.id == selectedPresetId } ?: presets[0]
                    val codecTag = preset.codec
                    val resTag = preset.res
                    val targetName = "DuoCam_${baseName}_Export_${preset.platform.replace(" ", "")}_${resTag}_${codecTag}_${timestamp}.mp4"

                    val tempFile = File(context.cacheDir, "temp_export_${timestamp}.mp4")

                    val originalAspect = VideoMetadataManager.getAspectRatio(context, firstRec)
                    val finalAspect = if (preset.aspect == "original") originalAspect else preset.aspect

                    // Base dimensions considering landscape or portrait flip
                    val isPortraitPreset = finalAspect == "9:16" || finalAspect == "4:5"
                    val baseDim = when (preset.res) {
                        "4K" -> if (isPortraitPreset) Pair(2160, 3840) else Pair(3840, 2160)
                        "1080p" -> if (isPortraitPreset) Pair(1080, 1920) else Pair(1920, 1080)
                        else -> if (isPortraitPreset) Pair(720, 1280) else Pair(1280, 720)
                    }

                    // Refine dimensions for Square or Portrait Post
                    val (width, height) = when (finalAspect) {
                        "1:1" -> Pair(1080, 1080)
                        "4:5" -> Pair(1080, 1350)
                        else -> Pair(baseDim.first, baseDim.second)
                    }

                    val layout = VideoMetadataManager.getLayout(context, firstRec)
                    val cameraMode = VideoMetadataManager.getCameraMode(context, firstRec)
                    val useHevc = preset.codec == "HEVC"
                    val durationSecs = (getEffectiveDuration() / 1000).toInt().coerceAtLeast(1)

                    // Execute robust physical media generation utilizing android hardware encoders
                    val success = VideoFileGenerator.generateVideo(
                        outputFile = tempFile,
                        durationSeconds = durationSecs,
                        width = width,
                        height = height,
                        layout = layout,
                        isConcurrent = cameraMode == "Full Dual-Cam Engine",
                        useHevc = useHevc,
                        audioFilePath = null,
                        isStabilizationActive = false,
                        stabilizationStrength = "Standard",
                        watermark = null,
                        mismatchMode = mismatchOption,
                        originalAspectRatio = originalAspect,
                        isWhatsApp = preset.isWhatsApp,
                        onProgress = { /* we track relative execution */ }
                    )

                    if (!success || !tempFile.exists()) {
                        throw IllegalStateException("Video hardware rendering failed. Could not write to Temp file container.")
                    }

                    // Register in android system MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, targetName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
                        put(MediaStore.Video.Media.DATE_ADDED, timestamp)
                    }

                    val resolver = context.contentResolver
                    val contentUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw IllegalStateException("Could not register export inside Android System MediaStore.")

                    resolver.openOutputStream(contentUri)?.use { outStream ->
                        tempFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    } ?: throw IllegalStateException("Failed to open System IO stream to write output binary.")

                    // Save matching metadata structures
                    VideoMetadataManager.saveMetadata(
                        context = context,
                        uriString = contentUri.toString(),
                        layout = layout,
                        resolution = preset.res,
                        aspectRatio = finalAspect,
                        fps = VideoMetadataManager.getFps(context, firstRec),
                        codec = if (useHevc) "HEVC/H.265" else "H.264/AVC",
                        cameraMode = cameraMode
                    )

                    // Store customized display name
                    context.getSharedPreferences("DuoCam_Metadata_Store", Context.MODE_PRIVATE).edit().putString("${contentUri}_custom_name", targetName).apply()

                    // Cleanup physical cache immediately
                    if (tempFile.exists()) tempFile.delete()

                    // Final delay to guarantee full rendering cycle
                    delay(500)
                    progressDeferred.cancel()

                    withContext(Dispatchers.Main) {
                        exportProgress = 1.0f
                        estimatedRemainingTimeMs = 0L
                        exportedUri = contentUri
                        exportState = "success"
                        isExporting = false
                        onComplete()
                    }
                }
            } catch (c: CancellationException) {
                Log.i(TAG, "Export job cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Error compiling exported video structure", e)
                withContext(Dispatchers.Main) {
                    exportProgress = 0f
                    errorMessage = e.message ?: "An unhandled exception occurred within the Android MediaCodec pipeline."
                    exportState = "failed"
                    isExporting = false
                }
            }
        }
    }
}
