package com.example

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StretchedSection(
    val section: SpeedSection,
    val origStartMs: Long,
    val origEndMs: Long,
    val stretchedStartMs: Long,
    val stretchedEndMs: Long,
    val speed: Float,
    val muteAudio: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    recording: DuoRecording,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sharedPreferences = remember { context.getSharedPreferences("duocam_prefs", android.content.Context.MODE_PRIVATE) }
    var isPro by remember { mutableStateOf(sharedPreferences.getBoolean("is_pro_upgraded", false)) }
    var showGoProScreen by remember { mutableStateOf(false) }
    var activeGoProFeatureName by remember { mutableStateOf<String?>(null) }

    fun triggerGoPro(feature: String) {
        activeGoProFeatureName = feature
        showGoProScreen = true
    }

    // Video playback state
    var isPlayingState by remember { mutableStateOf(false) }
    var currentPlaybackPosition by remember { mutableStateOf(0L) }
    val totalDurationMs = recording.durationMs

    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var lastSeekTime by remember { mutableStateOf(0L) }

    fun throttledSeek(targetMs: Long) {
        currentPlaybackPosition = targetMs
        val now = System.currentTimeMillis()
        if (now - lastSeekTime > 80) { // Limit hardware seeking to once every 80ms
            lastSeekTime = now
            videoViewInstance?.seekTo(targetMs.toInt())
        }
    }

    // Sequential video thumbnail frame previews
    val thumbnails = remember { mutableStateListOf<Bitmap>() }
    var framesLoading by remember { mutableStateOf(true) }

    // Export simulated pipeline states
    var showExportDialog by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var isExporting by remember { mutableStateOf(false) }

    // UI Interactive tool modifiers
    var activeTool by remember { mutableStateOf("Trim") }
    var showUnsavedChangesConfirm by remember { mutableStateOf(false) }
    
    val initialStartMs = remember(recording.uri) { VideoMetadataManager.getTrimStart(context, recording.uri) }
    val initialEndMs = remember(recording.uri) { VideoMetadataManager.getTrimEnd(context, recording.uri, recording.durationMs) }
    
    var isTrimRangeActive by remember { mutableStateOf(true) }
    var trimStartPct by remember { mutableFloatStateOf(if (recording.durationMs > 0) initialStartMs.toFloat() / recording.durationMs else 0f) }
    var trimEndPct by remember { mutableFloatStateOf(if (recording.durationMs > 0) initialEndMs.toFloat() / recording.durationMs else 1f) }

    var activeHandleForFineTune by remember { mutableStateOf("start") }
    
    var showTrimSaveDialog by remember { mutableStateOf(false) }
    var trimSaveProgress by remember { mutableFloatStateOf(0f) }
    var isSavingTrim by remember { mutableStateOf(false) }
    var showReplaceAlertConfirm by remember { mutableStateOf(false) }

    var showSpeedSaveDialog by remember { mutableStateOf(false) }
    var speedSaveProgress by remember { mutableFloatStateOf(0f) }
    var isSavingSpeed by remember { mutableStateOf(false) }
    var showSpeedReplaceAlertConfirm by remember { mutableStateOf(false) }

    val mergeClips = remember { mutableStateListOf<DuoRecording>(recording) }
    val currentPlayingIndex = remember { mutableStateOf(0) }
    val clipTransitions = remember { mutableStateMapOf<Int, ClipTransition>() }
    var editingTransitionIndex by remember { mutableStateOf<Int?>(null) }
    val mergeClipThumbnails = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(mergeClips.map { it.uri }) {
        mergeClips.forEach { clip ->
            if (!mergeClipThumbnails.containsKey(clip.uri.toString())) {
                withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, clip.uri)
                        val bmp = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bmp != null) {
                            val scaled = Bitmap.createScaledBitmap(bmp, 140, 90, false)
                            withContext(Dispatchers.Main) {
                                mergeClipThumbnails[clip.uri.toString()] = scaled
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("VideoEditorScreen", "Error getting start thumbnail for ${clip.uri}", e)
                    } finally {
                        try { retriever.release() } catch (ignored: Exception) {}
                    }
                }
            }
        }
    }
    
    var mediaPlayerInstance by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    // Speed control state variables
    var speedStartPct by remember { mutableFloatStateOf(0.0f) }
    var speedEndPct by remember { mutableFloatStateOf(1.0f) }
    var speedAdjustMode by remember { mutableStateOf("section") } // "entire" or "section"
    val speedSections = remember { mutableStateListOf<SpeedSection>() }

    LaunchedEffect(speedAdjustMode) {
        if (speedAdjustMode == "entire") {
            speedStartPct = 0.0f
            speedEndPct = 1.0f
        }
    }

    // Background Music states
    var selectedMusicUri by remember { mutableStateOf<String?>(null) }
    var selectedMusicName by remember { mutableStateOf<String>("No Background Music") }
    var selectedMusicMood by remember { mutableStateOf<String?>(null) }
    var originalAudioVolume by remember { mutableFloatStateOf(1.0f) }
    var musicVolume by remember { mutableFloatStateOf(0.5f) }
    var musicTrimStartPct by remember { mutableFloatStateOf(0.0f) }
    var musicTrimEndPct by remember { mutableFloatStateOf(1.0f) }
    var loopMusic by remember { mutableStateOf(true) }
    var musicOffsetMs by remember { mutableLongStateOf(0L) }
    var musicFadeIn by remember { mutableStateOf(true) }
    var musicFadeOut by remember { mutableStateOf(true) }

    var localMusicPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    val synthSynthesizer = remember { MoodMelodySynthesizer() }

    // List of active text overlays
    val textOverlays = remember { mutableStateListOf<TextOverlay>() }
    // Currently selected text overlay for editing
    var selectedOverlayId by remember { mutableStateOf<String?>(null) }

    // Visual Filter & Color Grading states
    var selectedFilterId by remember { mutableStateOf("original") }
    var filterIntensity by remember { mutableFloatStateOf(1.0f) }

    // Manual color grading variables (0 = default/no effect, -1 = minimum, 1 = maximum)
    var brightnessVal by remember { mutableFloatStateOf(0.0f) }
    var contrastVal by remember { mutableFloatStateOf(0.0f) }
    var saturationVal by remember { mutableFloatStateOf(0.0f) }
    var temperatureVal by remember { mutableFloatStateOf(0.0f) }
    var highlightsVal by remember { mutableFloatStateOf(0.0f) }
    var shadowsVal by remember { mutableFloatStateOf(0.0f) }
    var sharpnessVal by remember { mutableFloatStateOf(0.0f) }

    // Before/After comparison hold state
    var isHoldingBeforeByTap by remember { mutableStateOf(false) }

    fun syncBackgroundMusic(videoCurrentPosMs: Long, isVideoPlaying: Boolean) {
        val musicUri = selectedMusicUri
        val synthMood = selectedMusicMood
        
        val baseVolume = musicVolume
        var activeVolume = baseVolume

        val totalOfMusicSegment = if (synthMood != null) {
            120000L
        } else if (localMusicPlayer != null) {
            try { localMusicPlayer!!.duration.toLong() } catch (e: Exception) { 120000L }
        } else {
            120000L
        }

        val trimStartMs = (musicTrimStartPct * totalOfMusicSegment).toLong()
        val trimEndMs = (musicTrimEndPct * totalOfMusicSegment).toLong()
        val trimmedDurationMs = if (trimEndMs > trimStartMs) trimEndMs - trimStartMs else totalOfMusicSegment

        val elapsedMsFromVideoStart = videoCurrentPosMs - musicOffsetMs

        if (elapsedMsFromVideoStart < 0) {
            synthSynthesizer.setVolume(0f)
            try { localMusicPlayer?.setVolume(0f, 0f) } catch (ignored: Exception) {}
            if (synthMood != null) {
                synthSynthesizer.stop()
            } else {
                try { if (localMusicPlayer?.isPlaying == true) localMusicPlayer?.pause() } catch (ignored: Exception) {}
            }
            return
        }

        if (elapsedMsFromVideoStart >= trimmedDurationMs && !loopMusic) {
            synthSynthesizer.setVolume(0f)
            try { localMusicPlayer?.setVolume(0f, 0f) } catch (ignored: Exception) {}
            if (synthMood != null) {
                synthSynthesizer.stop()
            } else {
                try { if (localMusicPlayer?.isPlaying == true) localMusicPlayer?.pause() } catch (ignored: Exception) {}
            }
            return
        }

        val currentMusicTimeMs = if (loopMusic) {
            trimStartMs + (elapsedMsFromVideoStart % trimmedDurationMs)
        } else {
            trimStartMs + elapsedMsFromVideoStart
        }

        val fadeInDurationMs = 2000L
        val fadeOutDurationMs = 2000L
        
        val timeSinceMusicStart = elapsedMsFromVideoStart
        val timeUntilMusicEnd = if (loopMusic) {
            totalDurationMs - videoCurrentPosMs
        } else {
            trimmedDurationMs - elapsedMsFromVideoStart
        }

        if (musicFadeIn && timeSinceMusicStart < fadeInDurationMs) {
            val fraction = timeSinceMusicStart.toFloat() / fadeInDurationMs.toFloat()
            activeVolume *= fraction
        } else if (musicFadeOut && timeUntilMusicEnd < fadeOutDurationMs) {
            val fraction = timeUntilMusicEnd.toFloat() / fadeOutDurationMs.toFloat()
            activeVolume *= fraction
        }

        activeVolume = activeVolume.coerceIn(0f, 1f)

        if (synthMood != null) {
            synthSynthesizer.setVolume(activeVolume)
            if (isVideoPlaying) {
                if (!synthSynthesizer.isPlaying) {
                    synthSynthesizer.start(synthMood)
                }
            } else {
                synthSynthesizer.stop()
            }
        } else if (musicUri != null && localMusicPlayer != null) {
            try {
                localMusicPlayer?.let { mp ->
                    mp.setVolume(activeVolume, activeVolume)
                    if (isVideoPlaying) {
                        if (!mp.isPlaying) {
                            mp.start()
                        }
                        val currentTrackPos = mp.currentPosition.toLong()
                        if (Math.abs(currentTrackPos - currentMusicTimeMs) > 150L) {
                            mp.seekTo(currentMusicTimeMs.toInt())
                        }
                    } else {
                        if (mp.isPlaying) {
                            mp.pause()
                        }
                        mp.seekTo(currentMusicTimeMs.toInt())
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoEditor", "Failed dynamic bg music sync", e)
            }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMusicUri = uri.toString()
            selectedMusicName = getAudioFileName(context, uri)
            selectedMusicMood = null
            try {
                localMusicPlayer?.release()
                localMusicPlayer = android.media.MediaPlayer.create(context, uri).apply {
                    isLooping = loopMusic
                    setVolume(musicVolume, musicVolume)
                }
                Toast.makeText(context, "Loaded audio: $selectedMusicName", Toast.LENGTH_SHORT).show()
                syncBackgroundMusic(currentPlaybackPosition, isPlayingState)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load audio file", Toast.LENGTH_SHORT).show()
                Log.e("VideoEditor", "Failed to load audio uri=$uri", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                localMusicPlayer?.stop()
                localMusicPlayer?.release()
            } catch (ignored: Exception) {}
            synthSynthesizer.stop()
        }
    }

    // Initialize speed sections list with full 1x track if empty, or load saved speed sections if they exist
    LaunchedEffect(recording.uri, totalDurationMs) {
        if (speedSections.isEmpty() && totalDurationMs > 0) {
            val savedSections = VideoMetadataManager.getSpeedSections(context, recording.uri, totalDurationMs)
            speedSections.addAll(savedSections)
        }
    }

    val stretchedSections = remember(speedSections.size, speedSections.map { "${it.startMs}-${it.endMs}-${it.speed}-${it.muteAudio}" }.joinToString(",")) {
        val list = mutableListOf<StretchedSection>()
        var accumulatedStretchedMs = 0L
        for (sec in speedSections) {
            val origDur = sec.endMs - sec.startMs
            val stretchedDur = (origDur / sec.speed).toLong()
            list.add(
                StretchedSection(
                    section = sec,
                    origStartMs = sec.startMs,
                    origEndMs = sec.endMs,
                    stretchedStartMs = accumulatedStretchedMs,
                    stretchedEndMs = accumulatedStretchedMs + stretchedDur,
                    speed = sec.speed,
                    muteAudio = sec.muteAudio
                )
            )
            accumulatedStretchedMs += stretchedDur
        }
        list
    }

    val totalStretchedDurationMs = remember(stretchedSections) {
        stretchedSections.lastOrNull()?.stretchedEndMs ?: totalDurationMs
    }

    fun getOriginalTimeForStretchedTime(stretchedMs: Long): Long {
        if (stretchedSections.isEmpty()) return stretchedMs
        val pos = stretchedMs.coerceIn(0L, totalStretchedDurationMs)
        for (sec in stretchedSections) {
            if (pos >= sec.stretchedStartMs && pos <= sec.stretchedEndMs) {
                val relStretched = pos - sec.stretchedStartMs
                val relOrig = (relStretched * sec.speed).toLong()
                return (sec.origStartMs + relOrig).coerceIn(sec.origStartMs, sec.origEndMs)
            }
        }
        return stretchedMs
    }

    fun getStretchedTimeForOriginalTime(originalMs: Long): Long {
        if (stretchedSections.isEmpty()) return originalMs
        val pos = originalMs.coerceIn(0L, totalDurationMs)
        for (sec in stretchedSections) {
            if (pos >= sec.origStartMs && pos <= sec.origEndMs) {
                val relOrig = pos - sec.origStartMs
                val relStretched = (relOrig / sec.speed).toLong()
                return sec.stretchedStartMs + relStretched
            }
        }
        return originalMs
    }

    fun applySpeedToRange(startMs: Long, endMs: Long, speed: Float, mute: Boolean) {
        val newSections = mutableListOf<SpeedSection>()
        for (sec in speedSections) {
            if (sec.endMs <= startMs || sec.startMs >= endMs) {
                newSections.add(sec)
            } else {
                if (sec.startMs < startMs) {
                    newSections.add(sec.copy(endMs = startMs))
                }
                if (sec.endMs > endMs) {
                    newSections.add(sec.copy(startMs = endMs))
                }
            }
        }
        newSections.add(SpeedSection(startMs = startMs, endMs = endMs, speed = speed, muteAudio = mute))
        newSections.sortBy { it.startMs }
        
        // Merge contiguous identical sections
        val merged = mutableListOf<SpeedSection>()
        for (sec in newSections) {
            if (merged.isEmpty()) {
                merged.add(sec)
            } else {
                val last = merged.last()
                if (last.speed == sec.speed && last.muteAudio == sec.muteAudio) {
                    merged[merged.lastIndex] = last.copy(endMs = sec.endMs)
                } else {
                    merged.add(sec)
                }
            }
        }
        speedSections.clear()
        speedSections.addAll(merged)
    }

    fun resetSpeeds() {
        speedSections.clear()
        speedSections.add(SpeedSection(startMs = 0, endMs = totalDurationMs, speed = 1.0f, muteAudio = false))
    }
    
    val clipOffsets = remember(mergeClips.size, mergeClips.map { it.uri }.joinToString(",")) {
        var accumulated = 0L
        mergeClips.map { clip ->
            val start = VideoMetadataManager.getTrimStart(context, clip.uri)
            val end = VideoMetadataManager.getTrimEnd(context, clip.uri, clip.durationMs)
            val dur = end - start
            val offset = accumulated
            accumulated += dur
            Triple(clip, offset, dur)
        }
    }
    
    val totalMergeDurationMs = remember(clipOffsets) {
        clipOffsets.sumOf { it.third }
    }
    
    var showMergeSelectDialog by remember { mutableStateOf(false) }
    var showMergeProgressDialog by remember { mutableStateOf(false) }
    var mergeProgress by remember { mutableFloatStateOf(0f) }
    var isMergingProcessing by remember { mutableStateOf(false) }

    fun getClipAndPositionForGlobalTime(globalPosMs: Long): Pair<Int, Long> {
        if (clipOffsets.isEmpty()) return Pair(0, 0L)
        val tMs = globalPosMs.coerceIn(0L, totalMergeDurationMs)
        for (i in clipOffsets.indices) {
            val (_, offsetStart, dur) = clipOffsets[i]
            if (tMs >= offsetStart && tMs < offsetStart + dur) {
                val trimStart = VideoMetadataManager.getTrimStart(context, clipOffsets[i].first.uri)
                val rel = tMs - offsetStart + trimStart
                return Pair(i, rel)
            }
        }
        val lastIdx = clipOffsets.lastIndex
        val trimStart = VideoMetadataManager.getTrimStart(context, clipOffsets[lastIdx].first.uri)
        val relLast = (tMs - clipOffsets[lastIdx].second + trimStart).coerceAtMost(
            VideoMetadataManager.getTrimEnd(context, clipOffsets[lastIdx].first.uri, clipOffsets[lastIdx].first.durationMs)
        )
        return Pair(lastIdx, relLast)
    }

    fun throttledSeekMerge(clipIdx: Int, relPosMs: Long, globalMs: Long) {
        currentPlaybackPosition = globalMs
        val now = System.currentTimeMillis()
        if (now - lastSeekTime > 80) {
            lastSeekTime = now
            videoViewInstance?.let { view ->
                if (currentPlayingIndex.value != clipIdx) {
                    currentPlayingIndex.value = clipIdx
                    view.setVideoURI(mergeClips[clipIdx].uri)
                } else {
                    view.seekTo(relPosMs.toInt())
                }
            }
        }
    }

    val mismatchWarnings = remember(mergeClips.size, mergeClips.map { it.uri }) {
        val warnings = mutableListOf<String>()
        if (mergeClips.size > 1) {
            val firstRec = mergeClips[0]
            val firstRes = getRecordingResolution(firstRec)
            val firstAspect = VideoMetadataManager.getAspectRatio(context, firstRec)
            
            for (i in 1 until mergeClips.size) {
                val clip = mergeClips[i]
                val name = VideoMetadataManager.getDisplayName(context, clip)
                val res = getRecordingResolution(clip)
                val aspect = VideoMetadataManager.getAspectRatio(context, clip)
                
                if (res != firstRes) {
                    warnings.add("Resolution Mismatch: \"$name\" is in $res (Base clip is $firstRes). It will be automatically upscaled/downscaled to fit.")
                }
                if (aspect != firstAspect) {
                    warnings.add("Aspect Mismatch: \"$name\" has aspect ratio $aspect (Base clip is $firstAspect). It will be cropped/adjusted to match.")
                }
            }
        }
        warnings
    }

    val estimatedFileSizeBytes = remember(mergeClips.size, mergeClips.map { it.uri }) {
        mergeClips.sumOf { it.sizeBytes }
    }

    // Asynchronous thumbnail extraction on background scope
    LaunchedEffect(recording.uri) {
        framesLoading = true
        thumbnails.clear()
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, recording.uri)
                val durationUs = totalDurationMs * 1000
                val numFrames = 8
                val step = if (numFrames > 1) durationUs / (numFrames - 1) else 0L
                val tempBitmaps = mutableListOf<Bitmap>()
                for (i in 0 until numFrames) {
                    val timeUs = i * step
                    val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bmp != null) {
                        // Scaled to a lighter resolution to reduce heavy memory allocation
                        val scaled = Bitmap.createScaledBitmap(bmp, 140, 90, false)
                        tempBitmaps.add(scaled)
                    }
                }
                withContext(Dispatchers.Main) {
                    thumbnails.addAll(tempBitmaps)
                    framesLoading = false
                }
            } catch (e: Exception) {
                Log.e("VideoEditorScreen", "Error extracting video content thumbnails", e)
                withContext(Dispatchers.Main) {
                    framesLoading = false
                }
            } finally {
                try {
                    retriever.release()
                } catch (ignored: Exception) {}
            }
        }
    }

    LaunchedEffect(activeTool) {
        mediaPlayerInstance?.let { mp ->
            try {
                val params = mp.playbackParams
                params.speed = 1.0f
                params.pitch = 1.0f
                mp.playbackParams = params
                mp.setVolume(1.0f, 1.0f)
            } catch (e: Exception) {
                Log.e("VideoEditor", "Failed to reset playback params on tool change", e)
            }
        }
    }

    // Steady-state playback position tracker loop
    LaunchedEffect(isPlayingState, isScrubbing, isTrimRangeActive, trimStartPct, trimEndPct, activeTool, mergeClips.size, speedSections.size) {
        if (isPlayingState && !isScrubbing) {
            while (true) {
                if (activeTool == "Merge") {
                    val idx = currentPlayingIndex.value
                    if (idx in mergeClips.indices) {
                        videoViewInstance?.let { view ->
                            if (view.isPlaying) {
                                val current = view.currentPosition.toLong()
                                val startMs = VideoMetadataManager.getTrimStart(context, mergeClips[idx].uri)
                                val endMs = VideoMetadataManager.getTrimEnd(context, mergeClips[idx].uri, mergeClips[idx].durationMs)
                                
                                if (current < startMs) {
                                    view.seekTo(startMs.toInt())
                                    var acc = 0L
                                    for (k in 0 until idx) {
                                        acc += VideoMetadataManager.getEffectiveDuration(context, mergeClips[k])
                                    }
                                    currentPlaybackPosition = acc
                                } else if (current >= endMs) {
                                    if (idx + 1 < mergeClips.size) {
                                        val nextIdx = idx + 1
                                        currentPlayingIndex.value = nextIdx
                                        var acc = 0L
                                        for (k in 0 until nextIdx) {
                                            acc += VideoMetadataManager.getEffectiveDuration(context, mergeClips[k])
                                        }
                                        currentPlaybackPosition = acc
                                        view.setVideoURI(mergeClips[nextIdx].uri)
                                    } else {
                                        view.pause()
                                        isPlayingState = false
                                        currentPlayingIndex.value = 0
                                        currentPlaybackPosition = 0L
                                        view.setVideoURI(mergeClips[0].uri)
                                    }
                                } else {
                                    var acc = 0L
                                    for (k in 0 until idx) {
                                        acc += VideoMetadataManager.getEffectiveDuration(context, mergeClips[k])
                                    }
                                    currentPlaybackPosition = acc + (current - startMs)
                                }
                            } else {
                                isPlayingState = false
                            }
                        }
                    }
                } else {
                    val trimStartMs = (trimStartPct * totalDurationMs).toLong()
                    val trimEndMs = (trimEndPct * totalDurationMs).toLong()
                    videoViewInstance?.let { view ->
                        if (view.isPlaying) {
                            val current = view.currentPosition.toLong()
                            if (activeTool == "Speed") {
                                currentPlaybackPosition = current
                                val activeSec = stretchedSections.find { current >= it.origStartMs && current <= it.origEndMs }
                                if (activeSec != null) {
                                    mediaPlayerInstance?.let { mp ->
                                        try {
                                            val currentParams = mp.playbackParams
                                            val targetSpeed = activeSec.speed
                                            val targetPitch = if (activeSec.muteAudio) 1f else activeSec.speed
                                            val targetVol = if (activeSec.muteAudio) 0f else 1f
                                            if (currentParams.speed != targetSpeed || currentParams.pitch != targetPitch) {
                                                val params = mp.playbackParams
                                                params.speed = targetSpeed
                                                params.pitch = targetPitch
                                                mp.playbackParams = params
                                            }
                                            mp.setVolume(targetVol, targetVol)
                                        } catch (e: Exception) {
                                            Log.e("VideoEditor", "Could not apply speed to MediaPlayer", e)
                                        }
                                    }
                                }
                                if (current >= totalDurationMs) {
                                    view.seekTo(0)
                                    currentPlaybackPosition = 0L
                                }
                            } else if (isTrimRangeActive) {
                                if (current < trimStartMs) {
                                    view.seekTo(trimStartMs.toInt())
                                    currentPlaybackPosition = trimStartMs
                                } else if (current >= trimEndMs) {
                                    view.seekTo(trimStartMs.toInt())
                                    currentPlaybackPosition = trimStartMs
                                } else {
                                    currentPlaybackPosition = current
                                }
                            } else {
                                currentPlaybackPosition = current
                            }
                        } else {
                            isPlayingState = false
                        }
                    }
                }
                syncBackgroundMusic(currentPlaybackPosition, isPlayingState)
                delay(30) // Fast 30ms updates for silky display smoothness
            }
        } else {
            syncBackgroundMusic(currentPlaybackPosition, false)
        }
    }

    LaunchedEffect(originalAudioVolume, activeTool, mediaPlayerInstance) {
        if (activeTool != "Speed") {
            mediaPlayerInstance?.let { mp ->
                try {
                    mp.setVolume(originalAudioVolume, originalAudioVolume)
                } catch (e: Exception) {
                    Log.e("VideoEditor", "Failed to set original video volume", e)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("video_editor_root"),
        containerColor = ObsidianBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ObsidianBlack)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // PREMIUM TOP HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(DeepCharcoal.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showUnsavedChangesConfirm = true },
                        modifier = Modifier.testTag("editor_back_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Exit Editor", tint = TextWhite)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DUO-CAM EDITOR STUDIO",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp
                            ),
                            color = NeonGreen
                        )
                        Text(
                            text = recording.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }

                    Button(
                        onClick = {
                            VideoExportManager.currentRecording = recording
                            VideoExportManager.mergeClips.clear()
                            VideoExportManager.mergeClips.addAll(mergeClips)
                            VideoExportManager.activeTool = activeTool
                            VideoExportManager.totalMergeDurationMs = totalMergeDurationMs
                            VideoExportManager.exportState = "settings"
                            showExportDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp).testTag("editor_export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.IosShare,
                            contentDescription = "Export Video",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EXPORT",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Black
                        )
                    }
                }

                // MAIN COHESIVE EDITOR BODY
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // PREVIEW PLAYER WITH GLOW FRAME
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .pointerInput(activeTool, totalDurationMs) {
                                detectTapGestures { offset ->
                                    if (activeTool == "Text") {
                                        val parentWidth = size.width.toFloat()
                                        val parentHeight = size.height.toFloat()
                                        val tappedXPct = offset.x / parentWidth
                                        val tappedYPct = offset.y / parentHeight
                                        
                                        // Find if any visible overlay is clicked
                                        val currentMs = currentPlaybackPosition
                                        val currentPct = if (totalDurationMs > 0) currentMs.toFloat() / totalDurationMs else 0f
                                        val visibleOverlays = textOverlays.filter { currentPct in it.startPct..it.endPct }
                                        val clickedOverlay = visibleOverlays.find { overlay ->
                                            val dx = overlay.xPct - tappedXPct
                                            val dy = overlay.yPct - tappedYPct
                                            Math.abs(dx) < 0.15f && Math.abs(dy) < 0.10f
                                        }

                                        if (clickedOverlay != null) {
                                            selectedOverlayId = clickedOverlay.id
                                            Toast.makeText(context, "Selected overlay block", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Tap to Create new text overlay at this coordinate
                                            val startPct = (currentPlaybackPosition.toFloat() / totalDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                            val endPct = (startPct + 0.30f).coerceIn(0f, 1f)
                                            val newOverlay = TextOverlay(
                                                text = "New Title Block",
                                                xPct = tappedXPct.coerceIn(0.1f, 0.9f),
                                                yPct = tappedYPct.coerceIn(0.1f, 0.9f),
                                                startPct = startPct,
                                                endPct = endPct,
                                                color = Color.White,
                                                bgColor = Color.Black.copy(alpha = 0.5f),
                                                bgOpacity = 0.5f
                                            )
                                            textOverlays.add(newOverlay)
                                            selectedOverlayId = newOverlay.id
                                            Toast.makeText(context, "Added text overlay at tap location", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .testTag("video_preview_touch_zone"),
                        contentAlignment = Alignment.Center
                    ) {
                        key(recording.uri) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoURI(recording.uri)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = (activeTool != "Merge" || mergeClips.size <= 1)
                                            videoViewInstance = this@apply
                                            mediaPlayerInstance = mp
                                            val startPos = if (activeTool == "Merge") {
                                                val idx = currentPlayingIndex.value
                                                if (idx in mergeClips.indices) {
                                                    VideoMetadataManager.getTrimStart(context, mergeClips[idx].uri).toInt()
                                                } else 0
                                            } else {
                                                currentPlaybackPosition.toInt()
                                            }
                                            seekTo(startPos)
                                            if (isPlayingState) {
                                                start()
                                            }
                                        }
                                        setOnCompletionListener {
                                            if (activeTool == "Merge") {
                                                val idx = currentPlayingIndex.value
                                                if (idx + 1 < mergeClips.size) {
                                                    val nextIdx = idx + 1
                                                    currentPlayingIndex.value = nextIdx
                                                    setVideoURI(mergeClips[nextIdx].uri)
                                                } else {
                                                    isPlayingState = false
                                                }
                                            } else {
                                                isPlayingState = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize().testTag("editor_preview_player"),
                                update = { view ->
                                    videoViewInstance = view
                                }
                            )
                        }

                        // REAL-TIME VISUAL FILTERS & COLOR GRADING PREVIEW LAYER
                        if (!isHoldingBeforeByTap) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("filter_and_grading_preview_overlay")
                            ) {
                                // 1. Preset filters
                                when (selectedFilterId) {
                                    "vivid" -> {
                                        drawRect(
                                            color = Color(0xFFFF4081),
                                            alpha = 0.15f * filterIntensity,
                                            blendMode = BlendMode.Overlay
                                        )
                                        drawRect(
                                            color = Color.Yellow,
                                            alpha = 0.1f * filterIntensity,
                                            blendMode = BlendMode.ColorDodge
                                        )
                                        drawRect(
                                            color = Color(0xFF1A237E),
                                            alpha = 0.1f * filterIntensity,
                                            blendMode = BlendMode.Difference
                                        )
                                        drawRect(
                                            color = Color.Black,
                                            alpha = 0.08f * filterIntensity,
                                            blendMode = BlendMode.Overlay
                                        )
                                    }
                                    "warm" -> {
                                        drawRect(
                                            color = Color(0xFFFFB300),
                                            alpha = 0.28f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                        drawRect(
                                            color = Color(0xFFFFE082),
                                            alpha = 0.12f * filterIntensity,
                                            blendMode = BlendMode.Overlay
                                        )
                                    }
                                    "cool" -> {
                                        drawRect(
                                            color = Color(0xFF00E5FF),
                                            alpha = 0.25f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                        drawRect(
                                            color = Color(0xFF0D47A1),
                                            alpha = 0.12f * filterIntensity,
                                            blendMode = BlendMode.Overlay
                                        )
                                    }
                                    "bw" -> {
                                        drawRect(
                                            color = Color(0xFF808080),
                                            alpha = 1.0f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                        drawRect(
                                            color = Color.White,
                                            alpha = 0.05f * filterIntensity,
                                            blendMode = BlendMode.Overlay
                                        )
                                    }
                                    "vintage" -> {
                                        drawRect(
                                            color = Color(0xFF808080),
                                            alpha = 0.35f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                        drawRect(
                                            color = Color(0xFF8B5A2B),
                                            alpha = 0.28f * filterIntensity,
                                            blendMode = BlendMode.ColorDodge
                                        )
                                        drawRect(
                                            color = Color(0xFFF5DEB3),
                                            alpha = 0.15f * filterIntensity,
                                            blendMode = BlendMode.Multiply
                                        )
                                    }
                                    "cinema" -> {
                                        drawRect(
                                            color = Color(0xFF7E57C2),
                                            alpha = 0.12f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                        drawRect(
                                            color = Color(0xFF008080),
                                            alpha = 0.15f * filterIntensity,
                                            blendMode = BlendMode.ColorDodge
                                        )
                                        drawRect(
                                            color = Color(0xFFFF8C00),
                                            alpha = 0.15f * filterIntensity,
                                            blendMode = BlendMode.Softlight
                                        )
                                    }
                                    "night" -> {
                                        drawRect(
                                            color = Color(0xFF80D8FF),
                                            alpha = 0.25f * filterIntensity,
                                            blendMode = BlendMode.ColorDodge
                                        )
                                        drawRect(
                                            color = Color(0xFFE0F7FA),
                                            alpha = 0.15f * filterIntensity,
                                            blendMode = BlendMode.Screen
                                        )
                                    }
                                    "cyberpunk" -> {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color(0xFFFF007F), Color(0xFF00F5FF))
                                            ),
                                            alpha = 0.35f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                    }
                                    "sepia" -> {
                                        drawRect(
                                            color = Color(0xFF704214),
                                            alpha = 0.4f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                    }
                                    "rose" -> {
                                        drawRect(
                                            color = Color(0xFFFF3366),
                                            alpha = 0.25f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                        drawRect(
                                            color = Color(0xFFFFF0F5),
                                            alpha = 0.12f * filterIntensity,
                                            blendMode = BlendMode.Overlay
                                        )
                                    }
                                    "forest" -> {
                                        drawRect(
                                            color = Color(0xFF2E7D32),
                                            alpha = 0.32f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                        drawRect(
                                            color = Color(0xFF0D5C3A),
                                            alpha = 0.15f * filterIntensity,
                                            blendMode = BlendMode.ColorBurn
                                        )
                                    }
                                    "solarize" -> {
                                        drawRect(
                                            color = Color(0xFFCCCCCC),
                                            alpha = 0.45f * filterIntensity,
                                            blendMode = BlendMode.Exclusion
                                        )
                                    }
                                    "sunset" -> {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFF3D00), Color(0xFFFFD600))
                                            ),
                                            alpha = 0.3f * filterIntensity,
                                            blendMode = BlendMode.Overlay
                                        )
                                    }
                                    "emerald" -> {
                                        drawRect(
                                            color = Color(0xFF00C853),
                                            alpha = 0.25f * filterIntensity,
                                            blendMode = BlendMode.Color
                                        )
                                    }
                                    "aurora" -> {
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF00E676), Color(0xFFD500F9))
                                            ),
                                            alpha = 0.28f * filterIntensity,
                                            blendMode = BlendMode.Plus
                                        )
                                    }
                                }

                                // 2. Manual Color Grading Controls
                                if (brightnessVal > 0f) {
                                    drawRect(
                                        color = Color.White,
                                        alpha = brightnessVal * 0.45f,
                                        blendMode = BlendMode.Screen
                                    )
                                } else if (brightnessVal < 0f) {
                                    drawRect(
                                        color = Color.Black,
                                        alpha = -brightnessVal * 0.45f,
                                        blendMode = BlendMode.SrcOver
                                    )
                                }

                                if (contrastVal > 0f) {
                                    drawRect(
                                        color = Color(0xFF808080),
                                        alpha = contrastVal * 0.35f,
                                        blendMode = BlendMode.Overlay
                                    )
                                } else if (contrastVal < 0f) {
                                    drawRect(
                                        color = Color(0xFF808080),
                                        alpha = -contrastVal * 0.35f,
                                        blendMode = BlendMode.SrcOver
                                    )
                                }

                                if (saturationVal > 0f) {
                                    drawRect(
                                        color = Color.White,
                                        alpha = saturationVal * 0.15f,
                                        blendMode = BlendMode.ColorDodge
                                    )
                                    drawRect(
                                        color = Color.Yellow,
                                        alpha = saturationVal * 0.08f,
                                        blendMode = BlendMode.ColorDodge
                                    )
                                } else if (saturationVal < 0f) {
                                    drawRect(
                                        color = Color(0xFF808080),
                                        alpha = -saturationVal * 1.0f,
                                        blendMode = BlendMode.Color
                                    )
                                }

                                if (temperatureVal > 0f) {
                                    drawRect(
                                        color = Color(0xFFFF9100),
                                        alpha = temperatureVal * 0.3f,
                                        blendMode = BlendMode.Color
                                    )
                                } else if (temperatureVal < 0f) {
                                    drawRect(
                                        color = Color(0xFF00B0FF),
                                        alpha = -temperatureVal * 0.3f,
                                        blendMode = BlendMode.Color
                                    )
                                }

                                if (highlightsVal > 0f) {
                                    drawRect(
                                        color = Color.White,
                                        alpha = highlightsVal * 0.25f,
                                        blendMode = BlendMode.Screen
                                    )
                                } else if (highlightsVal < 0f) {
                                    drawRect(
                                        color = Color.Black,
                                        alpha = -highlightsVal * 0.25f,
                                        blendMode = BlendMode.Overlay
                                    )
                                }

                                if (shadowsVal > 0f) {
                                    drawRect(
                                        color = Color(0xFFD1D1D1),
                                        alpha = shadowsVal * 0.25f,
                                        blendMode = BlendMode.Softlight
                                    )
                                } else if (shadowsVal < 0f) {
                                    drawRect(
                                        color = Color(0xFF222222),
                                        alpha = -shadowsVal * 0.3f,
                                        blendMode = BlendMode.Multiply
                                    )
                                }

                                if (sharpnessVal > 0f) {
                                    drawRect(
                                        color = Color.White,
                                        alpha = sharpnessVal * 0.08f,
                                        blendMode = BlendMode.Difference
                                    )
                                    drawRect(
                                        color = Color.Black,
                                        alpha = sharpnessVal * 0.05f,
                                        blendMode = BlendMode.Overlay
                                    )
                                } else if (sharpnessVal < 0f) {
                                    drawRect(
                                        color = Color.Gray,
                                        alpha = -sharpnessVal * 0.12f,
                                        blendMode = BlendMode.Softlight
                                    )
                                }
                            }
                        }

                        // REAL-TIME VIDEO TRANSITIONS OVERLAY (Only if activeTool == "Merge")
                        if (activeTool == "Merge" && mergeClips.size > 1) {
                            var activeTransitionIdx: Int? = null
                            var transitionProgress = 0f
                            var fromClipRec: DuoRecording? = null
                            var toClipRec: DuoRecording? = null
                            var activeType = "none"

                            for (i in 0 until clipOffsets.size - 1) {
                                val transition = clipTransitions[i] ?: ClipTransition("none", 1000L)
                                if (transition.type != "none") {
                                    val joinTimeMs = clipOffsets[i].second + clipOffsets[i].third
                                    val transDur = transition.durationMs
                                    val halfDur = transDur / 2
                                    val startTransitionMs = joinTimeMs - halfDur
                                    val endTransitionMs = joinTimeMs + halfDur
                                    
                                    if (currentPlaybackPosition in startTransitionMs until endTransitionMs) {
                                        activeTransitionIdx = i
                                        transitionProgress = (currentPlaybackPosition - startTransitionMs).toFloat() / transDur
                                        fromClipRec = clipOffsets[i].first
                                        toClipRec = clipOffsets[i + 1].first
                                        activeType = transition.type
                                        break
                                    }
                                }
                            }

                            if (activeTransitionIdx != null && fromClipRec != null && toClipRec != null && activeType != "none") {
                                val fromThumb = mergeClipThumbnails[fromClipRec.uri.toString()]
                                val toThumb = mergeClipThumbnails[toClipRec.uri.toString()]
                                
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("transition_preview_overlay")
                                ) {
                                    val fromImageBitmap = fromThumb?.asImageBitmap()
                                    val toImageBitmap = toThumb?.asImageBitmap()
                                    
                                    when (activeType) {
                                        "crossfade" -> {
                                            if (transitionProgress < 0.5f) {
                                                toImageBitmap?.let {
                                                    drawImage(
                                                        image = it,
                                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                                                        alpha = transitionProgress * 2f
                                                    )
                                                }
                                            } else {
                                                fromImageBitmap?.let {
                                                    drawImage(
                                                        image = it,
                                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                                                        alpha = (1.0f - transitionProgress) * 2f
                                                    )
                                                }
                                            }
                                        }
                                        "fade_black" -> {
                                            val blackAlpha = if (transitionProgress < 0.5f) {
                                                transitionProgress * 2f
                                            } else {
                                                (1.0f - transitionProgress) * 2f
                                            }
                                            drawRect(
                                                color = Color.Black,
                                                alpha = blackAlpha
                                            )
                                        }
                                        "slide" -> {
                                            if (transitionProgress < 0.5f) {
                                                toImageBitmap?.let {
                                                    val offsetX = size.width * (1.0f - transitionProgress * 2f)
                                                    drawImage(
                                                        image = it,
                                                        dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), 0),
                                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                    )
                                                }
                                            } else {
                                                fromImageBitmap?.let {
                                                    val offsetX = -size.width * ((transitionProgress - 0.5f) * 2f)
                                                    drawImage(
                                                        image = it,
                                                        dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), 0),
                                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                    )
                                                }
                                            }
                                        }
                                        "slide_right" -> {
                                            if (transitionProgress < 0.5f) {
                                                toImageBitmap?.let {
                                                    val offsetX = -size.width * (1.0f - transitionProgress * 2f)
                                                    drawImage(
                                                        image = it,
                                                        dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), 0),
                                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                    )
                                                }
                                            } else {
                                                fromImageBitmap?.let {
                                                    val offsetX = size.width * ((transitionProgress - 0.5f) * 2f)
                                                    drawImage(
                                                        image = it,
                                                        dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), 0),
                                                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                    )
                                                }
                                            }
                                        }
                                        "zoom" -> {
                                            val scaleVal = if (transitionProgress < 0.5f) {
                                                0.1f + transitionProgress * 2f * 0.9f
                                            } else {
                                                1.0f + (transitionProgress - 0.5f) * 2f * 1.0f
                                            }
                                            val alpha = if (transitionProgress < 0.5f) {
                                                transitionProgress * 2f
                                            } else {
                                                (1.0f - transitionProgress) * 2f
                                            }
                                            
                                            withTransform({
                                                scale(scaleVal, scaleVal, pivot = center)
                                            }) {
                                                if (transitionProgress < 0.5f) {
                                                    toImageBitmap?.let {
                                                        drawImage(
                                                            image = it,
                                                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                                                            alpha = alpha
                                                        )
                                                    }
                                                } else {
                                                    fromImageBitmap?.let {
                                                        drawImage(
                                                            image = it,
                                                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                                                            alpha = alpha
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        "wipe" -> {
                                            if (transitionProgress < 0.5f) {
                                                val sweepX = size.width * (transitionProgress * 2f)
                                                clipRect(left = 0f, top = 0f, right = sweepX, bottom = size.height) {
                                                    toImageBitmap?.let {
                                                        drawImage(
                                                            image = it,
                                                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                        )
                                                    }
                                                }
                                                drawLine(
                                                    color = NeonGreen,
                                                    start = Offset(sweepX, 0f),
                                                    end = Offset(sweepX, size.height),
                                                    strokeWidth = 3.dp.toPx()
                                                )
                                            } else {
                                                val sweepX = size.width * ((transitionProgress - 0.5f) * 2f)
                                                clipRect(left = sweepX, top = 0f, right = size.width, bottom = size.height) {
                                                    fromImageBitmap?.let {
                                                        drawImage(
                                                            image = it,
                                                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                        )
                                                    }
                                                }
                                                drawLine(
                                                    color = NeonGreen,
                                                    start = Offset(sweepX, 0f),
                                                    end = Offset(sweepX, size.height),
                                                    strokeWidth = 3.dp.toPx()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ACTIVE TEXT OVERLAYS LAYER
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val parentWidthPx = constraints.maxWidth.toFloat()
                            val parentHeightPx = constraints.maxHeight.toFloat()
                            
                            val currentMs = currentPlaybackPosition
                            val currentPct = if (totalDurationMs > 0) currentMs.toFloat() / totalDurationMs else 0f
                            
                            textOverlays.forEach { overlay ->
                                if (currentPct in overlay.startPct..overlay.endPct) {
                                    val isSelected = selectedOverlayId == overlay.id
                                    val fontStyle = if (overlay.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                    val fontWeight = if (overlay.isBold) FontWeight.Bold else FontWeight.Normal
                                    
                                    val fontOptionsList = listOf(
                                        Triple("Sans-Serif", FontFamily.SansSerif, false),
                                        Triple("Serif", FontFamily.Serif, false),
                                        Triple("Monospace", FontFamily.Monospace, false),
                                        Triple("Cursive", FontFamily.Cursive, false),
                                        Triple("Default", FontFamily.Default, false),
                                        Triple("Elegant Italic", FontFamily.Serif, true),
                                        Triple("Modern Bold", FontFamily.SansSerif, false),
                                        Triple("Retro Fixed", FontFamily.Monospace, false),
                                        Triple("Stylish Cursive", FontFamily.Cursive, true),
                                        Triple("Custom Compact", FontFamily.Default, true)
                                    )
                                    val fontOption = fontOptionsList.getOrNull(overlay.fontIndex) ?: fontOptionsList[0]
                                    val textAlignment = when (overlay.alignment) {
                                        0 -> androidx.compose.ui.text.style.TextAlign.Left
                                        2 -> androidx.compose.ui.text.style.TextAlign.Right
                                        else -> androidx.compose.ui.text.style.TextAlign.Center
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset(
                                                x = (overlay.xPct * parentWidthPx / LocalDensity.current.density).dp - 40.dp,
                                                y = (overlay.yPct * parentHeightPx / LocalDensity.current.density).dp - 15.dp
                                            )
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (overlay.bgOpacity > 0f) {
                                                    overlay.bgColor.copy(alpha = overlay.bgOpacity)
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = if (isSelected) NeonGreen else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .pointerInput(overlay.id, parentWidthPx, parentHeightPx) {
                                                detectDragGestures(
                                                    onDragStart = { isScrubbing = true },
                                                    onDragEnd = { isScrubbing = false },
                                                    onDragCancel = { isScrubbing = false }
                                                ) { change, dragAmount ->
                                                    val currentX = overlay.xPct * parentWidthPx
                                                    val currentY = overlay.yPct * parentHeightPx
                                                    val targetXpct = ((currentX + dragAmount.x) / parentWidthPx).coerceIn(0.01f, 0.99f)
                                                    val targetYpct = ((currentY + dragAmount.y) / parentHeightPx).coerceIn(0.01f, 0.99f)
                                                    
                                                    val index = textOverlays.indexOfFirst { it.id == overlay.id }
                                                    if (index != -1) {
                                                        textOverlays[index] = textOverlays[index].copy(
                                                            xPct = targetXpct,
                                                            yPct = targetYpct
                                                        )
                                                    }
                                                    change.consume()
                                                }
                                            }
                                            .clickable {
                                                selectedOverlayId = overlay.id
                                                activeTool = "Text"
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("text_overlay_preview_${overlay.id}")
                                    ) {
                                        val textShadow = if (overlay.hasShadow) {
                                            Shadow(
                                                color = Color.Black.copy(alpha = 0.9f),
                                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                                blurRadius = 4f
                                            )
                                        } else null

                                        Text(
                                            text = overlay.text,
                                            color = overlay.color,
                                            fontSize = overlay.sizeSp.sp,
                                            fontFamily = fontOption.second,
                                            fontWeight = fontWeight,
                                            fontStyle = fontStyle,
                                            textAlign = textAlignment,
                                            style = TextStyle(
                                                shadow = textShadow
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Play/Pause Floating controller overlay
                        if (activeTool != "Text") {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), CircleShape)
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
                                    }
                                    .testTag("editor_play_pause_overlay_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlayingState) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/pause screen video",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Audio indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = TextWhite, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dual Stream Audio", color = TextWhite, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Before/After comparison button
                        if (activeTool == "Filters") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.82f))
                                    .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isHoldingBeforeByTap = true
                                                tryAwaitRelease()
                                                isHoldingBeforeByTap = false
                                            }
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("before_after_hold_overlay_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = "Hold for original",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "HOLD TO COMPARE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // TIME AND COUNTER OVERLAYS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Playhead Position / Total Time formatted
                        val currentForm = if (activeTool == "Speed") {
                            formatCentiseconds(getStretchedTimeForOriginalTime(currentPlaybackPosition))
                        } else {
                            formatCentiseconds(currentPlaybackPosition)
                        }
                        
                        val totalForm = if (activeTool == "Speed") {
                            formatCentiseconds(totalStretchedDurationMs)
                        } else if (activeTool == "Merge") {
                            formatCentiseconds(totalMergeDurationMs)
                        } else {
                            formatCentiseconds(totalDurationMs)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$currentForm / $totalForm",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = TextWhite
                            )
                        }

                        // Codec status block
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1F10B981))
                                .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RAW STREAM IN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = NeonGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // CORE HIGH-FIDELITY SCROLLABLE/SCRUBBABLE TIMELINE BAR
                    Text(
                        text = "MULTI-CAM TIMELINE DECK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.0.sp
                        ),
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )

                    var timelineWidth by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, BorderGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .onGloballyPositioned { timelineWidth = it.size.width.toFloat() }
                            .pointerInput(recording.uri, totalDurationMs, activeTool, mergeClips.size, totalStretchedDurationMs) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: continue
                                        if (change.pressed) {
                                            isScrubbing = true
                                            videoViewInstance?.let { view ->
                                                if (view.isPlaying) {
                                                    view.pause()
                                                    isPlayingState = false
                                                }
                                            }
                                            if (timelineWidth > 0f) {
                                                val pct = (change.position.x / timelineWidth).coerceIn(0f, 1f)
                                                if (activeTool == "Speed") {
                                                    val targetStretchedMs = (pct * totalStretchedDurationMs).toLong()
                                                    val targetOriginalMs = getOriginalTimeForStretchedTime(targetStretchedMs)
                                                    throttledSeek(targetOriginalMs)
                                                } else if (activeTool == "Merge") {
                                                    val targetMs = (pct * totalMergeDurationMs).toLong()
                                                    val (clipIdx, relPosMs) = getClipAndPositionForGlobalTime(targetMs)
                                                    throttledSeekMerge(clipIdx, relPosMs, targetMs)
                                                } else {
                                                    val targetMs = (pct * totalDurationMs).toLong()
                                                    throttledSeek(targetMs)
                                                }
                                            }
                                            change.consume()
                                        } else {
                                            if (isScrubbing) {
                                                isScrubbing = false
                                                videoViewInstance?.let { view ->
                                                    if (activeTool == "Speed") {
                                                        view.seekTo(currentPlaybackPosition.toInt())
                                                    } else if (activeTool == "Merge") {
                                                        val (clipIdx, relPosMs) = getClipAndPositionForGlobalTime(currentPlaybackPosition)
                                                        if (currentPlayingIndex.value != clipIdx) {
                                                            currentPlayingIndex.value = clipIdx
                                                            view.setVideoURI(mergeClips[clipIdx].uri)
                                                        } else {
                                                            view.seekTo(relPosMs.toInt())
                                                        }
                                                    } else {
                                                        view.seekTo(currentPlaybackPosition.toInt())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("editor_timeline_track")
                    ) {
                        // Strip of sequential film frames
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (framesLoading) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Parsing frames...", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else if (thumbnails.isEmpty()) {
                                // Dynamic dark filmstrips placeholder standard fallback
                                for (i in 0 until 8) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (i % 2 == 0) Color(0xFF18181B) else Color(0xFF09090B))
                                            .border(0.5.dp, BorderGray.copy(alpha = 0.2f))
                                    )
                                }
                            } else {
                                thumbnails.forEach { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Timeline Frame",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 0.5.dp)
                                    )
                                }
                            }
                        }

                        // If speed tool is active, overlay beautiful translucent speed-stretched blocks directly representing timeline scaling!
                        if (activeTool == "Speed") {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                stretchedSections.forEach { sSec ->
                                    val weight = (sSec.stretchedEndMs - sSec.stretchedStartMs).toFloat() / totalStretchedDurationMs
                                    if (weight > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(weight)
                                                .fillMaxHeight()
                                                .border(0.5.dp, BorderGray.copy(alpha = 0.2f))
                                                .background(
                                                    when (sSec.speed) {
                                                        1.0f -> Color.Transparent
                                                        0.25f -> Color(0xFF0D9488).copy(alpha = 0.45f)  // Teal slow
                                                        0.5f -> Color(0xFF0284C7).copy(alpha = 0.45f)   // Sky slow
                                                        1.5f -> Color(0xFFF59E0B).copy(alpha = 0.45f)  // Amber fast
                                                        2.0f -> Color(0xFFEA580C).copy(alpha = 0.45f)  // Orange double
                                                        4.0f -> Color(0xFFDC2626).copy(alpha = 0.45f)  // Red quad
                                                        else -> Color.DarkGray.copy(alpha = 0.45f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "${sSec.speed}x",
                                                            color = if (sSec.speed == 1f) Color.White else NeonGreen,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        if (sSec.muteAudio) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(
                                                                imageVector = Icons.Rounded.VolumeOff,
                                                                contentDescription = "Muted",
                                                                tint = GlowRecordRed,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // If speed tool is active, draw draggable selection handles over the timeline!
                        if (activeTool == "Speed" && speedAdjustMode == "section") {
                            val startX = speedStartPct * timelineWidth
                            val endX = speedEndPct * timelineWidth
                            
                            // Left dimmed box
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(speedStartPct)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .align(Alignment.CenterStart)
                            )
                            
                            // Right dimmed box
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(1f - speedEndPct)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .align(Alignment.CenterEnd)
                            )

                            // Highlight border for selected window
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(speedEndPct - speedStartPct)
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer { translationX = startX }
                                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(2.dp))
                            )

                            // Left Draggable Selection Handle
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer { translationX = startX - 14.dp.toPx() }
                                    .pointerInput(timelineWidth, totalDurationMs) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isScrubbing = true 
                                                videoViewInstance?.let { view ->
                                                    if (view.isPlaying) {
                                                        view.pause()
                                                        isPlayingState = false
                                                    }
                                                }
                                            },
                                            onDragEnd = { isScrubbing = false },
                                            onDragCancel = { isScrubbing = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (timelineWidth > 0f) {
                                                    val newStartX = (speedStartPct * timelineWidth + dragAmount.x).coerceIn(0f, speedEndPct * timelineWidth - 20f)
                                                    speedStartPct = newStartX / timelineWidth
                                                    
                                                    // Seek video to let user see frame
                                                    val targetMs = (speedStartPct * totalDurationMs).toLong()
                                                    throttledSeek(targetMs)
                                                }
                                            }
                                        )
                                    }
                                    .testTag("speed_start_handle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                )
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            
                            // Right Draggable Selection Handle
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer { translationX = endX - 14.dp.toPx() }
                                    .pointerInput(timelineWidth, totalDurationMs) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isScrubbing = true 
                                                videoViewInstance?.let { view ->
                                                    if (view.isPlaying) {
                                                        view.pause()
                                                        isPlayingState = false
                                                    }
                                                }
                                            },
                                            onDragEnd = { isScrubbing = false },
                                            onDragCancel = { isScrubbing = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (timelineWidth > 0f) {
                                                    val newEndX = (speedEndPct * timelineWidth + dragAmount.x).coerceIn(speedStartPct * timelineWidth + 20f, timelineWidth)
                                                    speedEndPct = newEndX / timelineWidth
                                                    
                                                    // Seek video to let user see frame
                                                    val targetMs = (speedEndPct * totalDurationMs).toLong()
                                                    throttledSeek(targetMs)
                                                }
                                            }
                                        )
                                    }
                                    .testTag("speed_end_handle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                )
                                Icon(
                                    Icons.Rounded.ChevronLeft,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Shaded trim range if user activates trimmer
                        if (activeTool == "Trim" && isTrimRangeActive) {
                            val startX = trimStartPct * timelineWidth
                            val endX = trimEndPct * timelineWidth
                            
                            // Left dimmed box
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(trimStartPct)
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .align(Alignment.CenterStart)
                            )
                            
                            // Right dimmed box
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(1f - trimEndPct)
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .align(Alignment.CenterEnd)
                            )

                            // Highlight border for trimmed window
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(trimEndPct - trimStartPct)
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer { translationX = startX }
                                    .border(2.dp, NeonGreen, RoundedCornerShape(2.dp))
                            )

                            // Left Draggable Trim Handle
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer { translationX = startX - 14.dp.toPx() }
                                    .pointerInput(timelineWidth, totalDurationMs) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isScrubbing = true 
                                                activeHandleForFineTune = "start"
                                                videoViewInstance?.let { view ->
                                                    if (view.isPlaying) {
                                                        view.pause()
                                                        isPlayingState = false
                                                    }
                                                }
                                            },
                                            onDragEnd = { isScrubbing = false },
                                            onDragCancel = { isScrubbing = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (timelineWidth > 0f) {
                                                    val newStartX = (trimStartPct * timelineWidth + dragAmount.x).coerceIn(0f, trimEndPct * timelineWidth - 20f)
                                                    trimStartPct = newStartX / timelineWidth
                                                    
                                                    // Seek video to let user see frame
                                                    val targetMs = (trimStartPct * totalDurationMs).toLong()
                                                    throttledSeek(targetMs)
                                                }
                                            }
                                        )
                                    }
                                    .testTag("trim_start_handle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .fillMaxHeight()
                                        .background(NeonGreen, RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                )
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            
                            // Right Draggable Trim Handle
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(28.dp)
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer { translationX = endX - 14.dp.toPx() }
                                    .pointerInput(timelineWidth, totalDurationMs) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isScrubbing = true 
                                                activeHandleForFineTune = "end"
                                                videoViewInstance?.let { view ->
                                                    if (view.isPlaying) {
                                                        view.pause()
                                                        isPlayingState = false
                                                    }
                                                }
                                            },
                                            onDragEnd = { isScrubbing = false },
                                            onDragCancel = { isScrubbing = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (timelineWidth > 0f) {
                                                    val newEndX = (trimEndPct * timelineWidth + dragAmount.x).coerceIn(trimStartPct * timelineWidth + 20f, timelineWidth)
                                                    trimEndPct = newEndX / timelineWidth
                                                    
                                                    // Seek video to let user see frame
                                                    val targetMs = (trimEndPct * totalDurationMs).toLong()
                                                    throttledSeek(targetMs)
                                                }
                                            }
                                        )
                                    }
                                    .testTag("trim_end_handle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .fillMaxHeight()
                                        .background(NeonGreen, RoundedCornerShape(4.dp))
                                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                )
                                Icon(
                                    Icons.Rounded.ChevronLeft,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            // Start Timestamp Label floating above left handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .graphicsLayer { translationX = (startX - 30.dp.toPx()).coerceAtLeast(0f) }
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatCentiseconds((trimStartPct * totalDurationMs).toLong()),
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // End Timestamp Label floating above right handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .graphicsLayer { translationX = (endX - 30.dp.toPx()).coerceAtMost(timelineWidth - 60.dp.toPx()) }
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatCentiseconds((trimEndPct * totalDurationMs).toLong()),
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // CLEARLY VISIBLE CO-ALIGNED VERTICAL PLAYHEAD LINE (Needle)
                        val playheadProgress = if (activeTool == "Speed") {
                            if (totalStretchedDurationMs > 0) {
                                val currentStretched = getStretchedTimeForOriginalTime(currentPlaybackPosition)
                                currentStretched.toFloat() / totalStretchedDurationMs
                            } else 0f
                        } else if (activeTool == "Merge") {
                            if (totalMergeDurationMs > 0) currentPlaybackPosition.toFloat() / totalMergeDurationMs else 0f
                        } else {
                            if (totalDurationMs > 0) currentPlaybackPosition.toFloat() / totalDurationMs else 0f
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .align(Alignment.CenterStart)
                                .graphicsLayer {
                                    translationX = (playheadProgress * timelineWidth) - 1.5f
                                }
                                .background(NeonGreen)
                                .shadow(elevation = 6.dp, shape = RoundedCornerShape(1.dp))
                                .testTag("timeline_playhead_line")
                        ) {
                            // Rounded handle on top of needle
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopCenter)
                                    .background(NeonGreen, CircleShape)
                            )
                        }

                        // TRANSITIONS TIMELINE DIAMOND MARKERS
                        if (activeTool == "Merge" && mergeClips.size > 1) {
                            clipOffsets.forEachIndexed { i, clipOffset ->
                                if (i < clipOffsets.size - 1) {
                                    val joinTimeMs = clipOffset.second + clipOffset.third
                                    val frac = if (totalMergeDurationMs > 0) joinTimeMs.toFloat() / totalMergeDurationMs else 0f
                                    val transition = clipTransitions[i] ?: ClipTransition("none", 1000L)
                                    val isTransitionActive = transition.type != "none"
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(36.dp)
                                            .align(Alignment.CenterStart)
                                            .graphicsLayer {
                                                translationX = (frac * timelineWidth) - 18.dp.toPx()
                                            }
                                            .pointerInput(timelineWidth) {
                                                detectTapGestures {
                                                    editingTransitionIndex = i
                                                }
                                            }
                                            .testTag("transition_timeline_diamond_$i"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .graphicsLayer { rotationZ = 45f }
                                                .background(
                                                    if (isTransitionActive) NeonGreen else Color.DarkGray,
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isTransitionActive) Color.Black else Color.White.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(3.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier.graphicsLayer { rotationZ = -45f }
                                            ) {
                                                Icon(
                                                    imageVector = if (isTransitionActive) {
                                                        when (transition.type) {
                                                            "crossfade" -> Icons.Rounded.BlurOn
                                                            "fade_black" -> Icons.Rounded.Brightness4
                                                            "slide" -> Icons.Rounded.ArrowForward
                                                            "slide_right" -> Icons.Rounded.ArrowBack
                                                            "zoom" -> Icons.Rounded.ZoomIn
                                                            "wipe" -> Icons.Rounded.Compare
                                                            else -> Icons.Rounded.FlashOn
                                                        }
                                                    } else {
                                                        Icons.Rounded.Add
                                                    },
                                                    contentDescription = "Transition icon",
                                                    tint = if (isTransitionActive) Color.Black else Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeline timestamp rules indicator at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val durationToUse = when (activeTool) {
                            "Speed" -> totalStretchedDurationMs
                            "Merge" -> totalMergeDurationMs
                            else -> totalDurationMs
                        }
                        Text("0.00s", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(formatSeconds(durationToUse / 2), color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(formatSeconds(durationToUse), color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (selectedMusicUri != null || selectedMusicMood != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "BACKGROUND SOUNDTRACK LAYER (DRAG TO OFFSET START TIME)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .testTag("music_track_container")
                            ) {
                                val offsetPct = if (totalDurationMs > 0) musicOffsetMs.toFloat() / totalDurationMs else 0f
                                
                                val totalOfMusicSegment = if (selectedMusicMood != null) 120000L else {
                                    try { localMusicPlayer?.duration?.toLong() ?: 120000L } catch (e: Exception) { 120000L }
                                }
                                val trimStartMs = (musicTrimStartPct * totalOfMusicSegment).toLong()
                                val trimEndMs = (musicTrimEndPct * totalOfMusicSegment).toLong()
                                val trimmedDurationMs = if (trimEndMs > trimStartMs) trimEndMs - trimStartMs else totalOfMusicSegment
                                
                                val activeDurationMs = if (loopMusic) {
                                    totalDurationMs - musicOffsetMs
                                } else {
                                    trimmedDurationMs.coerceAtMost(totalDurationMs - musicOffsetMs)
                                }
                                val widthPct = if (totalDurationMs > 0) activeDurationMs.toFloat() / totalDurationMs else 0.5f
                                
                                if (widthPct > 0f) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(widthPct)
                                            .graphicsLayer {
                                                translationX = offsetPct * timelineWidth
                                            }
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                                                )
                                            )
                                            .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(4.dp))
                                            .pointerInput(timelineWidth, totalDurationMs) {
                                                detectDragGestures(
                                                    onDragStart = { isScrubbing = true },
                                                    onDragEnd = { isScrubbing = false },
                                                    onDragCancel = { isScrubbing = false }
                                                ) { change, dragAmount ->
                                                    if (timelineWidth > 0f) {
                                                        val deltaMs = ((dragAmount.x / timelineWidth) * totalDurationMs).toLong()
                                                        musicOffsetMs = (musicOffsetMs + deltaMs).coerceIn(0L, totalDurationMs - 1000L)
                                                        syncBackgroundMusic(currentPlaybackPosition, isPlayingState)
                                                    }
                                                    change.consume()
                                                }
                                            }
                                            .padding(horizontal = 8.dp)
                                            .testTag("music_timeline_track"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = "Music Playing",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedMusicName,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Offset: ${formatSeconds(musicOffsetMs)}",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (textOverlays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "TEXT & SUBTITLE OVERLAY TIMELINES (CLICK RED BAR TO CHOOSE)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            textOverlays.forEachIndexed { index, overlay ->
                                val isSelected = selectedOverlayId == overlay.id
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "T${index + 1}",
                                        color = if (isSelected) NeonGreen else TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(1.dp, if (isSelected) NeonGreen.copy(alpha = 0.6f) else BorderGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    ) {
                                        val startPct = overlay.startPct
                                        val endPct = overlay.endPct
                                        val durationPct = (endPct - startPct).coerceIn(0.01f, 1f)
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(durationPct)
                                                .graphicsLayer {
                                                    translationX = startPct * timelineWidth
                                                }
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            overlay.color.copy(alpha = 0.8f),
                                                            overlay.color.copy(alpha = 0.5f)
                                                        )
                                                    )
                                                )
                                                .clickable {
                                                    selectedOverlayId = overlay.id
                                                    activeTool = "Text"
                                                }
                                                .padding(horizontal = 6.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = overlay.text,
                                                color = if (overlay.color == Color.Black) Color.White else Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PROFESSIONAL TACTILE EDITING SUITE CONTROLS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepCharcoal)
                        .padding(bottom = 16.dp, top = 12.dp)
                ) {
                    // Main Playback Controller Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                currentPlaybackPosition = 0L
                                videoViewInstance?.seekTo(0)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Back to start", tint = TextWhite)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
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
                                }
                                .testTag("editor_core_play_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlayingState) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Core Play/pause timer",
                                tint = GlowRecordRed,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                val jump = (currentPlaybackPosition + 2000).coerceAtMost(totalDurationMs)
                                currentPlaybackPosition = jump
                                videoViewInstance?.seekTo(jump.toInt())
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.Forward30, contentDescription = "Forward 30s", tint = TextWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderGray.copy(alpha = 0.2f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeTool == "Merge") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VIDEO MERGE SEQUENCE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = TextMuted
                                    )
                                    
                                    Button(
                                        onClick = { showMergeSelectDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("add_clip_to_merge_btn")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ADD CLIP", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color.Black)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("TOTAL TIMELINE", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        Text(formatCentiseconds(totalMergeDurationMs), color = Color.White, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("EST. FILE SIZE", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        Text(formatMergedSize(estimatedFileSizeBytes), color = NeonGreen, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                                    }
                                }
                                
                                if (mismatchWarnings.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GlowRecordRed.copy(alpha = 0.15f))
                                            .border(0.5.dp, GlowRecordRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        mismatchWarnings.forEach { warning ->
                                            Row(verticalAlignment = Alignment.Top) {
                                                Text("⚠️ ", color = GlowRecordRed, fontSize = 10.sp)
                                                Text(warning, color = TextWhite.copy(alpha = 0.9f), fontSize = 10.sp, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    mergeClips.forEachIndexed { idx, clip ->
                                        val clipName = VideoMetadataManager.getDisplayName(context, clip)
                                        val clipDur = VideoMetadataManager.getEffectiveDuration(context, clip)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (currentPlayingIndex.value == idx) NeonGreen else BorderGray.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DragHandle,
                                                contentDescription = "Reorder handle",
                                                tint = TextMuted,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .pointerInput(Unit) {
                                                        detectDragGestures(
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                if (dragAmount.y < -15f && idx > 0) {
                                                                    val item = mergeClips.removeAt(idx)
                                                                    mergeClips.add(idx - 1, item)
                                                                } else if (dragAmount.y > 15f && idx < mergeClips.size - 1) {
                                                                    val item = mergeClips.removeAt(idx)
                                                                    mergeClips.add(idx + 1, item)
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .testTag("clip_drag_handle_${idx}")
                                            )
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp, 32.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.DarkGray),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (idx == 0 && thumbnails.isNotEmpty()) {
                                                    Image(
                                                        bitmap = thumbnails[0].asImageBitmap(),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .background(Color.Black.copy(alpha = 0.7f))
                                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                                ) {
                                                    Text("#${idx + 1}", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = clipName,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Duration: ${formatSeconds(clipDur)}",
                                                    color = NeonGreen,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                )
                                             }
                                             
                                             Row(verticalAlignment = Alignment.CenterVertically) {
                                                 IconButton(
                                                     onClick = {
                                                         if (idx > 0) {
                                                             val item = mergeClips.removeAt(idx)
                                                             mergeClips.add(idx - 1, item)
                                                         }
                                                     },
                                                     enabled = idx > 0,
                                                     modifier = Modifier.size(24.dp).testTag("clip_move_up_${idx}")
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Rounded.KeyboardArrowUp,
                                                         contentDescription = "Move Up",
                                                         tint = if (idx > 0) TextWhite else TextMuted.copy(alpha = 0.3f),
                                                         modifier = Modifier.size(20.dp)
                                                     )
                                                 }
                                                 
                                                 IconButton(
                                                     onClick = {
                                                         if (idx < mergeClips.size - 1) {
                                                             val item = mergeClips.removeAt(idx)
                                                             mergeClips.add(idx + 1, item)
                                                         }
                                                     },
                                                     enabled = idx < mergeClips.size - 1,
                                                     modifier = Modifier.size(24.dp).testTag("clip_move_down_${idx}")
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Rounded.KeyboardArrowDown,
                                                         contentDescription = "Move Down",
                                                         tint = if (idx < mergeClips.size - 1) TextWhite else TextMuted.copy(alpha = 0.3f),
                                                         modifier = Modifier.size(20.dp)
                                                     )
                                                 }
                                             }
                                             
                                             Spacer(modifier = Modifier.width(4.dp))
                                             
                                             IconButton(
                                                 onClick = {
                                                     if (mergeClips.size > 1) {
                                                         mergeClips.removeAt(idx)
                                                         if (currentPlayingIndex.value >= mergeClips.size) {
                                                             currentPlayingIndex.value = mergeClips.lastIndex
                                                         }
                                                     } else {
                                                         Toast.makeText(context, "Cannot remove the only clip in sequence", Toast.LENGTH_SHORT).show()
                                                     }
                                                 },
                                                 modifier = Modifier.size(32.dp).testTag("clip_remove_btn_${idx}")
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Rounded.Delete,
                                                     contentDescription = "Remove Clip",
                                                     tint = GlowRecordRed,
                                                     modifier = Modifier.size(18.dp)
                                                 )
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                         Spacer(modifier = Modifier.height(10.dp))
                     }

                    if (activeTool == "Trim" && isTrimRangeActive) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "FRAME PRECISION TUNING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Selector
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (activeHandleForFineTune == "start") NeonGreen else Color.Transparent)
                                                .clickable { activeHandleForFineTune = "start" }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "START MARK",
                                                color = if (activeHandleForFineTune == "start") Color.Black else TextWhite,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (activeHandleForFineTune == "end") NeonGreen else Color.Transparent)
                                                .clickable { activeHandleForFineTune = "end" }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "END MARK",
                                                color = if (activeHandleForFineTune == "end") Color.Black else TextWhite,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    // Fine-tune Arrow Buttons
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val frameMs = 33L // approx 1 frame at 30fps
                                                if (activeHandleForFineTune == "start") {
                                                    val newStart = ((trimStartPct * totalDurationMs.toFloat()).toLong() - frameMs).coerceAtLeast(0L)
                                                    trimStartPct = newStart.toFloat() / totalDurationMs.toFloat()
                                                    throttledSeek(newStart)
                                                } else {
                                                    val startMs = (trimStartPct * totalDurationMs.toFloat()).toLong()
                                                    val newEnd = ((trimEndPct * totalDurationMs.toFloat()).toLong() - frameMs).coerceAtLeast(startMs + 100L)
                                                    trimEndPct = newEnd.toFloat() / totalDurationMs.toFloat()
                                                    throttledSeek(newEnd)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .border(1.dp, BorderGray.copy(alpha = 0.5f), CircleShape)
                                                .testTag("fine_tune_back")
                                        ) {
                                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "-1 Frame", tint = NeonGreen)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "1 FRAME",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = {
                                                val frameMs = 33L // approx 1 frame at 30fps
                                                if (activeHandleForFineTune == "start") {
                                                    val endMs = (trimEndPct * totalDurationMs.toFloat()).toLong()
                                                    val newStart = ((trimStartPct * totalDurationMs.toFloat()).toLong() + frameMs).coerceAtMost(endMs - 100L)
                                                    trimStartPct = newStart.toFloat() / totalDurationMs.toFloat()
                                                    throttledSeek(newStart)
                                                } else {
                                                    val newEnd = ((trimEndPct * totalDurationMs.toFloat()).toLong() + frameMs).coerceAtMost(totalDurationMs)
                                                    trimEndPct = newEnd.toFloat() / totalDurationMs.toFloat()
                                                    throttledSeek(newEnd)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .border(1.dp, BorderGray.copy(alpha = 0.5f), CircleShape)
                                                .testTag("fine_tune_forward")
                                        ) {
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = "+1 Frame", tint = NeonGreen)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (activeTool == "Audio") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("audio_mixer_workspace")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "AUDIO MIXER & SOUND STUDIO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Dynamic background music status
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = "Selected Track",
                                        tint = if (selectedMusicUri != null || selectedMusicMood != null) Color(0xFF818CF8) else TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedMusicName,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (selectedMusicMood != null) "Mood Track: $selectedMusicMood" else if (selectedMusicUri != null) "Local Audio Track" else "No background soundtrack active",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (selectedMusicUri != null || selectedMusicMood != null) {
                                        IconButton(
                                            onClick = {
                                                selectedMusicUri = null
                                                selectedMusicName = "No Background Music"
                                                selectedMusicMood = null
                                                synthSynthesizer.stop()
                                                localMusicPlayer?.let {
                                                    try {
                                                        it.release()
                                                    } catch (e: Exception) {}
                                                }
                                                localMusicPlayer = null
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Remove music", tint = GlowRecordRed)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Two high-contrast volume sliders
                                Text(
                                    text = "VOLUME MIXER CONTROL",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Slider 1: Original Audio Volume
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Original Video Audio", color = TextWhite, fontSize = 11.sp)
                                        Text(
                                            text = "${(originalAudioVolume * 100).toInt()}%",
                                            color = NeonGreen,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = originalAudioVolume,
                                        onValueChange = {
                                            originalAudioVolume = it
                                            mediaPlayerInstance?.let { mp ->
                                                try { mp.setVolume(it, it) } catch (ignored: Exception) {}
                                            }
                                        },
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = NeonGreen,
                                            inactiveTrackColor = Color.DarkGray,
                                            thumbColor = NeonGreen
                                        ),
                                        modifier = Modifier.testTag("original_audio_volume_slider")
                                    )
                                }

                                // Slider 2: Music Volume
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Background soundtrack", color = TextWhite, fontSize = 11.sp)
                                        Text(
                                            text = "${(musicVolume * 100).toInt()}%",
                                            color = Color(0xFF818CF8),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = musicVolume,
                                        onValueChange = {
                                            musicVolume = it
                                            syncBackgroundMusic(currentPlaybackPosition, isPlayingState)
                                        },
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = Color(0xFF818CF8),
                                            inactiveTrackColor = Color.DarkGray,
                                            thumbColor = Color(0xFF818CF8)
                                        ),
                                        modifier = Modifier.testTag("background_music_volume_slider")
                                    )
                                }

                                // MOOD SELECTOR FOR BUILT-IN MUSIC TRACKS
                                Text(
                                    text = "BUILT-IN SOUNDTRACKS BY MOOD",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val trackMoods = listOf(
                                    QuadMood("Upbeat/Energetic", listOf("Neon Horizon", "Electro Pulse"), Icons.Rounded.FlashOn, Color(0xFFFFD700)),
                                    QuadMood("Chill/Relaxed", listOf("Lo-Fi Rain", "Coffee & Sunset"), Icons.Rounded.Opacity, Color(0xFF00E5FF)),
                                    QuadMood("Cinematic/Epic", listOf("Titan Ascent", "Valiant Heart"), Icons.Rounded.Movie, Color(0xFFF43F5E)),
                                    QuadMood("Funny/Quirky", listOf("Silly Banjo", "Funky Duck"), Icons.Rounded.SentimentVerySatisfied, Color(0xFF34D399))
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    trackMoods.forEach { mood ->
                                        val isMoodActive = selectedMusicMood == mood.name
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isMoodActive) mood.accentColor.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.4f))
                                                .border(
                                                    1.dp,
                                                    if (isMoodActive) mood.accentColor else Color.DarkGray.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    selectedMusicMood = mood.name
                                                    selectedMusicUri = null
                                                    selectedMusicName = mood.tracks.first()
                                                    synthSynthesizer.stop()
                                                    localMusicPlayer?.let {
                                                        try { it.release() } catch (e: Exception) {}
                                                    }
                                                    localMusicPlayer = null
                                                    syncBackgroundMusic(currentPlaybackPosition, isPlayingState)
                                                }
                                                .padding(10.dp)
                                                .testTag("mood_music_card_${mood.name.replace("/", "_")}")
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
                                                Icon(mood.icon, contentDescription = mood.name, tint = mood.accentColor, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(mood.name.split("/").first(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(mood.name.split("/").last(), color = TextMuted, fontSize = 9.sp)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = mood.tracks.first(),
                                                    color = mood.accentColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Browse custom local music from device
                                Button(
                                    onClick = { audioLauncher.launch("audio/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("import_custom_audio_btn")
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Open file", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("BROWSE MUSIC FROM DEVICE LIBRARY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // TRACK TRIMMER & PLAYBACK SETTINGS
                                Text(
                                    text = "SOUNDTRACK PLAYBACK & TRIMMER",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Trim parameters
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Trim Start Slider
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Trim Start: ${(musicTrimStartPct * 100).toInt()}%",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                        Slider(
                                            value = musicTrimStartPct,
                                            onValueChange = {
                                                if (it < musicTrimEndPct - 0.05f) {
                                                    musicTrimStartPct = it
                                                    syncBackgroundMusic(currentPlaybackPosition, isPlayingState)
                                                }
                                            },
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = Color(0xFF818CF8),
                                                inactiveTrackColor = Color.DarkGray,
                                                thumbColor = Color(0xFF818CF8)
                                            ),
                                            modifier = Modifier.testTag("music_trim_start_slider")
                                        )
                                    }

                                    // Trim End Slider
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Trim End: ${(musicTrimEndPct * 100).toInt()}%",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                        Slider(
                                            value = musicTrimEndPct,
                                            onValueChange = {
                                                if (it > musicTrimStartPct + 0.05f) {
                                                    musicTrimEndPct = it
                                                    syncBackgroundMusic(currentPlaybackPosition, isPlayingState)
                                                }
                                            },
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = Color(0xFF818CF8),
                                                inactiveTrackColor = Color.DarkGray,
                                                thumbColor = Color(0xFF818CF8)
                                            ),
                                            modifier = Modifier.testTag("music_trim_end_slider")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Loop track & Fade parameters row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { loopMusic = !loopMusic }
                                    ) {
                                        Checkbox(
                                            checked = loopMusic,
                                            onCheckedChange = { loopMusic = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF818CF8),
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("music_loop_checkbox")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Auto Loop if shorter", color = TextWhite, fontSize = 11.sp)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { musicFadeIn = !musicFadeIn }
                                    ) {
                                        Checkbox(
                                            checked = musicFadeIn,
                                            onCheckedChange = { musicFadeIn = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF818CF8),
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("music_fade_in_checkbox")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fade-In (2s)", color = TextWhite, fontSize = 11.sp)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { musicFadeOut = !musicFadeOut }
                                    ) {
                                        Checkbox(
                                            checked = musicFadeOut,
                                            onCheckedChange = { musicFadeOut = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF818CF8),
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("music_fade_out_checkbox")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fade-Out (2s)", color = TextWhite, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (activeTool == "Text") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("text_editor_workspace")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TEXT STUDIO & SUBTITLE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = TextMuted
                                    )
                                    Button(
                                        onClick = {
                                            val currentMs = currentPlaybackPosition
                                            val startPct = (currentMs.toFloat() / totalDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1.0f)
                                            val endPct = (startPct + 0.3f).coerceIn(0f, 1.0f)
                                            val newOverlay = TextOverlay(
                                                text = "New Title Block",
                                                startPct = startPct,
                                                endPct = endPct
                                            )
                                            textOverlays.add(newOverlay)
                                            selectedOverlayId = newOverlay.id
                                            Toast.makeText(context, "Added text overlay!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp).testTag("add_text_button")
                                    ) {
                                        Text("+ ADD TEXT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val activeOverlay = textOverlays.find { it.id == selectedOverlayId }
                                if (activeOverlay != null) {
                                    Text("EDIT CONTENT", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = activeOverlay.text,
                                        onValueChange = { newVal ->
                                            val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                            if (idx != -1) {
                                                textOverlays[idx] = textOverlays[idx].copy(text = newVal)
                                            }
                                        },
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("text_input_field"),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    textOverlays.remove(activeOverlay)
                                                    selectedOverlayId = textOverlays.lastOrNull()?.id
                                                },
                                                modifier = Modifier.testTag("delete_text_overlay_btn")
                                            ) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete Text", tint = GlowRecordRed)
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("FONT FAMILY CHOICE (10 OPTIONS)", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val fontOptionsList = listOf(
                                            Triple("Sans-Serif", FontFamily.SansSerif, false),
                                            Triple("Serif", FontFamily.Serif, false),
                                            Triple("Monospace", FontFamily.Monospace, false),
                                            Triple("Cursive", FontFamily.Cursive, false),
                                            Triple("Default", FontFamily.Default, false),
                                            Triple("Elegant Italic", FontFamily.Serif, true),
                                            Triple("Modern Bold", FontFamily.SansSerif, false),
                                            Triple("Retro Fixed", FontFamily.Monospace, false),
                                            Triple("Stylish Cursive", FontFamily.Cursive, true),
                                            Triple("Custom Compact", FontFamily.Default, true)
                                        )
                                        fontOptionsList.forEachIndexed { fontIdx, (name, family, needsItalic) ->
                                            val isFontSelected = activeOverlay.fontIndex == fontIdx
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isFontSelected) NeonGreen.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.3f))
                                                    .border(1.dp, if (isFontSelected) NeonGreen else Color.DarkGray, RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                        if (idx != -1) {
                                                            textOverlays[idx] = textOverlays[idx].copy(
                                                                fontIndex = fontIdx,
                                                                isItalic = needsItalic || activeOverlay.isItalic,
                                                                isBold = fontIdx == 6 || activeOverlay.isBold
                                                            )
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .testTag("font_option_$fontIdx")
                                            ) {
                                                Text(
                                                    text = name,
                                                    color = if (isFontSelected) NeonGreen else TextWhite,
                                                    fontFamily = family,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (fontIdx == 6) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (needsItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("TEXT COLOR PRESETS", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val presets = listOf(
                                                Color.White, Color.Yellow, Color.Cyan, NeonGreen,
                                                Color(0xFFF43F5E), Color.Black, Color(0xFFFF8A65), Color(0xFFA5D6A7)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                presets.forEachIndexed { presetIdx, presetColor ->
                                                    val isSelected = activeOverlay.color == presetColor
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(presetColor)
                                                            .border(2.dp, if (isSelected) NeonGreen else Color.Black.copy(alpha = 0.5f), CircleShape)
                                                            .clickable {
                                                                val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                                if (idx != -1) {
                                                                    textOverlays[idx] = textOverlays[idx].copy(color = presetColor)
                                                                }
                                                            }
                                                            .testTag("preset_color_$presetIdx")
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("CUSTOM COLOR WHEEL", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val rainbowColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                            var customSliderVal by remember { mutableStateOf(0f) }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Brush.horizontalGradient(rainbowColors))
                                            )
                                            Slider(
                                                value = customSliderVal,
                                                onValueChange = { newVal ->
                                                    customSliderVal = newVal
                                                    val hsv = floatArrayOf(newVal * 360f, 1f, 1f)
                                                    val wheelColor = Color(android.graphics.Color.HSVToColor(hsv))
                                                    val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                    if (idx != -1) {
                                                        textOverlays[idx] = textOverlays[idx].copy(color = wheelColor)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(24.dp).testTag("custom_color_wheel_slider")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("BACKGROUND CONTAINER COLOR", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val bgColorPresets = listOf(
                                                Color.Transparent, Color.Black, Color(0xFF1E1B4B), Color(0xFF311B92), Color(0xFF006064), Color(0xFF1B5E20)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                bgColorPresets.forEachIndexed { bgIdx, presetColor ->
                                                    val isSelected = activeOverlay.bgColor == presetColor
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (presetColor == Color.Transparent) Color.DarkGray else presetColor)
                                                            .border(
                                                                width = 2.dp,
                                                                color = if (isSelected) NeonGreen else Color.Black,
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .clickable {
                                                                val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                                if (idx != -1) {
                                                                    textOverlays[idx] = textOverlays[idx].copy(
                                                                        bgColor = presetColor,
                                                                        bgOpacity = if (presetColor == Color.Transparent) 0f else if (activeOverlay.bgOpacity == 0f) 0.6f else activeOverlay.bgOpacity
                                                                    )
                                                                }
                                                            }
                                                            .testTag("bg_color_$bgIdx")
                                                    ) {
                                                        if (presetColor == Color.Transparent) {
                                                            Text("/", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("BACKGROUND OPACITY (${(activeOverlay.bgOpacity * 100).toInt()}%)", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Slider(
                                                value = activeOverlay.bgOpacity,
                                                onValueChange = { newVal ->
                                                    val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                    if (idx != -1) {
                                                        textOverlays[idx] = textOverlays[idx].copy(bgOpacity = newVal)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("bg_opacity_slider")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("TEXT ALIGN", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                val alignments = listOf(
                                                    0 to "L",
                                                    1 to "C",
                                                    2 to "R"
                                                )
                                                alignments.forEach { (alignVal, label) ->
                                                    val isAlignSelected = activeOverlay.alignment == alignVal
                                                    IconButton(
                                                        onClick = {
                                                            val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                            if (idx != -1) {
                                                                textOverlays[idx] = textOverlays[idx].copy(alignment = alignVal)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(if (isAlignSelected) NeonGreen.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                            .border(1.dp, if (isAlignSelected) NeonGreen else Color.DarkGray, RoundedCornerShape(4.dp))
                                                            .testTag("align_button_$alignVal")
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = if (isAlignSelected) NeonGreen else Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Column {
                                            Text("TEXT STYLE TWEAKS", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                        if (idx != -1) {
                                                            textOverlays[idx] = textOverlays[idx].copy(isBold = !activeOverlay.isBold)
                                                        }
                                                    }.testTag("bold_toggle_wrapper")
                                                ) {
                                                    Checkbox(
                                                        checked = activeOverlay.isBold,
                                                        onCheckedChange = { isBold ->
                                                            val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                            if (idx != -1) {
                                                                textOverlays[idx] = textOverlays[idx].copy(isBold = isBold)
                                                            }
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = NeonGreen),
                                                        modifier = Modifier.testTag("bold_checkbox")
                                                    )
                                                    Text("B", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                        if (idx != -1) {
                                                            textOverlays[idx] = textOverlays[idx].copy(isItalic = !activeOverlay.isItalic)
                                                        }
                                                    }.testTag("italic_toggle_wrapper")
                                                ) {
                                                    Checkbox(
                                                        checked = activeOverlay.isItalic,
                                                        onCheckedChange = { isItalic ->
                                                            val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                            if (idx != -1) {
                                                                textOverlays[idx] = textOverlays[idx].copy(isItalic = isItalic)
                                                            }
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = NeonGreen),
                                                        modifier = Modifier.testTag("italic_checkbox")
                                                    )
                                                    Text("I", color = Color.White, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                }
                                            }
                                        }

                                        Column {
                                            Text("TEXT SHADOW", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                    if (idx != -1) {
                                                        textOverlays[idx] = textOverlays[idx].copy(hasShadow = !activeOverlay.hasShadow)
                                                    }
                                                }.testTag("shadow_toggle_wrapper")
                                            ) {
                                                Checkbox(
                                                    checked = activeOverlay.hasShadow,
                                                    onCheckedChange = { s ->
                                                        val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                        if (idx != -1) {
                                                            textOverlays[idx] = textOverlays[idx].copy(hasShadow = s)
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = NeonGreen),
                                                    modifier = Modifier.testTag("shadow_checkbox")
                                                )
                                                Text("Shadow", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("FONT SIZE (${activeOverlay.sizeSp.toInt()} sp)", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = activeOverlay.sizeSp,
                                        valueRange = 10f..60f,
                                        onValueChange = { newVal ->
                                            val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                            if (idx != -1) {
                                                textOverlays[idx] = textOverlays[idx].copy(sizeSp = newVal)
                                            }
                                        },
                                        colors = SliderDefaults.colors(activeTrackColor = NeonGreen),
                                        modifier = Modifier.testTag("font_size_slider")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("ADJUST DURATION ON TIMELINE LAYER", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Start: ${formatSeconds((activeOverlay.startPct * totalDurationMs).toLong())}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Slider(
                                                value = activeOverlay.startPct,
                                                onValueChange = { newVal ->
                                                    val secureStart = newVal.coerceIn(0f, activeOverlay.endPct - 0.05f)
                                                    val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                    if (idx != -1) {
                                                        textOverlays[idx] = textOverlays[idx].copy(startPct = secureStart)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("text_start_pct_slider")
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "End: ${formatSeconds((activeOverlay.endPct * totalDurationMs).toLong())}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Slider(
                                                value = activeOverlay.endPct,
                                                onValueChange = { newVal ->
                                                    val secureEnd = newVal.coerceIn(activeOverlay.startPct + 0.05f, 1.0f)
                                                    val idx = textOverlays.indexOfFirst { it.id == activeOverlay.id }
                                                    if (idx != -1) {
                                                        textOverlays[idx] = textOverlays[idx].copy(endPct = secureEnd)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("text_end_pct_slider")
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                // auto-create first overlay to get them started of they tap
                                                val startPct = (currentPlaybackPosition.toFloat() / totalDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                                val endPct = (startPct + 0.30f).coerceIn(0f, 1f)
                                                val newOverlay = TextOverlay(
                                                    text = "Tap/Double-tap to Edit Content",
                                                    startPct = startPct,
                                                    endPct = endPct
                                                )
                                                textOverlays.add(newOverlay)
                                                selectedOverlayId = newOverlay.id
                                            }
                                            .testTag("text_no_selection_placeholder"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Tap to Create or Select a Text Overlay",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (activeTool == "Filters") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("filters_editor_workspace")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CINEMATIC FILTERS & GRADING",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = TextMuted
                                    )
                                    
                                    // Reset Button
                                    TextButton(
                                        onClick = {
                                            selectedFilterId = "original"
                                            filterIntensity = 1.0f
                                            brightnessVal = 0.0f
                                            contrastVal = 0.0f
                                            saturationVal = 0.0f
                                            temperatureVal = 0.0f
                                            highlightsVal = 0.0f
                                            shadowsVal = 0.0f
                                            sharpnessVal = 0.0f
                                            Toast.makeText(context, "Reset all color grading", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp).testTag("filters_reset_all_button")
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset", tint = NeonGreen, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("RESET ALL", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // PRESETS LIST (Horizontal scroll with nice preview circles)
                                Text(
                                    text = "PRESET CHROMA FILTERS",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                val filterPresets = listOf(
                                    Triple("original", "Original", Color.DarkGray),
                                    Triple("vivid", "Vivid Boost", Color(0xFFFF4081)),
                                    Triple("warm", "Golden Hour", Color(0xFFFFB300)),
                                    Triple("cool", "Nordic Blue", Color(0xFF00E5FF)),
                                    Triple("bw", "Noir Mono", Color(0xFF808080)),
                                    Triple("vintage", "Retro 1970", Color(0xFF8B5A2B)),
                                    Triple("cinema", "Teal & Orange", Color(0xFF008080)),
                                    Triple("night", "Night Boost", Color(0xFF00B0FF)),
                                    Triple("cyberpunk", "Neon Pulse", Color(0xFFFF007F)),
                                    Triple("sepia", "Antique Sepia", Color(0xFF704214)),
                                    Triple("rose", "Rose Dream", Color(0xFFFF3366)),
                                    Triple("forest", "Jungle Canopy", Color(0xFF2E7D32)),
                                    Triple("solarize", "Neon Solar", Color(0xFFCCCCCC)),
                                    Triple("sunset", "Amber Flare", Color(0xFFFF3D00)),
                                    Triple("emerald", "Jade Emerald", Color(0xFF00C853)),
                                    Triple("aurora", "Aurora Neon", Color(0xFF00E676))
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    filterPresets.forEach { (fid, fname, fcolor) ->
                                        val isSel = selectedFilterId == fid
                                        val isFreeFilter = fid in listOf("original", "vivid", "warm", "cool")
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    selectedFilterId = fid
                                                    if (!isPro && !isFreeFilter) {
                                                        Toast.makeText(context, "Free trial: $fname filter applied! (Watermark will be added on download)", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                .padding(vertical = 4.dp)
                                                .testTag("filter_preset_item_$fid")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (fid == "cyberpunk") {
                                                            Brush.horizontalGradient(listOf(Color(0xFFFF007F), Color(0xFF00F5FF)))
                                                        } else if (fid == "aurora") {
                                                            Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFFD500F9)))
                                                        } else if (fid == "sunset") {
                                                            Brush.verticalGradient(listOf(Color(0xFFFF3D00), Color(0xFFFFD600)))
                                                        } else {
                                                            Brush.radialGradient(listOf(fcolor, fcolor.copy(alpha = 0.4f)))
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSel) 2.5.dp else 1.dp,
                                                        color = if (isSel) NeonGreen else BorderGray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSel) {
                                                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = fname,
                                                color = if (isSel) NeonGreen else TextWhite,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // FILTER INTENSITY SLIDER (Only show if not 'original')
                                if (selectedFilterId != "original") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "FILTER INTENSITY (${(filterIntensity * 100).toInt()}%)",
                                            color = TextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(onClick = { filterIntensity = 1.0f }, modifier = Modifier.height(18.dp)) {
                                            Text("RESET INTENSITY", color = NeonGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Slider(
                                        value = filterIntensity,
                                        onValueChange = { filterIntensity = it },
                                        colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                        modifier = Modifier.fillMaxWidth().height(24.dp).testTag("filters_intensity_slider")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // MANUAL COLOR GRADING SECTION
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(BorderGray.copy(alpha = 0.2f))
                                        .padding(vertical = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "MANUAL RESOLVE COLOR GRADING",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                // Tabular or scrollable layout for the manual controls to avoid huge height
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    
                                    // 1. Brightness Slider (-100% to 100%)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.LightMode, contentDescription = null, tint = TextWhite, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("BRIGHTNESS (${(brightnessVal * 100).toInt()}%)", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (brightnessVal != 0.0f) {
                                                Text("Reset", color = NeonGreen, fontSize = 8.sp, modifier = Modifier.clickable { brightnessVal = 0.0f }.testTag("reset_brightness"))
                                            }
                                        }
                                        Slider(
                                            value = brightnessVal,
                                            valueRange = -1.0f..1.0f,
                                            onValueChange = { brightnessVal = it },
                                            colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("brightness_slider")
                                        )
                                    }

                                    // 2. Contrast Slider (-100% to 100%)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Contrast, contentDescription = null, tint = TextWhite, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("CONTRAST (${(contrastVal * 100).toInt()}%)", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (contrastVal != 0.0f) {
                                                Text("Reset", color = NeonGreen, fontSize = 8.sp, modifier = Modifier.clickable { contrastVal = 0.0f }.testTag("reset_contrast"))
                                            }
                                        }
                                        Slider(
                                            value = contrastVal,
                                            valueRange = -1.0f..1.0f,
                                            onValueChange = { contrastVal = it },
                                            colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("contrast_slider")
                                        )
                                    }

                                    // 3. Saturation Slider (-100% to 100%)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = TextWhite, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("SATURATION (${(saturationVal * 100).toInt()}%)", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (saturationVal != 0.0f) {
                                                Text("Reset", color = NeonGreen, fontSize = 8.sp, modifier = Modifier.clickable { saturationVal = 0.0f }.testTag("reset_saturation"))
                                            }
                                        }
                                        Slider(
                                            value = saturationVal,
                                            valueRange = -1.0f..1.0f,
                                            onValueChange = { saturationVal = it },
                                            colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("saturation_slider")
                                        )
                                    }

                                    // 4. Temperature Slider (Warm ↔ Cool)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = TextWhite, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("TEMPERATURE (WARM ↔ COOL)", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (temperatureVal != 0.0f) {
                                                Text("Reset", color = NeonGreen, fontSize = 8.sp, modifier = Modifier.clickable { temperatureVal = 0.0f }.testTag("reset_temperature"))
                                            }
                                        }
                                        Slider(
                                            value = temperatureVal,
                                            valueRange = -1.0f..1.0f,
                                            onValueChange = { temperatureVal = it },
                                            colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("temperature_slider")
                                        )
                                    }

                                    // 5. Highlights Slider (-100% to 100%)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.FilterHdr, contentDescription = null, tint = TextWhite, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("HIGHLIGHTS (${(highlightsVal * 100).toInt()}%)", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (highlightsVal != 0.0f) {
                                                Text("Reset", color = NeonGreen, fontSize = 8.sp, modifier = Modifier.clickable { highlightsVal = 0.0f }.testTag("reset_highlights"))
                                            }
                                        }
                                        Slider(
                                            value = highlightsVal,
                                            valueRange = -1.0f..1.0f,
                                            onValueChange = { highlightsVal = it },
                                            colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("highlights_slider")
                                        )
                                    }

                                    // 6. Shadows Slider (-100% to 100%)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.FilterBAndW, contentDescription = null, tint = TextWhite, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("SHADOWS (${(shadowsVal * 100).toInt()}%)", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (shadowsVal != 0.0f) {
                                                Text("Reset", color = NeonGreen, fontSize = 8.sp, modifier = Modifier.clickable { shadowsVal = 0.0f }.testTag("reset_shadows"))
                                            }
                                        }
                                        Slider(
                                            value = shadowsVal,
                                            valueRange = -1.0f..1.0f,
                                            onValueChange = { shadowsVal = it },
                                            colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("shadows_slider")
                                        )
                                    }

                                    // 7. Sharpness Slider (-100% to 100%)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.BlurOn, contentDescription = null, tint = TextWhite, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("SHARPNESS/TEXTURE (${(sharpnessVal * 100).toInt()}%)", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (sharpnessVal != 0.0f) {
                                                Text("Reset", color = NeonGreen, fontSize = 8.sp, modifier = Modifier.clickable { sharpnessVal = 0.0f }.testTag("reset_sharpness"))
                                            }
                                        }
                                        Slider(
                                            value = sharpnessVal,
                                            valueRange = -1.0f..1.0f,
                                            onValueChange = { sharpnessVal = it },
                                            colors = SliderDefaults.colors(activeTrackColor = NeonGreen, thumbColor = NeonGreen),
                                            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("sharpness_slider")
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (activeTool == "Speed") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "CHRONOS SPEED MASTER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // SPEED ADJUST MODE TABS
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val modes = listOf("entire" to "Entire Video", "section" to "Selected Section")
                                    modes.forEach { (modeKey, modeTitle) ->
                                        val isSel = speedAdjustMode == modeKey
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent)
                                                .border(if (isSel) BorderStroke(1.dp, Color(0xFF00E5FF)) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(6.dp))
                                                .clickable { speedAdjustMode = modeKey }
                                                .padding(vertical = 8.dp)
                                                .testTag("speed_mode_$modeKey"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = modeTitle,
                                                color = if (isSel) Color(0xFF00E5FF) else TextWhite.copy(alpha = 0.6f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (speedAdjustMode == "entire") {
                                        "Entire Video Timeline"
                                    } else {
                                        "Position Section: " + 
                                                formatCentiseconds((speedStartPct * totalDurationMs).toLong()) + 
                                                " - " + 
                                                formatCentiseconds((speedEndPct * totalDurationMs).toLong())
                                    },
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                var selectedSpeedFactor by remember { mutableFloatStateOf(1.0f) }
                                var selectedMuteAudio by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val speeds = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f)
                                    speeds.forEach { sp ->
                                        val isCurrent = selectedSpeedFactor == sp
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isCurrent) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.4f))
                                                .clickable { selectedSpeedFactor = sp }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("speed_factor_btn_${sp.toString().replace(".", "_")}")
                                        ) {
                                            Text(
                                                text = "${sp}x",
                                                color = if (isCurrent) Color.Black else TextWhite,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { selectedMuteAudio = !selectedMuteAudio }
                                    ) {
                                        Checkbox(
                                            checked = selectedMuteAudio,
                                            onCheckedChange = { selectedMuteAudio = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF00E5FF),
                                                checkmarkColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("speed_mute_checkbox")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Mute audio",
                                            color = TextWhite,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row {
                                        Button(
                                            onClick = { resetSpeeds() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp).testTag("speed_reset_btn")
                                        ) {
                                            Text("RESET", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val startMs = (speedStartPct * totalDurationMs).toLong()
                                                val endMs = (speedEndPct * totalDurationMs).toLong()
                                                applySpeedToRange(startMs, endMs, selectedSpeedFactor, selectedMuteAudio)
                                                val label = if (speedAdjustMode == "entire") "Applied ${selectedSpeedFactor}x to entire video!" else "Applied ${selectedSpeedFactor}x to range!"
                                                Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp).testTag("speed_apply_btn")
                                        ) {
                                            Text(
                                                text = if (speedAdjustMode == "entire") "APPLY TO ENTIRE VIDEO" else "APPLY TO SECTION",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Visual explanation of the dynamic stretched duration
                                if (totalStretchedDurationMs != totalDurationMs) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF111827))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "Timeline scaled: ${formatSeconds(totalDurationMs)} Original → ${formatSeconds(totalStretchedDurationMs)} Stretched due to slow/motion speed mappings.",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Multi-utility panel selections
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val tools = listOf(
                            Triple("Trim Range", Icons.Rounded.ContentCut, "Trim"),
                            Triple("Merge/Combine", Icons.Rounded.CallMerge, "Merge"),
                            Triple("Split Video", Icons.Rounded.CallSplit, "Split"),
                            Triple("Audio Mixer", Icons.Rounded.GraphicEq, "Audio"),
                            Triple("Chroma Filters", Icons.Rounded.FilterBAndW, "Filters"),
                            Triple("Playback Speed", Icons.Rounded.Speed, "Speed"),
                            Triple("Add Text overlay", Icons.Rounded.TextFields, "Text")
                        )

                        tools.forEach { (label, icon, key) ->
                            val isSelected = activeTool == key
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        activeTool = key
                                        if (!isPro && key != "Trim" && key != "Filters") {
                                            Toast.makeText(context, "Free tier: $label is unlocked for trial editing! (Watermark added on export)", Toast.LENGTH_LONG).show()
                                        }
                                        if (key == "Trim") {
                                            isTrimRangeActive = !isTrimRangeActive
                                        } else {
                                            if (key != "Merge") {
                                                Toast.makeText(context, "$label settings menu activated", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) NeonGreen else TextWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) NeonGreen else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // DYNAMIC MERGE COMPILING DIALOG
            if (showMergeProgressDialog) {
                val startTime = remember { mutableStateOf(0L) }
                LaunchedEffect(showMergeProgressDialog) {
                    if (showMergeProgressDialog) {
                        startTime.value = System.currentTimeMillis()
                    }
                }
                
                AlertDialog(
                    onDismissRequest = { if (!isMergingProcessing) showMergeProgressDialog = false },
                    title = {
                        Text(
                            text = "COMPILING COMBINED SEQUENCE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            if (isMergingProcessing) {
                                val currentStep = when {
                                    mergeProgress < 0.20f -> "Analyzing tracks & aspect ratios..."
                                    mergeProgress < 0.50f -> "Scaling mismatched video feeds..."
                                    mergeProgress < 0.85f -> "Stitching continuous audio grids & join smoothing..."
                                    else -> "Multiplexing final master HEVC stream..."
                                }
                                Text(
                                    text = "Processing video stream & welding audio joins...",
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { mergeProgress },
                                    color = NeonGreen,
                                    trackColor = Color(0xFF27272A),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                    Text(
                                        text = currentStep,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    val now = System.currentTimeMillis()
                                    val elapsed = now - startTime.value
                                    val remainingText = if (mergeProgress > 0.05f) {
                                        val totalEstTime = (elapsed.toFloat() / mergeProgress)
                                        val remainingMs = (totalEstTime - elapsed).toLong()
                                        val remSecs = (remainingMs / 1000).coerceAtLeast(1)
                                        "${remSecs}s remaining"
                                    } else {
                                        "Calculating remaining time..."
                                    }
                                    Text(
                                        text = remainingText,
                                        color = NeonGreen,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Weld all clips in sequence into a single high-definition film. Audio joins will be processed continuously with pop-filtering and continuous track stitchers.",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clips count:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "${mergeClips.size} clips in sequence",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Merged Duration:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = formatCentiseconds(totalMergeDurationMs),
                                        color = NeonGreen,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Aspect Ratio:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    val firstRec = mergeClips.firstOrNull() ?: recording
                                    Text(
                                        text = VideoMetadataManager.getAspectRatio(context, firstRec),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Compilation Bitrate:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "High Quality HEVC (Fixed)",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!isMergingProcessing) {
                            Button(
                                onClick = {
                                    isMergingProcessing = true
                                    mergeProgress = 0f
                                    coroutineScope.launch {
                                        val outputUri = withContext(Dispatchers.IO) {
                                            VideoMetadataManager.saveMergedVideo(
                                                context = context,
                                                recordings = mergeClips.toList(),
                                                totalDurationMs = totalMergeDurationMs,
                                                onProgress = { p ->
                                                    mergeProgress = p
                                                }
                                            )
                                        }
                                        
                                        if (outputUri != null) {
                                            Toast.makeText(context, "Sequence merged & saved to DuoCam Gallery successfully!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to compile merged sequence", Toast.LENGTH_SHORT).show()
                                        }
                                        
                                        isMergingProcessing = false
                                        showMergeProgressDialog = false
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                modifier = Modifier.fillMaxWidth().testTag("compile_merge_confirm_btn")
                            ) {
                                Text("Compile & Render Film", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        if (!isMergingProcessing) {
                            TextButton(onClick = { showMergeProgressDialog = false }) {
                                Text("Cancel", color = TextWhite)
                            }
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            // CHOSE/CONFIGURE CLIP TRANSITION DIALOG
            if (editingTransitionIndex != null) {
                val idx = editingTransitionIndex!!
                val transition = clipTransitions[idx] ?: ClipTransition("none", 1000L)
                
                AlertDialog(
                    onDismissRequest = { editingTransitionIndex = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CallMerge,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clip Transition #${idx + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = TextWhite
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "SELECT TRANSITION STYLE:",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Transition type selection options
                            val transitionTypes = listOf(
                                Triple("none", "None (Hard Cut)", "Immediate hard cut switch between clips"),
                                Triple("crossfade", "Crossfade/Dissolve", "Smooth blend between consecutive clips"),
                                Triple("fade_black", "Fade through Black", "First clip fades to black, second fades in"),
                                Triple("slide", "Slide Left", "Second clip slides smoothly over the current one to the left"),
                                Triple("slide_right", "Slide Right", "Second clip slides smoothly over the current one to the right"),
                                Triple("zoom", "Zoom Transition", "Current clip zooms in and morphs into the next clip"),
                                Triple("wipe", "Sweep Wipe", "Revelatory vertical sweep line reveals the next clip")
                            )
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                transitionTypes.forEach { (typeKey, typeLabel, typeDesc) ->
                                    val isSelected = transition.type == typeKey
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.3f))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) NeonGreen else BorderGray.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                clipTransitions[idx] = transition.copy(type = typeKey)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                            .testTag("transition_select_type_${typeKey}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) NeonGreen else Color.DarkGray)
                                                .border(1.dp, if (isSelected) Color.Black else Color.White.copy(alpha = 0.3f), CircleShape)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(10.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = typeLabel,
                                                color = if (isSelected) NeonGreen else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = typeDesc,
                                                color = TextMuted,
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Duration selection options if a transition style is selected
                            if (transition.type != "none") {
                                Text(
                                    text = "DURATION CONTROL:",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val durations = listOf(
                                    Pair(300L, "0.3s"),
                                    Pair(500L, "0.5s"),
                                    Pair(1000L, "1.0s"),
                                    Pair(1500L, "1.5s"),
                                    Pair(2000L, "2.0s")
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    durations.forEach { (durMs, label) ->
                                        val isDurSelected = transition.durationMs == durMs
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isDurSelected) NeonGreen else Color.Black.copy(alpha = 0.4f))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isDurSelected) NeonGreen else BorderGray.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    clipTransitions[idx] = transition.copy(durationMs = durMs)
                                                }
                                                .padding(vertical = 8.dp)
                                                .testTag("transition_select_duration_${label}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isDurSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { editingTransitionIndex = null },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("transition_confirm_btn")
                        ) {
                            Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    },
                    dismissButton = {
                        if (transition.type != "none") {
                            TextButton(
                                onClick = {
                                    clipTransitions[idx] = ClipTransition("none", 1000L)
                                    editingTransitionIndex = null
                                },
                                modifier = Modifier.testTag("transition_remove_btn")
                            ) {
                                Text("REMOVE EFFECT", color = GlowRecordRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 6.dp
                )
            }

            // SELECT CLIPS DIALOG
            if (showMergeSelectDialog) {
                var allRecordings by remember { mutableStateOf<List<DuoRecording>>(emptyList()) }
                LaunchedEffect(showMergeSelectDialog) {
                    if (showMergeSelectDialog) {
                        allRecordings = getLocalRecordings(context)
                    }
                }
                AlertDialog(
                    onDismissRequest = { showMergeSelectDialog = false },
                    title = {
                        Text(
                            text = "SELECT CLIPS TO MERGE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                        ) {
                            Text(
                                text = "Tap on any recording recorded on your device's Dual Camera to append it to the active combine queue.",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            if (allRecordings.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No recordings found in DuoCam folder", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(allRecordings) { index, rec ->
                                        val name = VideoMetadataManager.getDisplayName(context, rec)
                                        val durationStr = formatSeconds(rec.durationMs)
                                        val sizeStr = formatMergedSize(rec.sizeBytes)
                                        val resLabel = getRecordingResolution(rec)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.4f))
                                                .border(1.dp, BorderGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    mergeClips.add(rec)
                                                    Toast.makeText(context, "Added $name to sequence", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = name,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(durationStr, color = NeonGreen, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                                    Text(sizeStr, color = TextMuted, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(resLabel, color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonGreen.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = "Add clip",
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showMergeSelectDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Done Selecting", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            // SIMULATED HIGH-TECH EXPORT ENGINE DIALOGUE
            if (showExportDialog) {
                val haptic = LocalHapticFeedback.current
                
                AlertDialog(
                    onDismissRequest = { 
                        if (VideoExportManager.exportState != "exporting") {
                            showExportDialog = false 
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (VideoExportManager.exportState == "success") Icons.Rounded.CheckCircle 
                                             else if (VideoExportManager.exportState == "failed") Icons.Rounded.ErrorOutline
                                             else Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = if (VideoExportManager.exportState == "success") NeonGreen 
                                       else if (VideoExportManager.exportState == "failed") GlowRecordRed 
                                       else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (VideoExportManager.exportState) {
                                    "settings" -> "EXPORT SETTINGS"
                                    "exporting" -> "EXPORT PROGRESS"
                                    "success" -> "EXPORT COMPLETE!"
                                    "failed" -> "EXPORT FAILED"
                                    else -> "COMPILE SEQUENCE"
                                },
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            when (VideoExportManager.exportState) {
                                "settings" -> {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    Text(
                                        text = "PLATFORM EXPORT PRESET",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    // LazyRow of platform presets selection cards
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("preset_row"),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(VideoExportManager.presets) { preset ->
                                            val isSel = VideoExportManager.selectedPresetId == preset.id
                                            val pClr = when (preset.platform) {
                                                "YouTube" -> Color(0xFFFF0000)
                                                "TikTok" -> Color(0xFF00F2FE)
                                                "Instagram Reels", "Instagram Post" -> Color(0xFFE1306C)
                                                "Twitter/X" -> Color(0xFF1DA1F2)
                                                "WhatsApp" -> Color(0xFF25D366)
                                                else -> NeonGreen
                                            }
                                            val icon = when (preset.platform) {
                                                "YouTube" -> Icons.Rounded.PlayCircle
                                                "TikTok" -> Icons.Rounded.MusicNote
                                                "Instagram Reels" -> Icons.Rounded.VideoCameraFront
                                                "Instagram Post" -> Icons.Rounded.Photo
                                                "Twitter/X" -> Icons.Rounded.AlternateEmail
                                                "WhatsApp" -> Icons.Rounded.Chat
                                                else -> Icons.Rounded.VideoFile
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(135.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSel) pClr.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f))
                                                    .border(2.dp, if (isSel) pClr else BorderGray.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                                    .clickable { VideoExportManager.selectedPresetId = preset.id }
                                                    .padding(10.dp)
                                                    .testTag("preset_${preset.id}")
                                            ) {
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(icon, contentDescription = null, tint = pClr, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(preset.platform, color = pClr, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(preset.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text("${preset.aspect} • ${preset.res}", color = TextMuted, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }

                                    val activePreset = VideoExportManager.presets.find { it.id == VideoExportManager.selectedPresetId } ?: VideoExportManager.presets[0]
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = activePreset.description,
                                        color = Color(0xFFD4D4D8),
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.fillMaxWidth().testTag("preset_desc")
                                    )

                                    // Check aspect ratio mismatch
                                    val origAspect = VideoExportManager.currentRecording?.let { VideoMetadataManager.getAspectRatio(context, it) } ?: "16:9"
                                    val targetAspect = if (activePreset.aspect == "original") origAspect else activePreset.aspect
                                    val showMismatch = origAspect != targetAspect

                                    if (showMismatch) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            color = GlowRecordRed.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, GlowRecordRed.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("aspect_mismatch_banner")
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "ASPECT RATIO MISMATCH DETECTED",
                                                    color = GlowRecordRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "Recording ratio ($origAspect) doesn't match preset ratio ($targetAspect). Choose how to conform:",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                                
                                                val options = listOf("Crop to fit", "Add bars", "Stretch to fit")
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    options.forEach { opt ->
                                                        val isSel = VideoExportManager.mismatchOption == opt
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isSel) GlowRecordRed else Color.Black.copy(alpha = 0.3f))
                                                                .border(1.dp, if (isSel) GlowRecordRed else BorderGray.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                                .clickable { VideoExportManager.mismatchOption = opt }
                                                                .padding(vertical = 6.dp)
                                                                .testTag("mismatch_opt_${opt.replace(" ", "_")}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = opt,
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

                                    // Visual Framing Screen Preview Simulator Box
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "PLATFORM FRAMING PREVIEW",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF09090B))
                                            .border(1.dp, BorderGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .testTag("framing_preview_container"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val targetW_ratio = when (targetAspect) {
                                            "16:9" -> 1.77f
                                            "9:16" -> 0.562f
                                            "1:1" -> 1.0f
                                            "4:5" -> 0.8f
                                            else -> 1.77f
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(targetW_ratio)
                                                .fillMaxHeight(0.85f)
                                                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val opt = VideoExportManager.mismatchOption
                                            if (!showMismatch) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                colors = listOf(Color(0xFF1E1B4B), Color(0xFF311042))
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonGreen.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                                        Text("MATChING FIT ($origAspect)", color = NeonGreen.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            } else {
                                                val origW_ratio = when (origAspect) {
                                                    "16:9" -> 1.77f
                                                    "9:16" -> 0.562f
                                                    "1:1" -> 1.0f
                                                    "4:5" -> 0.8f
                                                    else -> 1.77f
                                                }
                                                
                                                if (opt == "Stretch to fit") {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                    colors = listOf(Color(0xFF2E1065), Color(0xFF1E3A8A))
                                                                )
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("STRETCHED TO FIT", color = Color.Yellow.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else if (opt == "Crop to fit") {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                    colors = listOf(Color(0xFF2E1065), Color(0xFF1E3A8A))
                                                                )
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("CROPPED (FILL)", color = NeonGreen.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .aspectRatio(origW_ratio)
                                                            .fillMaxSize()
                                                            .background(
                                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                    colors = listOf(Color(0xFF2D142C), Color(0xFF311042))
                                                                )
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("LETTERBOX / PILLAR", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = "@duocam_master",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 6.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.BottomStart)
                                                )
                                                if (activePreset.platform == "TikTok" || activePreset.platform == "Instagram Reels") {
                                                    Column(
                                                        modifier = Modifier.align(Alignment.BottomEnd),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.PostAdd, contentDescription = null, tint = Color.White.copy(alpha=0.4f), modifier = Modifier.size(8.dp))
                                                        Icon(Icons.Rounded.Chat, contentDescription = null, tint = Color.White.copy(alpha=0.4f), modifier = Modifier.size(8.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Hardware Acceleration & Dev Switch Toggle
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.ElectricBolt, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Hardware Acceleration", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Switch(
                                                    checked = VideoExportManager.useHardwareAcceleration,
                                                    onCheckedChange = { VideoExportManager.useHardwareAcceleration = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.Black,
                                                        checkedTrackColor = NeonGreen,
                                                        uncheckedThumbColor = Color.Gray,
                                                        uncheckedTrackColor = Color.DarkGray
                                                    ),
                                                    modifier = Modifier
                                                        .graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
                                                        .testTag("export_toggle_hw_accel")
                                                )
                                            }
                                            
                                            HorizontalDivider(color = BorderGray.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
  
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.BugReport, contentDescription = null, tint = GlowRecordRed, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Simulate Storage Full Error", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Switch(
                                                    checked = VideoExportManager.simulateFailure,
                                                    onCheckedChange = { VideoExportManager.simulateFailure = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.Black,
                                                        checkedTrackColor = GlowRecordRed,
                                                        uncheckedThumbColor = Color.Gray,
                                                        uncheckedTrackColor = Color.DarkGray
                                                    ),
                                                    modifier = Modifier
                                                        .graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
                                                        .testTag("export_toggle_fail_sim")
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Estimates Card
                                    Surface(
                                        color = NeonGreen.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Estimated File Size:", color = TextMuted, fontSize = 11.sp)
                                                val estBytes = VideoExportManager.getEstimatedFileSize()
                                                val estMb = estBytes / (1024.0 * 1024.0)
                                                Text(
                                                    text = String.format("%.2f MB", estMb),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.testTag("export_est_size_text")
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Estimated Export Time:", color = TextMuted, fontSize = 11.sp)
                                                val estSec = VideoExportManager.getEstimatedExportDurationMs() / 1000.0
                                                Text(
                                                    text = String.format("~%.1f seconds", estSec),
                                                    color = NeonGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.testTag("export_est_time_text")
                                                )
                                            }
                                        }
                                    }
                                }

                                "exporting" -> {
                                    Text(
                                        text = "Encoding edited video streams utilizing native multi-core hardware codec architecture...",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    LinearProgressIndicator(
                                        progress = { VideoExportManager.exportProgress },
                                        color = NeonGreen,
                                        trackColor = Color(0xFF27272A),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .testTag("export_progress_indicator")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(VideoExportManager.exportProgress * 100).toInt()}% Compiled",
                                            color = NeonGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.testTag("export_pct_text")
                                        )
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Elapsed: ${String.format("%.1fs", VideoExportManager.elapsedTimeMs / 1000f)}",
                                                color = TextWhite,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "•",
                                                color = Color.DarkGray,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Rem: ~${String.format("%.1fs", VideoExportManager.estimatedRemainingTimeMs / 1000f)}",
                                                color = NeonGreen,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.testTag("export_rem_time_text")
                                            )
                                        }
                                    }
                                }

                                "success" -> {
                                    val successThumbnail = VideoExportManager.currentRecording?.let { rec ->
                                        mergeClipThumbnails[rec.uri.toString()]
                                    } ?: mergeClips.firstOrNull()?.let { rec ->
                                        mergeClipThumbnails[rec.uri.toString()]
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Card displaying final thumbnail
                                        Surface(
                                            color = Color.Black,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .size(100.dp, 65.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            if (successThumbnail != null) {
                                                Image(
                                                    bitmap = successThumbnail.asImageBitmap(),
                                                    contentDescription = "Success video frame preview",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color.DarkGray)
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "COMPILATION SUCCESSFUL!",
                                                color = NeonGreen,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "The composition has been fully merged, re-encoded, and registered in standard Gallery format.",
                                                color = TextWhite,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                "failed" -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            color = GlowRecordRed.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, GlowRecordRed.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = VideoExportManager.errorMessage ?: "General write-to-disk error. System IO or hardware codec failure.",
                                                color = GlowRecordRed,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(10.dp).testTag("export_error_detail_text")
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(14.dp))
                                        
                                        Text(
                                            text = "TROUBLESHOOTING RESOLUTIONS:",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        listOf(
                                            "1. Ensure you have at least 150MB of free space in your storage.",
                                            "2. Try choosing a lower resolution (e.g. 720p HD) to speed up rendering.",
                                            "3. Change codec to standard high-compatibility H.264.",
                                            "4. Clear system cache memory before initiating render pipeline."
                                        ).forEach { suggestion ->
                                            Text(
                                                text = suggestion,
                                                color = TextMuted,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        when (VideoExportManager.exportState) {
                            "settings" -> {
                                Button(
                                    onClick = {
                                        VideoExportManager.startExport(context) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            Toast.makeText(context, "Export finished successfully!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("export_confirm_settings_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ENGAGE RENDERING", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            "exporting" -> {
                                Button(
                                    onClick = { 
                                        showExportDialog = false // close dialog but leave job running in background!
                                        Toast.makeText(context, "Exporting continues in background... dynamic island active.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("export_background_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("BACKGROUND MODE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            "success" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Option 1: PLAY
                                    Button(
                                        onClick = {
                                            VideoExportManager.exportedUri?.let { uri ->
                                                try {
                                                    val playIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "video/mp4")
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(playIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "No standard media player found on device.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("export_play_btn")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PLAY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    // Option 2: SHARE
                                    Button(
                                        onClick = {
                                            VideoExportManager.exportedUri?.let { uri ->
                                                try {
                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "video/mp4"
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Exported Video"))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to initialize sharing menu.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("export_share_btn")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SHARE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    // Option 3: DONE GALLERY
                                    Button(
                                        onClick = {
                                            showExportDialog = false
                                            VideoExportManager.reset()
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("export_gallery_btn")
                                    ) {
                                        Text("FINISHED", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            "failed" -> {
                                Button(
                                    onClick = { 
                                        VideoExportManager.reset()
                                        VideoExportManager.exportState = "settings"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("export_retry_btn")
                                ) {
                                    Text("CONFIGURE SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    },
                    dismissButton = {
                        when (VideoExportManager.exportState) {
                            "settings" -> {
                                TextButton(
                                    onClick = { showExportDialog = false },
                                    modifier = Modifier.testTag("export_cancel_settings_btn")
                                ) {
                                    Text("CANCEL", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            "exporting" -> {
                                TextButton(
                                    onClick = { 
                                        VideoExportManager.cancelExport()
                                        showExportDialog = false
                                    },
                                    modifier = Modifier.testTag("export_cancel_compile_btn")
                                ) {
                                    Text("ABORT COMPILATION", color = GlowRecordRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            "failed" -> {
                                TextButton(
                                    onClick = { 
                                        VideoExportManager.reset()
                                        showExportDialog = false 
                                    },
                                    modifier = Modifier.testTag("export_close_failed_btn")
                                ) {
                                    Text("CANCEL", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            // Dynamic Island Progress Pill for background exporting
            if (VideoExportManager.isExporting && !showExportDialog) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .clickable { showExportDialog = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .zIndex(99f)
                        .testTag("export_dynamic_island_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { VideoExportManager.exportProgress },
                            color = NeonGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Exporting sequence: ${(VideoExportManager.exportProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• Tap to view",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // DISCARD CONFIRMATION DIALOGUE
            if (showUnsavedChangesConfirm) {
                AlertDialog(
                    onDismissRequest = { showUnsavedChangesConfirm = false },
                    title = {
                        Text(
                            text = "DISCARD CHANGES",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to exit? Any active edits done to this video stream will be discarded.",
                            color = TextWhite.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUnsavedChangesConfirm = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlowRecordRed)
                        ) {
                            Text("Discard", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnsavedChangesConfirm = false }) {
                            Text("Resume Editing", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            // TRIM SAVE MENU DIALOG
            if (showTrimSaveDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSavingTrim) showTrimSaveDialog = false },
                    title = {
                        Text(
                            text = "SAVE TRIM/CUT SEQUENCE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            if (isSavingTrim) {
                                Text(
                                    text = "Processing video stream & applying new crop parameters...",
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { trimSaveProgress },
                                    color = NeonGreen,
                                    trackColor = Color(0xFF27272A),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${(trimSaveProgress * 100).toInt()}% Cut Done",
                                    color = NeonGreen,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            } else {
                                Text(
                                    text = "Select how you would like to save your edited clip. Your original recording can be kept safe, or safely overwritten.",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Trim Range:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "${formatCentiseconds((trimStartPct * totalDurationMs).toLong())} - ${formatCentiseconds((trimEndPct * totalDurationMs).toLong())}",
                                        color = NeonGreen,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("New Duration:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    val trimmedLen = ((trimEndPct - trimStartPct) * totalDurationMs).toLong()
                                    Text(
                                        text = formatCentiseconds(trimmedLen),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!isSavingTrim) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        isSavingTrim = true
                                        trimSaveProgress = 0f
                                        coroutineScope.launch {
                                            for (i in 1..20) {
                                                delay(60)
                                                trimSaveProgress = i / 20f
                                            }
                                            val sMs = (trimStartPct * totalDurationMs).toLong()
                                            val eMs = (trimEndPct * totalDurationMs).toLong()
                                            val newUri = VideoMetadataManager.saveTrimAsNew(context, recording, sMs, eMs)
                                            if (newUri != null) {
                                                Toast.makeText(context, "Trimmed copy saved as new video!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Failed to clone trimmed video file", Toast.LENGTH_SHORT).show()
                                            }
                                            isSavingTrim = false
                                            showTrimSaveDialog = false
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("save_as_new_button")
                                ) {
                                    Text("Save as New Copy", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        showReplaceAlertConfirm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, GlowRecordRed),
                                    modifier = Modifier.fillMaxWidth().testTag("replace_original_button")
                                ) {
                                    Text("Replace Original Source", color = GlowRecordRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    dismissButton = {
                        if (!isSavingTrim) {
                            TextButton(onClick = { showTrimSaveDialog = false }) {
                                Text("Go Back", color = TextWhite)
                            }
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            // REPLACE ORIGINAL ALERT CONFIRMATION DIALOG
            if (showReplaceAlertConfirm) {
                AlertDialog(
                    onDismissRequest = { if (!isSavingTrim) showReplaceAlertConfirm = false },
                    title = {
                        Text(
                            text = "CONFIRM REPLACEMENT",
                            color = GlowRecordRed,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to replace the original video file? This will apply the trim markers directly and cannot be undone.",
                            color = TextWhite.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showReplaceAlertConfirm = false
                                isSavingTrim = true
                                trimSaveProgress = 0f
                                coroutineScope.launch {
                                    for (i in 1..20) {
                                        delay(60)
                                        trimSaveProgress = i / 20f
                                    }
                                    val sMs = (trimStartPct * totalDurationMs).toLong()
                                    val eMs = (trimEndPct * totalDurationMs).toLong()
                                    VideoMetadataManager.saveTrimPoints(context, recording.uri, sMs, eMs)
                                    Toast.makeText(context, "Original video overwritten successfully!", Toast.LENGTH_LONG).show()
                                    isSavingTrim = false
                                    showTrimSaveDialog = false
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlowRecordRed)
                        ) {
                            Text("Confirm Overwrite", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReplaceAlertConfirm = false }) {
                            Text("Cancel", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            // SPEED SAVE MENU DIALOG
            if (showSpeedSaveDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSavingSpeed) showSpeedSaveDialog = false },
                    title = {
                        Text(
                            text = "SAVE SPEED ADJUSTED RECORDING",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            if (isSavingSpeed) {
                                Text(
                                    text = "Processing video stream & rendering slow-mo/fast-mo curves...",
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { speedSaveProgress },
                                    color = NeonGreen,
                                    trackColor = Color(0xFF27272A),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${(speedSaveProgress * 100).toInt()}% Rendered",
                                    color = NeonGreen,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            } else {
                                Text(
                                    text = "Apply motion speed configurations (0.25x - 4x) over timeline sectors and save. Standard audio pitch is naturally adjusted.",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Speed Sections Range:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "${speedSections.size} active map layers",
                                        color = NeonGreen,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Original Duration:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = formatCentiseconds(totalDurationMs),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("New Playback Duration:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = formatCentiseconds(totalStretchedDurationMs),
                                        color = NeonGreen,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!isSavingSpeed) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        isSavingSpeed = true
                                        speedSaveProgress = 0f
                                        coroutineScope.launch {
                                            for (i in 1..20) {
                                                delay(60)
                                                speedSaveProgress = i / 20f
                                            }
                                            val newUri = VideoMetadataManager.saveSpeedVideoAsNew(context, recording, speedSections)
                                            if (newUri != null) {
                                                Toast.makeText(context, "Speed-altered duplicate saved successfully!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Failed to clone speed-altered video file", Toast.LENGTH_SHORT).show()
                                            }
                                            isSavingSpeed = false
                                            showSpeedSaveDialog = false
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("save_speed_as_new_button")
                                ) {
                                    Text("Save as New Copy", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        showSpeedReplaceAlertConfirm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, GlowRecordRed),
                                    modifier = Modifier.fillMaxWidth().testTag("replace_speed_original_button")
                                ) {
                                    Text("Replace Original Source", color = GlowRecordRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    dismissButton = {
                        if (!isSavingSpeed) {
                            TextButton(onClick = { showSpeedSaveDialog = false }) {
                                Text("Go Back", color = TextWhite)
                            }
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            // REPLACE SPEED ORIGINAL ALERT CONFIRMATION DIALOG
            if (showSpeedReplaceAlertConfirm) {
                AlertDialog(
                    onDismissRequest = { if (!isSavingSpeed) showSpeedReplaceAlertConfirm = false },
                    title = {
                        Text(
                            text = "CONFIRM REPLACEMENT",
                            color = GlowRecordRed,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to replace the original video file with speed-altered parameters? This will apply the speed curves directly and cannot be undone.",
                            color = TextWhite.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSpeedReplaceAlertConfirm = false
                                isSavingSpeed = true
                                speedSaveProgress = 0f
                                coroutineScope.launch {
                                    for (i in 1..20) {
                                        delay(60)
                                        speedSaveProgress = i / 20f
                                    }
                                    VideoMetadataManager.saveSpeedSections(context, recording.uri, speedSections)
                                    Toast.makeText(context, "Original speed options overwritten successfully!", Toast.LENGTH_LONG).show()
                                    isSavingSpeed = false
                                    showSpeedSaveDialog = false
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlowRecordRed)
                        ) {
                            Text("Confirm Speed Overwrite", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSpeedReplaceAlertConfirm = false }) {
                            Text("Cancel", color = TextWhite)
                        }
                    },
                    containerColor = DeepCharcoal,
                    tonalElevation = 8.dp
                )
            }

            if (showGoProScreen) {
                GoProScreen(
                    context = context,
                    colors = com.example.ui.theme.LocalThemeColors.current,
                    featureName = activeGoProFeatureName,
                    onDismiss = { showGoProScreen = false },
                    onPurchaseSuccess = {
                        isPro = true
                        showGoProScreen = false
                    }
                )
            }

            // 4. Video Editor Trim Tip (first time on editor screen and Trim is active)
            if (activeTool == "Trim") {
                ContextualTipOverlay(
                    tip = ContextualTip.EDITOR_TRIM,
                    sharedPreferences = sharedPreferences,
                    alignment = Alignment.BottomCenter,
                    yOffset = (-180).dp,
                    arrowDirection = ArrowDirection.DOWN,
                    arrowOffsetX = 0.dp
                )
            }
        }
    }
}

// FORMAT MILLISECONDS TO MIN:SEC:CENTISEC FOR PROFESSIONAL TIME DESIGN
private fun formatCentiseconds(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    val centisecs = (ms % 1000) / 10 // Two digits representing centiseconds
    return String.format(java.util.Locale.US, "%02d:%02d.%02d", minutes, seconds, centisecs)
}

private fun formatSeconds(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatMergedSize(bytes: Long): String {
    if (bytes <= 0) return "0.0 MB"
    val k = 1024
    val m = bytes.toFloat() / (k * k)
    return String.format(java.util.Locale.US, "%.1f MB", m)
}

private fun getAudioFileName(context: android.content.Context, uri: android.net.Uri): String {
    var name = "User Soundtrack.mp3"
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx != -1) {
                    name = it.getString(idx)
                }
            }
        }
    } catch (e: Exception) {
        uri.path?.let { path ->
            val cut = path.lastIndexOf('/')
            if (cut != -1) {
                name = path.substring(cut + 1)
            }
        }
    }
    return name
}

private data class QuadMood(
    val name: String,
    val tracks: List<String>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

class MoodMelodySynthesizer {
    private var audioTrack: android.media.AudioTrack? = null
    var isPlaying = false
        private set
    private var synthThread: Thread? = null
    private var mood: String = "Chill"
    private var volume = 0.5f

    fun start(moodType: String) {
        stop()
        isPlaying = true
        mood = moodType
        val sampleRate = 22050
        val minBufferSize = android.media.AudioTrack.getMinBufferSize(
            sampleRate,
            android.media.AudioFormat.CHANNEL_OUT_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT
        )
        try {
            audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val audioFormat = android.media.AudioFormat.Builder()
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
                android.media.AudioTrack(
                    audioAttributes,
                    audioFormat,
                    minBufferSize.coerceAtLeast(4096),
                    android.media.AudioTrack.MODE_STREAM,
                    android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
                )
            } else {
                @Suppress("DEPRECATION")
                android.media.AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(4096),
                    android.media.AudioTrack.MODE_STREAM
                )
            }
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("VideoEditor", "Failed to start AudioTrack", e)
        }

        synthThread = Thread {
            var phase = 0.0
            val baseFreqs = when (mood) {
                "Upbeat/Energetic" -> doubleArrayOf(261.63, 329.63, 392.00, 493.88)
                "Chill/Relaxed" -> doubleArrayOf(220.00, 261.63, 329.63, 392.00)
                "Cinematic/Epic" -> doubleArrayOf(146.83, 220.00, 329.63, 440.00)
                "Funny/Quirky" -> doubleArrayOf(349.23, 440.00, 523.25, 659.25)
                else -> doubleArrayOf(220.00, 261.63, 329.63)
            }
            var freqIdx = 0
            var noteCount = 0L

            while (isPlaying) {
                val noteDurationSamples = sampleRate / 2
                val currentFreq = baseFreqs[freqIdx]
                
                val currentBuffer = ShortArray(1024)
                for (i in currentBuffer.indices) {
                    val sampleVal = Math.sin(phase) * 32767.0 * 0.15
                    currentBuffer[i] = sampleVal.toInt().toShort()
                    
                    phase += 2.0 * java.lang.Math.PI * currentFreq / sampleRate
                    if (phase > 2.0 * java.lang.Math.PI) {
                        phase -= 2.0 * java.lang.Math.PI
                    }
                    
                    noteCount++
                    if (noteCount >= noteDurationSamples) {
                        noteCount = 0
                        freqIdx = (freqIdx + 1) % baseFreqs.size
                    }
                }
                
                try {
                    audioTrack?.write(currentBuffer, 0, currentBuffer.size)
                } catch (e: Exception) {
                    break
                }
            }
        }
        synthThread?.start()
    }

    fun setVolume(vol: Float) {
        volume = vol
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                audioTrack?.setVolume(vol)
            } else {
                @Suppress("DEPRECATION")
                audioTrack?.setStereoVolume(vol, vol)
            }
        } catch (ignored: Exception) {}
    }

    fun stop() {
        isPlaying = false
        synthThread?.join(100)
        synthThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
    }
}

data class TextOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "Double-tap to Edit",
    val xPct: Float = 0.5f,
    val yPct: Float = 0.5f,
    val fontIndex: Int = 0,
    val sizeSp: Float = 22f,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val bgColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    val bgOpacity: Float = 0f,
    val alignment: Int = 1, // 0 = Left, 1 = Center, 2 = Right
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val hasShadow: Boolean = true,
    val startPct: Float = 0.0f,
    val endPct: Float = 1.0f
)

data class ClipTransition(
    val type: String = "none", // "none", "crossfade", "fade_black", "slide", "slide_right", "zoom", "wipe"
    val durationMs: Long = 1000L
)

