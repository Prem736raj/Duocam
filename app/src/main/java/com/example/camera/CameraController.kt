package com.example.camera

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

enum class CaptureMode {
    SINGLE,
    DUAL,
}

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isFinalizing: Boolean = false,
    val durationMs: Long = 0L,
    val savedUri: Uri? = null,
    val errorMessage: String? = null,
)

class CameraController(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onRecordingStateChanged: (RecordingUiState) -> Unit,
    private val onModeChanged: (CaptureMode, String?) -> Unit,
) {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)
    private var provider: ProcessCameraProvider? = null
    private var providerFuture = ProcessCameraProvider.getInstance(appContext)
    private var recorder: Recorder? = null
    private var activeRecording: Recording? = null
    private var activeOutputUri: Uri? = null
    private var bindGeneration = 0
    private var released = false
    private var actualMode = CaptureMode.SINGLE

    fun bind(capabilities: CameraCapabilities, requestedMode: CaptureMode) {
        if (released) return
        val generation = ++bindGeneration
        providerFuture.addListener(
            {
                if (released || generation != bindGeneration) return@addListener
                try {
                    val cameraProvider = providerFuture.get()
                    provider = cameraProvider
                    bindWithProvider(cameraProvider, capabilities, requestedMode)
                } catch (error: Throwable) {
                    onModeChanged(
                        CaptureMode.SINGLE,
                        error.message ?: "Camera initialization failed.",
                    )
                    onRecordingStateChanged(
                        RecordingUiState(errorMessage = "Camera initialization failed."),
                    )
                }
            },
            mainExecutor,
        )
    }

    private fun bindWithProvider(
        cameraProvider: ProcessCameraProvider,
        capabilities: CameraCapabilities,
        requestedMode: CaptureMode,
    ) {
        cameraProvider.unbindAll()
        recorder = null

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
        )
        val newRecorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        val capture = VideoCapture.withOutput(newRecorder)
        recorder = newRecorder
        val concurrentSelectors = capabilities.concurrentCameraSelectors

        if (requestedMode == CaptureMode.DUAL && concurrentSelectors != null) {
            try {
                val sharedGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(capture)
                    .build()
                val fullFrame = CompositionSettings.Builder()
                    .setAlpha(1f)
                    .setOffset(0f, 0f)
                    .setScale(1f, 1f)
                    .build()
                val pictureInPicture = CompositionSettings.Builder()
                    .setAlpha(1f)
                    .setOffset(0.62f, -0.62f)
                    .setScale(0.34f, 0.34f)
                    .build()

                cameraProvider.bindToLifecycle(
                    listOf(
                        SingleCameraConfig(
                            concurrentSelectors.first,
                            sharedGroup,
                            fullFrame,
                            lifecycleOwner,
                        ),
                        SingleCameraConfig(
                            concurrentSelectors.second,
                            sharedGroup,
                            pictureInPicture,
                            lifecycleOwner,
                        ),
                    ),
                )
                actualMode = CaptureMode.DUAL
                onModeChanged(
                    CaptureMode.DUAL,
                    "Dual-camera picture-in-picture recording is active.",
                )
                return
            } catch (error: Throwable) {
                Log.e(TAG, "Concurrent camera binding failed; using single camera.", error)
                cameraProvider.unbindAll()
                onModeChanged(
                    CaptureMode.SINGLE,
                    "Dual-camera recording is unavailable right now; single-camera recording is ready.",
                )
            }
        }

        val selector = capabilities.backCameraSelector
            ?: capabilities.frontCameraSelector
            ?: CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            actualMode = CaptureMode.SINGLE
            onModeChanged(CaptureMode.SINGLE, null)
        } catch (error: Throwable) {
            Log.e(TAG, "Single camera binding failed.", error)
            onModeChanged(CaptureMode.SINGLE, "Camera unavailable.")
            onRecordingStateChanged(
                RecordingUiState(errorMessage = "Camera unavailable: ${error.message ?: "unknown error"}"),
            )
        }
    }

    fun startRecording(audioRequested: Boolean) {
        val currentRecorder = recorder ?: run {
            publishError("Camera is not ready yet.")
            return
        }
        if (activeRecording != null) return

        val outputUri = createOutputUri(actualMode)
        if (outputUri == null) {
            publishError("Unable to create a video file in Movies/DuoCam.")
            return
        }
        activeOutputUri = outputUri
        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            outputUri,
        ).build()
        var pendingRecording = currentRecorder.prepareRecording(appContext, outputOptions)
        val audioAllowed = audioRequested &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        if (audioAllowed) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }

        onRecordingStateChanged(RecordingUiState())
        activeRecording = pendingRecording.start(mainExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    onRecordingStateChanged(RecordingUiState(isRecording = true))
                }

                is VideoRecordEvent.Status -> {
                    onRecordingStateChanged(
                        RecordingUiState(
                            isRecording = true,
                            durationMs = event.recordingStats.recordedDurationNanos / NANOS_PER_MILLISECOND,
                        ),
                    )
                }

                is VideoRecordEvent.Finalize -> finalizeRecording(event)
            }
        }
    }

    fun stopRecording() {
        activeRecording?.let {
            onRecordingStateChanged(RecordingUiState(isFinalizing = true))
            it.stop()
        }
    }

    fun cancelRecording() {
        activeRecording?.close()
    }

    fun release() {
        if (released) return
        released = true
        activeRecording?.stop()
        activeRecording = null
        provider?.unbindAll()
        provider = null
    }

    private fun finalizeRecording(event: VideoRecordEvent.Finalize) {
        val outputUri = event.outputResults.outputUri.takeUnless { it == Uri.EMPTY } ?: activeOutputUri
        activeRecording = null
        activeOutputUri = null
        if (event.hasError() || outputUri == null || !hasMediaContent(outputUri)) {
            outputUri?.let { contentResolver.delete(it, null, null) }
            publishError(
                event.cause?.message
                    ?: "The recording could not be finalized. No video was saved.",
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val finalized = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            val updated = contentResolver.update(outputUri, finalized, null, null)
            if (updated <= 0) {
                contentResolver.delete(outputUri, null, null)
                publishError("The recording was created but could not be finalized in the gallery.")
                return
            }
        }
        onRecordingStateChanged(
            RecordingUiState(
                savedUri = outputUri,
                durationMs = event.recordingStats.recordedDurationNanos / NANOS_PER_MILLISECOND,
            ),
        )
    }

    private fun createOutputUri(mode: CaptureMode): Uri? {
        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                "DuoCam_${System.currentTimeMillis()}_${mode.name.lowercase()}.mp4",
            )
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DuoCam")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        return runCatching { contentResolver.insert(collection, values) }.getOrNull()
    }

    private fun hasMediaContent(uri: Uri): Boolean {
        val size = contentResolver.query(
            uri,
            arrayOf(MediaStore.Video.Media.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        } ?: 0L
        return size > 0L
    }

    private fun publishError(message: String) {
        onRecordingStateChanged(RecordingUiState(errorMessage = message))
    }

    companion object {
        private const val TAG = "DuoCamCameraController"
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
