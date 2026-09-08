package com.example.gallery

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class RecordingItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
)

internal class RecordingRepository(
    private val context: Context,
) {

    suspend fun queryOwnedRecordings(): List<RecordingItem> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Video.Media.DATA} LIKE ?"
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("Movies/DuoCam/%")
        } else {
            arrayOf("%/Movies/DuoCam/%")
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
        )
        resolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        RecordingItem(
                            uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)),
                            name = cursor.getString(nameColumn) ?: "Unknown",
                            durationMs = cursor.getLong(durationColumn),
                            sizeBytes = cursor.getLong(sizeColumn),
                            mimeType = cursor.getString(mimeColumn) ?: "video/mp4",
                        ),
                    )
                }
            }
        } ?: emptyList()
    }

    suspend fun deleteRecording(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        context.contentResolver.delete(uri, null, null) > 0
    }

    fun shareRecording(recording: RecordingItem): Result<Unit> {
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = recording.mimeType
                putExtra(Intent.EXTRA_STREAM, recording.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share recording").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
