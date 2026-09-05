package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class DuoCamRecordingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var mediaRecorder: MediaRecorder? = null
    
    companion object {
        private const val TAG = "DuoCamRecordingService"
        
        const val CHANNEL_ID = "duocam_active_recording_channel"
        const val NOTIFICATION_ID = 2026
        
        const val ACTION_START = "com.example.duocam.action.START"
        const val ACTION_PAUSE = "com.example.duocam.action.PAUSE"
        const val ACTION_RESUME = "com.example.duocam.action.RESUME"
        const val ACTION_STOP = "com.example.duocam.action.STOP"
        
        // Static state variables for Jetpack Compose reactivity (single-process)
        val isServiceActive = mutableStateOf(false)
        val isRecording = mutableStateOf(false)
        val isPaused = mutableStateOf(false)
        val elapsedMillis = mutableStateOf(0L)
        val activeAudioFile = mutableStateOf<File?>(null)
        val currentRecordingStartedTime = mutableStateOf(0L)
        
        var onRecordingStoppedExternally: (() -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        isServiceActive.value = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        Log.d(TAG, "onStartCommand with Action: $action")
        
        when (action) {
            ACTION_START -> {
                val noiseReduction = intent.getBooleanExtra("noise_reduction", false)
                startSession(noiseReduction)
            }
            ACTION_PAUSE -> {
                pauseSession()
            }
            ACTION_RESUME -> {
                resumeSession()
            }
            ACTION_STOP -> {
                stopSession()
            }
        }
        
        return START_STICKY
    }

    private fun startSession(noiseReduction: Boolean) {
        if (isRecording.value) return
        
        isRecording.value = true
        isPaused.value = false
        elapsedMillis.value = 0L
        currentRecordingStartedTime.value = System.currentTimeMillis()
        
        // Start real background persistent audio support
        try {
            val file = File(cacheDir, "duocam_audio_${System.currentTimeMillis()}.mp4")
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            
            val source = if (noiseReduction) {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            } else {
                MediaRecorder.AudioSource.MIC
            }
            recorder.setAudioSource(source)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            
            recorder.prepare()
            recorder.start()
            
            mediaRecorder = recorder
            activeAudioFile.value = file
            Log.d(TAG, "Audio MediaRecorder started in Foreground Service: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder in Foreground Service: ${e.message}", e)
            activeAudioFile.value = null
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                buildLiveNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, buildLiveNotification())
        }
        startTimer()
    }

    private fun pauseSession() {
        if (!isRecording.value || isPaused.value) return
        isPaused.value = true
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                Log.d(TAG, "MediaRecorder successfully paused")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause MediaRecorder: ${e.message}")
        }
        
        stopTimer()
        updateNotification()
    }

    private fun resumeSession() {
        if (!isRecording.value || !isPaused.value) return
        isPaused.value = false
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                Log.d(TAG, "MediaRecorder successfully resumed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume MediaRecorder: ${e.message}")
        }
        
        startTimer()
        updateNotification()
    }

    private fun stopSession() {
        if (!isRecording.value) return
        
        stopTimer()
        
        try {
            mediaRecorder?.let { recorder ->
                recorder.stop()
                recorder.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop/release MediaRecorder inside service stopSession: ${e.message}")
        }
        mediaRecorder = null
        
        isRecording.value = false
        isPaused.value = false
        isServiceActive.value = false
        
        // Invoke trigger on main UI if active
        onRecordingStoppedExternally?.invoke()
        
        stopForeground(true)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        val baseStart = System.currentTimeMillis() - elapsedMillis.value
        
        timerJob = serviceScope.launch {
            while (isRecording.value && !isPaused.value) {
                elapsedMillis.value = System.currentTimeMillis() - baseStart
                updateNotification()
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildLiveNotification())
    }

    private fun buildLiveNotification(): Notification {
        val formattedTime = formatNotificationDuration(elapsedMillis.value)
        val titleText = if (isPaused.value) "DuoCam Video Paused" else "DuoCam Video Recording..."
        val statusText = if (isPaused.value) "Paused • $formattedTime" else "Recording Active • $formattedTime"
        
        // Open main activity when tapping notification
        val resultIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Actions mapping
        val pauseResumeAction = if (isPaused.value) {
            val resumeIntent = Intent(this, DuoCamRecordingService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = PendingIntent.getService(
                this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play, "Resume", resumePendingIntent
            ).build()
        } else {
            val pauseIntent = Intent(this, DuoCamRecordingService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(
                this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, "Pause", pausePendingIntent
            ).build()
        }
        
        val stopIntent = Intent(this, DuoCamRecordingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent
        ).build()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle(titleText)
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Unobtrusive background recording
            .setShowWhen(false)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .setColor(0xFFFF2E56.toInt()) // DuoCam Red theme
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Active Background Recording Sessions",
                    NotificationManager.IMPORTANCE_LOW // Clear, unobtrusive status indicators
                ).apply {
                    description = "Silent foreground notification to keep recording active in background."
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun formatNotificationDuration(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isServiceActive.value = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
