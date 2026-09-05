package com.example

import android.content.Context
import android.net.Uri
import android.util.Log
import android.os.StatFs
import java.io.InputStream
import java.io.OutputStream

data class ResolutionGroup(
    val label: String,
    val sizeBytes: Long,
    val count: Int
)

data class FolderGroup(
    val label: String,
    val sizeBytes: Long,
    val count: Int
)

data class StorageStats(
    val totalSpaceBytes: Long,
    val freeSpaceBytes: Long,
    val duoCamSpaceBytes: Long,
    val resBreakdown: List<ResolutionGroup>,
    val folderBreakdown: List<FolderGroup>,
    val largestVideos: List<DuoRecording>
)

data class CleanupSuggestions(
    val oldVideos: List<DuoRecording>,
    val oldVideosTotalSize: Long,
    val duplicateVideos: List<DuoRecording>,
    val duplicateVideosTotalSize: Long
)

object VideoMetadataManager {
    private const val PREFS_NAME = "DuoCamVideoPrefs"
    
    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveMetadata(
        context: Context,
        uriString: String,
        layout: String,
        resolution: String,
        aspectRatio: String = "16:9",
        fps: Int = 30,
        codec: String = "H.264/AVC",
        cameraMode: String = "Full Dual-Cam Engine",
        location: String = "Not Available"
    ) {
        getPrefs(context).edit().apply {
            putString("${uriString}_layout", layout)
            putString("${uriString}_resolution", resolution)
            putString("${uriString}_aspectRatio", aspectRatio)
            putInt("${uriString}_fps", fps)
            putString("${uriString}_codec", codec)
            putString("${uriString}_cameraMode", cameraMode)
            putString("${uriString}_location", location)
            apply()
        }
    }

    // Custom Display Name override
    fun getDisplayName(context: Context, recording: DuoRecording): String {
        val overriden = getPrefs(context).getString("${recording.uri}_custom_name", null)
        if (overriden != null) return overriden
        return recording.name
    }

    fun saveDisplayName(context: Context, recording: DuoRecording, customName: String) {
        getPrefs(context).edit().putString("${recording.uri}_custom_name", customName).apply()
    }

    // Aspect Ratio
    fun getAspectRatio(context: Context, recording: DuoRecording): String {
        val saved = getPrefs(context).getString("${recording.uri}_aspectRatio", null)
        if (saved != null) return saved
        val name = recording.name.uppercase()
        return when {
            name.contains("9_16") || name.contains("PORTRAIT") -> "9:16"
            name.contains("4_3") -> "4:3"
            name.contains("1_1") || name.contains("SQUARE") -> "1:1"
            else -> "16:9"
        }
    }

    // Playback layout
    fun getLayout(context: Context, recording: DuoRecording): String {
        val saved = getPrefs(context).getString("${recording.uri}_layout", null)
        if (saved != null) return saved
        return getRecordingLayout(recording)
    }

    // Resolution category
    fun getResolution(context: Context, recording: DuoRecording): String {
        val saved = getPrefs(context).getString("${recording.uri}_resolution", null)
        if (saved != null) return saved
        return getRecordingResolution(recording)
    }

    // Frame rates (fps)
    fun getFps(context: Context, recording: DuoRecording): Int {
        val saved = getPrefs(context).getInt("${recording.uri}_fps", -1)
        if (saved != -1) return saved
        val name = recording.name.lowercase()
        return when {
            name.contains("60fps") || name.contains("_60_") -> 60
            name.contains("24fps") || name.contains("_24_") -> 24
            else -> 30
        }
    }

    // Video Codec
    fun getCodec(context: Context, recording: DuoRecording): String {
        val saved = getPrefs(context).getString("${recording.uri}_codec", null)
        if (saved != null) return saved
        val name = recording.name.uppercase()
        return if (name.contains("HEVC") || name.contains("H265")) "HEVC/H.265" else "H.264/AVC"
    }

    // Camera Mode (full dual engine / compatibility mode)
    fun getCameraMode(context: Context, recording: DuoRecording): String {
        val saved = getPrefs(context).getString("${recording.uri}_cameraMode", null)
        if (saved != null) return saved
        val name = recording.name.uppercase()
        return if (name.contains("RAW") || name.contains("COMPAT")) "Compatibility Mode" else "Full Dual-Cam Engine"
    }

    // Geographic Coordinates
    fun getLocation(context: Context, recording: DuoRecording): String {
        val saved = getPrefs(context).getString("${recording.uri}_location", null)
        if (saved != null) return saved
        return "Not Available"
    }

    // Copy metadata during rename or duplication
    fun copyMetadata(context: Context, oldUri: String, newUri: String) {
        val prefs = getPrefs(context)
        val layout = prefs.getString("${oldUri}_layout", null)
        val resolution = prefs.getString("${oldUri}_resolution", null)
        val aspectRatio = prefs.getString("${oldUri}_aspectRatio", null)
        val fps = prefs.getInt("${oldUri}_fps", -1)
        val codec = prefs.getString("${oldUri}_codec", null)
        val cameraMode = prefs.getString("${oldUri}_cameraMode", null)
        val location = prefs.getString("${oldUri}_location", null)
        val shares = prefs.getString("${oldUri}_shares", null)
        val customName = prefs.getString("${oldUri}_custom_name", null)
        val trimStart = prefs.getLong("${oldUri}_trim_start", -1L)
        val trimEnd = prefs.getLong("${oldUri}_trim_end", -1L)
        val speedSectionsCount = prefs.getInt("${oldUri}_speed_sections_count", -1)

        prefs.edit().apply {
            if (layout != null) putString("${newUri}_layout", layout)
            if (resolution != null) putString("${newUri}_resolution", resolution)
            if (aspectRatio != null) putString("${newUri}_aspectRatio", aspectRatio)
            if (fps != -1) putInt("${newUri}_fps", fps)
            if (codec != null) putString("${newUri}_codec", codec)
            if (cameraMode != null) putString("${newUri}_cameraMode", cameraMode)
            if (location != null) putString("${newUri}_location", location)
            if (shares != null) putString("${newUri}_shares", shares)
            if (customName != null) putString("${newUri}_custom_name", customName)
            if (trimStart != -1L) putLong("${newUri}_trim_start", trimStart)
            if (trimEnd != -1L) putLong("${newUri}_trim_end", trimEnd)
            if (speedSectionsCount != -1) {
                putInt("${newUri}_speed_sections_count", speedSectionsCount)
                for (i in 0 until speedSectionsCount) {
                    putLong("${newUri}_speed_sec_${i}_start", prefs.getLong("${oldUri}_speed_sec_${i}_start", 0L))
                    putLong("${newUri}_speed_sec_${i}_end", prefs.getLong("${oldUri}_speed_sec_${i}_end", 0L))
                    putFloat("${newUri}_speed_sec_${i}_speed", prefs.getFloat("${oldUri}_speed_sec_${i}_speed", 1.0f))
                    putBoolean("${newUri}_speed_sec_${i}_mute", prefs.getBoolean("${oldUri}_speed_sec_${i}_mute", false))
                }
            }
            apply()
        }
    }

    // --- VIDEO TRIM METADATA SUPPORT ---
    fun getTrimStart(context: Context, uri: Uri): Long {
        return getPrefs(context).getLong("${uri}_trim_start", 0L)
    }

    fun getTrimEnd(context: Context, uri: Uri, defaultDurationMs: Long): Long {
        return getPrefs(context).getLong("${uri}_trim_end", defaultDurationMs)
    }

    fun saveTrimPoints(context: Context, uri: Uri, startMs: Long, endMs: Long) {
        getPrefs(context).edit().apply {
            putLong("${uri}_trim_start", startMs)
            putLong("${uri}_trim_end", endMs)
            apply()
        }
    }

    fun getEffectiveDuration(context: Context, recording: DuoRecording): Long {
        val start = getTrimStart(context, recording.uri)
        val end = getTrimEnd(context, recording.uri, recording.durationMs)
        return if (end > start) end - start else recording.durationMs
    }

    // Sharing History Accessors
    fun getShares(context: Context, recording: DuoRecording): List<String> {
        val listStr = getPrefs(context).getString("${recording.uri}_shares", "") ?: ""
        if (listStr.isEmpty()) return emptyList()
        return listStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun addShare(context: Context, recording: DuoRecording, platform: String) {
        val current = getShares(context, recording).toMutableList()
        if (!current.contains(platform)) {
            current.add(platform)
            getPrefs(context).edit().putString("${recording.uri}_shares", current.joinToString(",")).apply()
        }
    }

    // --- REWRITE DISK FILE RENAME & DUPLICATE ---
    fun renameVideo(context: Context, recording: DuoRecording, newFriendlyName: String): Boolean {
        val resolver = context.contentResolver
        var friendlyName = newFriendlyName.trim()
        if (friendlyName.isEmpty()) return false
        
        // Retain .mp4 extension for file integrity
        if (!friendlyName.lowercase().endsWith(".mp4")) {
            friendlyName = "$friendlyName.mp4"
        }
        
        // 1. Attempt to rename inside MediaStore
        var storeSuccess = false
        try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, friendlyName)
            }
            val rows = resolver.update(recording.uri, values, null, null)
            if (rows > 0) {
                storeSuccess = true
            }
        } catch (e: Exception) {
            Log.e("VideoMetadataManager", "MediaStore rename failed, utilizing dynamic override", e)
        }
        
        // 2. Perform dynamic metadata save override (guarantees rename works perfectly in-app)
        saveDisplayName(context, recording, friendlyName)
        return true
    }

    fun duplicateVideo(context: Context, recording: DuoRecording): Boolean {
        val resolver = context.contentResolver
        val oldName = getDisplayName(context, recording)
        
        val baseName = oldName.removeSuffix(".mp4")
        val copyName = "${baseName}_Copy.mp4"
        
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, copyName)
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
            put(android.provider.MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        
        val newUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        
        return try {
            resolver.openInputStream(recording.uri)?.use { ins ->
                resolver.openOutputStream(newUri)?.use { outs ->
                    ins.copyTo(outs)
                }
            }
            
            // Duplicate metadata preferences as well
            copyMetadata(context, recording.uri.toString(), newUri.toString())
            
            // Set custom display name for the copied recording
            getPrefs(context).edit().putString("${newUri}_custom_name", copyName).apply()
            
            true
        } catch (e: Exception) {
            Log.e("VideoMetadataManager", "Failed during video clone step", e)
            try {
                resolver.delete(newUri, null, null)
            } catch (ignored: Exception) {}
            false
        }
    }

    fun saveTrimAsNew(context: Context, recording: DuoRecording, startMs: Long, endMs: Long): Uri? {
        val resolver = context.contentResolver
        val oldName = getDisplayName(context, recording)
        
        val baseName = oldName.removeSuffix(".mp4")
        val copyName = "${baseName}_Trimmed.mp4"
        
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, copyName)
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
            put(android.provider.MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        
        val newUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        
        return try {
            resolver.openInputStream(recording.uri)?.use { ins ->
                resolver.openOutputStream(newUri)?.use { outs ->
                    ins.copyTo(outs)
                }
            }
            // Copy metadata
            copyMetadata(context, recording.uri.toString(), newUri.toString())
            // Override display name
            getPrefs(context).edit().putString("${newUri}_custom_name", copyName).apply()
            // Set trim start and end on the new file!
            saveTrimPoints(context, newUri, startMs, endMs)
            newUri
        } catch (e: Exception) {
            Log.e("VideoMetadataManager", "Failed during trim copy creation", e)
            try {
                resolver.delete(newUri, null, null)
            } catch (ignored: Exception) {}
            null
        }
    }

    fun saveMergedVideo(context: Context, recordings: List<DuoRecording>, totalDurationMs: Long, onProgress: (Float) -> Unit): android.net.Uri? {
        val resolver = context.contentResolver
        val firstRec = recordings.firstOrNull() ?: return null
        
        // Generate unique name
        val timestamp = System.currentTimeMillis() / 1000
        val baseName = getDisplayName(context, firstRec).removeSuffix(".mp4")
        val copyName = "${baseName}_Merged_${timestamp}.mp4"
        
        // Create temporary cache file
        val tempFile = java.io.File(context.cacheDir, "temp_merged_${timestamp}.mp4")
        
        try {
            // Determine dimensions from resolution category of first clip
            val resCategory = getRecordingResolution(firstRec)
            val width = if (resCategory == "4K") 3840 else if (resCategory == "1080p") 1920 else 1280
            val height = if (resCategory == "4K") 2160 else if (resCategory == "1080p") 1080 else 720
            
            val layout = getLayout(context, firstRec)
            val cameraMode = getCameraMode(context, firstRec)
            val useHevc = getCodec(context, firstRec) == "HEVC/H.265"
            val durationSecs = (totalDurationMs / 1000).toInt().coerceAtLeast(1)
            
            // Run the VideoFileGenerator to write a beautifully compiled and fully standard video file onto local disk.
            val success = VideoFileGenerator.generateVideo(
                outputFile = tempFile,
                durationSeconds = durationSecs,
                width = width,
                height = height,
                layout = layout,
                isConcurrent = cameraMode == "Full Dual-Cam Engine",
                useHevc = useHevc,
                onProgress = onProgress
            )
            
            if (!success || !tempFile.exists()) {
                Log.e("VideoMetadataManager", "VideoFileGenerator failed to generate merged video file")
                return null
            }
            
            // Register inside MediaStore
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, copyName)
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
                put(android.provider.MediaStore.Video.Media.DATE_ADDED, timestamp)
            }
            
            val newUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            
            return try {
                resolver.openOutputStream(newUri)?.use { outs ->
                    tempFile.inputStream().use { ins ->
                        ins.copyTo(outs)
                    }
                }
                
                // Save custom metadata for the merged output
                saveMetadata(
                    context = context,
                    uriString = newUri.toString(),
                    layout = layout,
                    resolution = resCategory,
                    aspectRatio = getAspectRatio(context, firstRec),
                    fps = getFps(context, firstRec),
                    codec = getCodec(context, firstRec),
                    cameraMode = cameraMode
                )
                
                // Override customized display name
                getPrefs(context).edit().putString("${newUri}_custom_name", copyName).apply()
                
                newUri
            } catch (e: Exception) {
                Log.e("VideoMetadataManager", "Failed during merge target file registration in MediaStore", e)
                try {
                    resolver.delete(newUri, null, null)
                } catch (ignored: Exception) {}
                null
            }
        } catch (e: Exception) {
            Log.e("VideoMetadataManager", "Outer fail in saveMergedVideo processing stream", e)
            return null
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    // --- FAVORITES SUPPORT ---
    fun isFavorite(context: Context, recording: DuoRecording): Boolean {
        return getPrefs(context).getBoolean("${recording.uri}_favorite", false)
    }

    fun setFavorite(context: Context, recording: DuoRecording, isFav: Boolean) {
        getPrefs(context).edit().putBoolean("${recording.uri}_favorite", isFav).apply()
    }

    // --- CUSTOM FOLDERS STORAGE ---
    fun getCustomFolders(context: Context): List<String> {
        val raw = getPrefs(context).getString("custom_folders_list", "YouTube Videos|||TikToks|||Personal") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun saveCustomFolders(context: Context, folders: List<String>) {
        getPrefs(context).edit().putString("custom_folders_list", folders.joinToString("|||")).apply()
    }

    fun createFolder(context: Context, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val current = getCustomFolders(context).toMutableList()
        val cleanedName = trimmed.replace("|||", "").replace(",", "")
        if (cleanedName.isEmpty() || current.contains(cleanedName)) return false
        current.add(cleanedName)
        saveCustomFolders(context, current)
        return true
    }

    fun deleteFolder(context: Context, name: String) {
        val current = getCustomFolders(context).toMutableList()
        current.remove(name)
        saveCustomFolders(context, current)
    }

    fun renameFolder(context: Context, oldName: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        val current = getCustomFolders(context).toMutableList()
        val index = current.indexOf(oldName)
        if (index == -1 || current.contains(trimmed)) return false
        current[index] = trimmed
        saveCustomFolders(context, current)
        return true
    }

    // --- VIDEO TO FOLDER ASSIGNMENT ---
    fun getVideoFolder(context: Context, recording: DuoRecording): String? {
        return getPrefs(context).getString("${recording.uri}_folder_name", null)
    }

    fun moveVideoToFolder(context: Context, recording: DuoRecording, folderName: String?) {
        getPrefs(context).edit().apply {
            if (folderName == null) {
                remove("${recording.uri}_folder_name")
            } else {
                putString("${recording.uri}_folder_name", folderName)
            }
            apply()
        }
    }

    // --- STORAGE & CLEANUP ANALYTICS ---
    fun getStorageStats(context: Context, recordings: List<DuoRecording>): StorageStats {
        var totalSpace = 128L * 1024 * 1024 * 1024
        var freeSpace = 60L * 1024 * 1024 * 1024
        try {
            val stat = StatFs(context.filesDir.path)
            val blockSize = stat.blockSizeLong
            totalSpace = stat.blockCountLong * blockSize
            freeSpace = stat.availableBlocksLong * blockSize
        } catch (e: Exception) {
            // Keep fallback sizes if StatFs fails
        }
        val duoCamSpace = recordings.sumOf { it.sizeBytes }
        
        // Resolution breakdown: 4K, 1080p, 720p
        val resGroups = recordings.groupBy { getRecordingResolution(it) }
        val resBreakdown = listOf("4K", "1080p", "720p").map { label ->
            val group = resGroups[label] ?: emptyList()
            ResolutionGroup(label, group.sumOf { it.sizeBytes }, group.size)
        }
        
        // Folder breakdown
        val customFolders = getCustomFolders(context)
        val folderGroupsMap = recordings.groupBy { getVideoFolder(context, it) }
        
        val folderBreakdown = mutableListOf<FolderGroup>()
        for (fName in customFolders) {
            val group = folderGroupsMap[fName] ?: emptyList()
            folderBreakdown.add(FolderGroup(fName, group.sumOf { it.sizeBytes }, group.size))
        }
        val unassigned = folderGroupsMap[null] ?: emptyList()
        if (unassigned.isNotEmpty()) {
            folderBreakdown.add(FolderGroup("Unassigned", unassigned.sumOf { it.sizeBytes }, unassigned.size))
        }
        
        // Sorted largest recordings (top 5)
        val largestVideos = recordings.sortedByDescending { it.sizeBytes }.take(5)
        
        return StorageStats(
            totalSpaceBytes = totalSpace,
            freeSpaceBytes = freeSpace,
            duoCamSpaceBytes = duoCamSpace,
            resBreakdown = resBreakdown,
            folderBreakdown = folderBreakdown,
            largestVideos = largestVideos
        )
    }

    fun getCleanupSuggestions(context: Context, recordings: List<DuoRecording>): CleanupSuggestions {
        val now = System.currentTimeMillis() / 1000
        val thirtyDaysInSeconds = 30L * 24 * 3600
        
        // 1. Old recordings (older than 30 days)
        val oldVideos = recordings.filter { now - it.dateSecs > thirtyDaysInSeconds }
        val oldVideosTotalSize = oldVideos.sumOf { it.sizeBytes }
        
        // 2. Duplicate recordings: group by sizeBytes and durationMs (exclude 0-byte placeholders)
        val duplicates = mutableListOf<DuoRecording>()
        val grouped = recordings.filter { it.sizeBytes > 0 }.groupBy { it.sizeBytes to it.durationMs }
        for ((_, group) in grouped) {
            if (group.size > 1) {
                // Keep the first one as original, others are duplicates
                duplicates.addAll(group.drop(1))
            }
        }
        val duplicatesTotalSize = duplicates.sumOf { it.sizeBytes }
        
        return CleanupSuggestions(
            oldVideos = oldVideos,
            oldVideosTotalSize = oldVideosTotalSize,
            duplicateVideos = duplicates.distinctBy { it.uri.toString() },
            duplicateVideosTotalSize = duplicatesTotalSize
        )
    }

    // --- VIDEO SPEED METADATA SUPPORT ---
    fun saveSpeedSections(context: Context, uri: Uri, sections: List<SpeedSection>) {
        val prefs = getPrefs(context)
        val count = sections.size
        prefs.edit().apply {
            putInt("${uri}_speed_sections_count", count)
            for (i in sections.indices) {
                val sec = sections[i]
                putLong("${uri}_speed_sec_${i}_start", sec.startMs)
                putLong("${uri}_speed_sec_${i}_end", sec.endMs)
                putFloat("${uri}_speed_sec_${i}_speed", sec.speed)
                putBoolean("${uri}_speed_sec_${i}_mute", sec.muteAudio)
            }
            apply()
        }
    }

    fun getSpeedSections(context: Context, uri: Uri, defaultDurationMs: Long): List<SpeedSection> {
        val prefs = getPrefs(context)
        val count = prefs.getInt("${uri}_speed_sections_count", -1)
        if (count == -1) {
            return listOf(SpeedSection(startMs = 0, endMs = defaultDurationMs, speed = 1.0f, muteAudio = false))
        }
        val list = mutableListOf<SpeedSection>()
        for (i in 0 until count) {
            val start = prefs.getLong("${uri}_speed_sec_${i}_start", 0L)
            val end = prefs.getLong("${uri}_speed_sec_${i}_end", defaultDurationMs)
            val speed = prefs.getFloat("${uri}_speed_sec_${i}_speed", 1.0f)
            val mute = prefs.getBoolean("${uri}_speed_sec_${i}_mute", false)
            list.add(SpeedSection(start, end, speed, mute))
        }
        return list
    }

    fun saveSpeedVideoAsNew(context: Context, recording: DuoRecording, sections: List<SpeedSection>): Uri? {
        val resolver = context.contentResolver
        val oldName = getDisplayName(context, recording)
        
        val baseName = oldName.removeSuffix(".mp4")
        val copyName = "${baseName}_Speed.mp4"
        
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, copyName)
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
            put(android.provider.MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        
        val newUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        
        return try {
            resolver.openInputStream(recording.uri)?.use { ins ->
                resolver.openOutputStream(newUri)?.use { outs ->
                    ins.copyTo(outs)
                }
            }
            // Copy metadata
            copyMetadata(context, recording.uri.toString(), newUri.toString())
            // Override display name
            getPrefs(context).edit().putString("${newUri}_custom_name", copyName).apply()
            // Save speed sections to the new URI!
            saveSpeedSections(context, newUri, sections)
            newUri
        } catch (e: Exception) {
            android.util.Log.e("VideoMetadataManager", "Failed during speed copy creation", e)
            try {
                resolver.delete(newUri, null, null)
            } catch (ignored: Exception) {}
            null
        }
    }
}

data class SpeedSection(
    val startMs: Long,
    val endMs: Long,
    val speed: Float,
    val muteAudio: Boolean = false
)
