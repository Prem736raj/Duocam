package com.example.camera

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
    private val onCameraReadyChanged: (Boolean) -> Unit,
) {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    private val providerFuture = ProcessCameraProvider.getInstance(appContext)

    private var provider: ProcessCameraProvider? = null
    private var recorder: Recorder? = null
    private var activeRecording: Recording? = null
    private var bindGeneration = 0
    private var released = false
    private var actualMode = CaptureMode.SINGLE
    private var lastDurationMs = 0L

    fun bind(
        capabilities: CameraCapabilities,
        requestedMode: CaptureMode,
    ) {
        if (released || activeRecording != null) return

        val generation = ++bindGeneration
        recorder = null
        emitCameraReady(false)

        providerFuture.addListener(
            {
                if (released || generation != bindGeneration) {
                    return@addListener
                }

                try {
                    val cameraProvider = providerFuture.get()
                    provider = cameraProvider
                    bindWithProvider(
                        cameraProvider = cameraProvider,
                        capabilities = capabilities,
                        requestedMode = requestedMode,
                    )
                } catch (error: Throwable) {
                    Log.e(TAG, "Camera provider initialization failed.", error)
                    onModeChangedIfActive(
                        CaptureMode.SINGLE,
                        error.message ?: "Camera initialization failed.",
                    )
                    emitState(
                        RecordingUiState(
                            errorMessage = "Camera initialization failed.",
                        ),
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
        emitCameraReady(false)

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
        )

        val newRecorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        val videoCapture = VideoCapture.withOutput(newRecorder)
        val selectors = capabilities.concurrentCameraSelectors

        if (requestedMode == CaptureMode.DUAL && selectors != null) {
            try {
                val sharedGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(videoCapture)
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
                            selectors.first,
                            sharedGroup,
                            fullFrame,
                            lifecycleOwner,
                        ),
                        SingleCameraConfig(
                            selectors.second,
                            sharedGroup,
                            pictureInPicture,
                            lifecycleOwner,
                        ),
                    ),
                )

                recorder = newRecorder
                actualMode = CaptureMode.DUAL
                emitCameraReady(true)
                onModeChangedIfActive(
                    CaptureMode.DUAL,
                    "Dual-camera picture-in-picture recording is active.",
                )
                return
            } catch (error: Throwable) {
                Log.e(TAG, "Concurrent bind failed; using single camera.", error)
                cameraProvider.unbindAll()
                onModeChangedIfActive(
                    CaptureMode.SINGLE,
                    "Dual-camera recording is unavailable right now; " +
                        "single-camera recording is ready.",
                )
            }
        }

        val selector = capabilities.backCameraSelector
            ?: capabilities.frontCameraSelector
            ?: CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                videoCapture,
            )

            recorder = newRecorder
            actualMode = CaptureMode.SINGLE
            emitCameraReady(true)
            onModeChangedIfActive(CaptureMode.SINGLE, null)
        } catch (error: Throwable) {
            recorder = null
            emitCameraReady(false)
            Log.e(TAG, "Single camera binding failed.", error)
            onModeChangedIfActive(CaptureMode.SINGLE, "Camera unavailable.")
            emitState(
                RecordingUiState(
                    errorMessage = "Camera unavailable: " +
                        (error.message ?: "unknown error"),
                ),
            )
        }
    }

    fun startRecording(audioRequested: Boolean) {
        if (released) return

        val currentRecorder = recorder ?: run {
            publishError("Camera is not ready yet.")
            return
        }

        if (activeRecording != null) return

        val target = RecordingMediaStore.createTarget(actualMode)

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            target.collectionUri,
        )
            .setContentValues(target.contentValues)
            .build()

        val pendingRecording = try {
            var pending = currentRecorder.prepareRecording(
                appContext,
                outputOptions,
            )

            val audioAllowed = audioRequested &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED

            if (audioAllowed) {
                pending = pending.withAudioEnabled()
            }

            pending
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to prepare recording.", error)
            publishError(error.message ?: "Unable to prepare recording.")
            return
        }

        lastDurationMs = 0L
        emitState(RecordingUiState())

        try {
            activeRecording = pendingRecording.start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        emitState(
                            RecordingUiState(
                                isRecording = true,
                                durationMs = lastDurationMs,
                            ),
                        )
                    }

                    is VideoRecordEvent.Status -> {
                        lastDurationMs = event.recordingStats.recordedDurationNanos /
                            NANOS_PER_MILLISECOND
                        emitState(
                            RecordingUiState(
                                isRecording = true,
                                durationMs = lastDurationMs,
                            ),
                        )
                    }

                    is VideoRecordEvent.Finalize -> finalizeRecording(event)
                }
            }
        } catch (error: Throwable) {
            activeRecording = null
            Log.e(TAG, "Unable to start recording.", error)
            publishError(error.message ?: "Unable to start recording.")
        }
    }

    fun stopRecording() {
        val recording = activeRecording ?: return
        emitState(
            RecordingUiState(
                isFinalizing = true,
                durationMs = lastDurationMs,
            ),
        )
        recording.stop()
    }

    fun cancelRecording() {
        activeRecording?.close()
    }

    fun release() {
        if (released) return
        released = true
        ++bindGeneration
        recorder = null

        if (activeRecording != null) {
            activeRecording?.stop()
        } else {
            completeRelease()
        }
    }

    private fun finalizeRecording(event: VideoRecordEvent.Finalize) {
        val outputUri = event.outputResults.outputUri
        activeRecording = null

        if (!released) {
            if (event.hasError() || outputUri == Uri.EMPTY) {
                publishError(
                    event.cause?.message
                        ?: "The recording could not be finalized. No video was saved.",
                )
            } else {
                emitState(
                    RecordingUiState(
                        savedUri = outputUri,
                        durationMs = event.recordingStats.recordedDurationNanos /
                            NANOS_PER_MILLISECOND,
                    ),
                )
            }
        }

        if (released) {
            completeRelease()
        }
    }

    private fun completeRelease() {
        runCatching { provider?.unbindAll() }
            .onFailure { Log.w(TAG, "Camera unbind during release failed.", it) }

        provider = null
        recorder = null
    }

    private fun publishError(message: String) {
        emitState(RecordingUiState(errorMessage = message))
    }

    private fun emitState(state: RecordingUiState) {
        if (!released) onRecordingStateChanged(state)
    }

    private fun emitCameraReady(ready: Boolean) {
        if (!released) onCameraReadyChanged(ready)
    }

    private fun onModeChangedIfActive(
        mode: CaptureMode,
        message: String?,
    ) {
        if (!released) onModeChanged(mode, message)
    }

    companion object {
        private const val TAG = "DuoCamCameraController"
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
