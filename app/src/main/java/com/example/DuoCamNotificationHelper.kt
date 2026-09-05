package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object DuoCamNotificationHelper {
    private const val TAG = "DuoCamNotification"
    
    const val CHANNEL_RECORDING = "duocam_recording_channel"
    const val CHANNEL_BACKUP = "duocam_backup_channel"
    const val CHANNEL_TIPS = "duocam_tips_channel"
    
    private val CREATIVE_TIPS = listOf(
        "Keep your device at eye level to maximize natural presenter contact.",
        "Ensure warm front-facing lightning for professional skin tones.",
        "Use dual-camera picture-in-picture mode to seamlessly sync reaction and detail views.",
        "Triple-check default storage availability and backup sync configuration beforehand.",
        "Optimize recording efficiency and save 50% more storage with HEVC/H.265 compression.",
        "Record in soft ambient light (such as near a window) for cinema-grade local contrast.",
        "Keep a steady pace and let the teleprompter speed flow naturally with your voice."
    )

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val channels = listOf(
                    NotificationChannel(
                        CHANNEL_RECORDING,
                        "Recordings & Alerts",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Notifications on recording status, complete sessions, and low storage."
                    },
                    NotificationChannel(
                        CHANNEL_BACKUP,
                        "Cloud Backups",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Notifications when Google Drive cloud sync or backup activities finish."
                    },
                    NotificationChannel(
                        CHANNEL_TIPS,
                        "Tips of the Day",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Daily insights, camera tips, and creative teleprompter tricks."
                    }
                )
                
                for (channel in channels) {
                    notificationManager.createNotificationChannel(channel)
                }
                Log.d(TAG, "Notification channels initialized successfully")
            }
        }
    }

    fun triggerRecordingCompletion(context: Context, videoName: String, duration: String) {
        val prefs = context.getSharedPreferences("duocam_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notify_recording_completion", true)
        if (!isEnabled) {
            Log.d(TAG, "Recording completion notification requested but disabled by user settings")
            return
        }

        try {
            createNotificationChannels(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_RECORDING)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Recording Captured Successfully")
                .setContentText("Saved video '$videoName' ($duration) to storage library.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify((1000..9999).random(), builder.build())
            Log.d(TAG, "Posted recording completion notification for: $videoName")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing to display notifications: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification: ${e.message}", e)
        }
    }

    fun triggerBackupCompletion(context: Context, videoName: String) {
        val prefs = context.getSharedPreferences("duocam_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notify_backup_completion", true)
        if (!isEnabled) {
            Log.d(TAG, "Backup completion notification requested but disabled by user settings")
            return
        }

        try {
            createNotificationChannels(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_BACKUP)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("Cloud Backup Success")
                .setContentText("'$videoName' synchronized to Google Drive secure cloud storage.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify((10000..19999).random(), builder.build())
            Log.d(TAG, "Posted backup completion notification for: $videoName")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing to display notifications: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification: ${e.message}", e)
        }
    }

    fun triggerLowStorageWarning(context: Context, freeSpaceMB: Long) {
        val prefs = context.getSharedPreferences("duocam_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notify_low_storage", true)
        if (!isEnabled) {
            Log.d(TAG, "Low storage warning notification requested but disabled by user settings")
            return
        }

        try {
            createNotificationChannels(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_RECORDING)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("DuoCam Storage Alert")
                .setContentText("Low space warning: Only $freeSpaceMB MB available on device storage.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(20002, builder.build())
            Log.d(TAG, "Posted low storage warning notification")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing to display notifications: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification: ${e.message}", e)
        }
    }

    fun triggerDailyTip(context: Context, force: Boolean = false): String {
        val prefs = context.getSharedPreferences("duocam_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notify_recording_tip_of_the_day", true)
        
        val randomTip = CREATIVE_TIPS.random()
        
        if (!isEnabled && !force) {
            Log.d(TAG, "Daily tip notification requested but disabled by setting")
            return randomTip
        }

        try {
            createNotificationChannels(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_TIPS)
                .setSmallIcon(android.R.drawable.btn_star_big_on)
                .setContentTitle("DuoCam Recording Tip of the Day")
                .setContentText(randomTip)
                .setStyle(NotificationCompat.BigTextStyle().bigText(randomTip))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(30003, builder.build())
            Log.d(TAG, "Posted Tip of the Day notification")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing to display notifications: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification: ${e.message}", e)
        }
        return randomTip
    }
}
