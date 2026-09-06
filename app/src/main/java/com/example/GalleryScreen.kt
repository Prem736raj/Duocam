package com.example

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RecordingItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
)

@Composable
internal fun GalleryScreen(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recordings by remember { mutableStateOf<List<RecordingItem>>(emptyList()) }
    var selectedRecording by remember { mutableStateOf<RecordingItem?>(null) }
    var pendingDelete by remember { mutableStateOf<RecordingItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            recordings = withContext(Dispatchers.IO) { queryOwnedRecordings(context) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Local gallery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Only recordings created in Movies/DuoCam are shown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Default.Home, contentDescription = "Refresh gallery")
            }
        }
        message?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (recordings.isEmpty()) {
            Text(
                "Your saved recordings will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recordings, key = { it.uri.toString() }) { recording ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRecording = recording },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play ${recording.name}",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                            ) {
                                Text(recording.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${formatGalleryDuration(recording.durationMs)} • ${formatBytes(recording.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { shareRecording(context, recording) }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share ${recording.name}",
                                )
                            }
                            IconButton(onClick = { pendingDelete = recording }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${recording.name}",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedRecording?.let { recording ->
        PlaybackDialog(recording = recording, onDismiss = { selectedRecording = null })
    }
    pendingDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete recording?") },
            text = { Text("This removes the local media file. The action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        scope.launch(Dispatchers.IO) {
                            val deleted = context.contentResolver.delete(recording.uri, null, null)
                            withContext(Dispatchers.Main) {
                                if (deleted > 0) {
                                    recordings = recordings.filterNot { it.uri == recording.uri }
                                } else {
                                    message = "The recording could not be deleted."
                                }
                            }
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PlaybackDialog(recording: RecordingItem, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(recording.name, fontWeight = FontWeight.Bold, maxLines = 1)
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setMediaController(MediaController(context))
                            setVideoURI(recording.uri)
                            setOnPreparedListener { start() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
        }
    }
}

private fun queryOwnedRecordings(context: Context): List<RecordingItem> {
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
    return resolver.query(
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
                        name = cursor.getString(nameColumn),
                        durationMs = cursor.getLong(durationColumn),
                        sizeBytes = cursor.getLong(sizeColumn),
                        mimeType = cursor.getString(mimeColumn) ?: "video/mp4",
                    ),
                )
            }
        }
    } ?: emptyList()
}

private fun shareRecording(context: Context, recording: RecordingItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = recording.mimeType
        putExtra(Intent.EXTRA_STREAM, recording.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share recording"))
}

private fun formatGalleryDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    if (bytes < 1024L * 1024L) return "${bytes / 1024L} KB"
    return "%.1f MB".format(bytes.toDouble() / (1024L * 1024L))
}
