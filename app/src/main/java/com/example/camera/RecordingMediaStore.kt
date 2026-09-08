package com.example.camera

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

internal data class RecordingTarget(
    val collectionUri: Uri,
    val contentValues: ContentValues,
)

internal object RecordingMediaStore {

    fun createTarget(
        mode: CaptureMode,
        timestampMs: Long = System.currentTimeMillis(),
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): RecordingTarget {
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                "DuoCam_${timestampMs}_${mode.name.lowercase()}.mp4",
            )
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, timestampMs / 1000L)

            if (sdkInt >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "Movies/DuoCam",
                )
            }
        }

        val collectionUri =
            if (sdkInt >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        return RecordingTarget(
            collectionUri = collectionUri,
            contentValues = contentValues,
        )
    }
}
