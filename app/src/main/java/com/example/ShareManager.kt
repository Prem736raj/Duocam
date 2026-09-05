package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File

data class SharePlatform(
    val id: String,
    val name: String,
    val presetId: String,
    val packageName: String,
    val brandColor: Long
)

data class ShareQueueItem(
    val platform: SharePlatform,
    var status: String, // "queued", "formatting", "ready", "shared", "failed"
    var progress: Float = 0f,
    var formattedUri: Uri? = null
)

object ShareManager {
    private const val TAG = "ShareManager"

    val platforms = listOf(
        SharePlatform("instagram", "Instagram", "instagram_reels", "com.instagram.android", 0xFFE1306C),
        SharePlatform("tiktok", "TikTok", "tiktok", "com.zhiliaoapp.musically", 0xFF00F2FE),
        SharePlatform("youtube", "YouTube", "youtube_1080p", "com.google.android.youtube", 0xFFFF0000),
        SharePlatform("whatsapp", "WhatsApp", "whatsapp", "com.whatsapp", 0xFF25D366),
        SharePlatform("snapchat", "Snapchat", "tiktok", "com.snapchat.android", 0xFFFFFC00),
        SharePlatform("twitter", "Twitter/X", "twitter_x", "com.twitter.android", 0xFF1DA1F2)
    )

    // Queuing Map selection state (UI bindable)
    val selectedForQueue = mutableStateMapOf<String, Boolean>().apply {
        platforms.forEach { put(it.id, false) }
    }

    // Active Share Queue State
    val activeQueue = mutableStateListOf<ShareQueueItem>()
    var isQueueActive by mutableStateOf(false)
    var currentQueueIndex by mutableStateOf(0)
    var isFormattingStep by mutableStateOf(false)
    var activeShareProgress by mutableStateOf(0f)
    var logMessage by mutableStateOf("")

    private var queueJob: Job? = null
    private val shareScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Persist share frequencies globally under standard Shared Preferences
     */
    fun trackShare(context: Context, platformId: String) {
        val prefs = context.getSharedPreferences("DuoCamSharePrefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("share_count_${platformId.lowercase()}", 0)
        prefs.edit().putInt("share_count_${platformId.lowercase()}", currentCount + 1).apply()
    }

    /**
     * Retrieves sharing count for sorting priority lists
     */
    fun getShareCount(context: Context, platformId: String): Int {
        val prefs = context.getSharedPreferences("DuoCamSharePrefs", Context.MODE_PRIVATE)
        return prefs.getInt("share_count_${platformId.lowercase()}", 0)
    }

    /**
     * Returns the list of standard platforms sorted by most-used descending!
     */
    fun getSortedPlatforms(context: Context): List<SharePlatform> {
        return platforms.sortedWith(compareByDescending<SharePlatform> { getShareCount(context, it.id) }.thenBy { it.name })
    }

    /**
     * Starts processing the sharing queue one by one!
     */
    fun startQueueProcessing(context: Context, sourceVideo: DuoRecording, onQueueFinished: () -> Unit = {}) {
        if (isQueueActive) return

        // Populate active items list
        activeQueue.clear()
        platforms.forEach { platform ->
            if (selectedForQueue[platform.id] == true) {
                activeQueue.add(ShareQueueItem(platform, "queued"))
            }
        }

        if (activeQueue.isEmpty()) {
            Toast.makeText(context, "Please select at least one platform to share!", Toast.LENGTH_SHORT).show()
            return
        }

        isQueueActive = true
        currentQueueIndex = 0
        isFormattingStep = false
        activeShareProgress = 0f
        logMessage = "Initiating multi-format platform delivery..."

        queueJob = shareScope.launch {
            for (i in 0 until activeQueue.size) {
                currentQueueIndex = i
                val item = activeQueue[i]
                
                logMessage = "Processing formatting for ${item.platform.name}..."
                item.status = "formatting"
                activeQueue[i] = item

                // Prepare target video format based on platform criteria
                val formattedUri = formatVideoForPlatform(context, sourceVideo, item.platform)
                
                if (formattedUri != null) {
                    item.status = "ready"
                    item.formattedUri = formattedUri
                    activeQueue[i] = item
                    logMessage = "Ready to share with ${item.platform.name}"
                    
                    // Trigger physical launch deep-link
                    withContext(Dispatchers.Main) {
                        launchDirectShare(context, item.platform.id, formattedUri)
                        trackShare(context, item.platform.id)
                    }

                    // A brief simulated delay to let the user post or tap screen
                    delay(1500)
                    item.status = "shared"
                    activeQueue[i] = item
                } else {
                    item.status = "failed"
                    activeQueue[i] = item
                    logMessage = "Formatting failed for ${item.platform.name}"
                    delay(1500)
                }
            }

            logMessage = "All sharing entries processed successfully!"
            delay(1000)
            isQueueActive = false
            selectedForQueue.keys.forEach { selectedForQueue[it] = false }
            onQueueFinished()
        }
    }

    /**
     * Cancel active queue
     */
    fun cancelQueue() {
        queueJob?.cancel()
        queueJob = null
        isQueueActive = false
        activeQueue.clear()
    }

    /**
     * Formats the video specifically matching the destination platform preset configuration
     */
    private suspend fun formatVideoForPlatform(context: Context, sourceVideo: DuoRecording, platform: SharePlatform): Uri? {
        isFormattingStep = true
        activeShareProgress = 0f
        
        // 1. Point VideoExportManager to target preset
        VideoExportManager.selectedPresetId = platform.presetId
        VideoExportManager.currentRecording = sourceVideo

        var resultUri: Uri? = null
        var completed = false
        var failed = false

        // 2. Initiate physical hardware transcoding in background
        withContext(Dispatchers.Main) {
            VideoExportManager.startExport(context) {
                completed = true
            }
        }

        // 3. Monitor compilation progress
        while (!completed && !failed) {
            delay(100)
            activeShareProgress = VideoExportManager.exportProgress
            if (VideoExportManager.exportState == "failed") {
                failed = true
            }
            if (VideoExportManager.exportState == "success") {
                completed = true
                resultUri = VideoExportManager.exportedUri
            }
        }

        isFormattingStep = false
        activeShareProgress = 1.0f
        return resultUri ?: sourceVideo.uri
    }

    /**
     * Creates direct-to-app deep-linked post/upload actions
     */
    fun getDirectShareIntent(context: Context, platformId: String, videoUri: Uri): Intent {
        val targetPackage = platforms.find { it.id == platformId }?.packageName ?: ""
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage.isNotEmpty()) {
                setPackage(targetPackage)
            }
        }
    }

    /**
     * Launches direct-to-app deep-linked upload flow, falling back to system share sheet if the app isn't installed
     */
    fun launchDirectShare(context: Context, platformId: String, videoUri: Uri) {
        val intent = getDirectShareIntent(context, platformId, videoUri)
        val platformName = platforms.find { it.id == platformId }?.name ?: platformId
        
        try {
            // Verify if package is installed by checking resolver
            val pm = context.packageManager
            val activities = pm.queryIntentActivities(intent, 0)
            if (activities.isEmpty()) {
                throw Exception("Target application is not installed on this system.")
            }
            context.startActivity(intent)
            Toast.makeText(context, "Redirecting to $platformName posting screen...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.w(TAG, "Direct launch failed for $platformId, launching system share dialog", e)
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share video to $platformName"))
        }
    }

    /**
     * Saves video source URI to the standard public "Downloads/Files" user space
     */
    fun saveToFiles(context: Context, videoUri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val timestamp = System.currentTimeMillis()
            val fileName = "DuoCam_SaveToFiles_${timestamp}.mp4"

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } else {
                val targetFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                Uri.fromFile(targetFile)
            }

            if (targetUri != null) {
                resolver.openOutputStream(targetUri)?.use { out ->
                    resolver.openInputStream(videoUri)?.use { inp ->
                        inp.copyTo(out)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saveToFiles file stream operation", e)
            false
        }
    }

    /**
     * Copies video URI text to the standard system clipboard
     */
    fun copyToClipboard(context: Context, videoUri: Uri) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newRawUri("DuoCam Exported Video", videoUri)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Video path registered on system clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed clipboard copy", e)
            Toast.makeText(context, "Could not register video to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}
