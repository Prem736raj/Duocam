package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gallery.GalleryViewModel
import com.example.gallery.RecordingItem

@Composable
internal fun GalleryScreen(modifier: Modifier) {
    val context = LocalContext.current
    val viewModel: GalleryViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedRecording by remember { mutableStateOf<RecordingItem?>(null) }
    var pendingDelete by remember { mutableStateOf<RecordingItem?>(null) }

    val legacyStorageRequired = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
    var hasStoragePermission by remember {
        mutableStateOf(
            !legacyStorageRequired || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasStoragePermission = granted
        if (granted) {
            viewModel.refresh()
        } else {
            viewModel.setPermissionDeniedMessage()
        }
    }

    fun refresh() {
        if (legacyStorageRequired && !hasStoragePermission) {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            return
        }
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.gallery_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.gallery_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { refresh() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.gallery_refresh),
                )
            }
        }
        uiState.message?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        if (uiState.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.recordings.isEmpty()) {
            Text(
                stringResource(R.string.gallery_empty),
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
                items(uiState.recordings, key = { it.uri.toString() }) { recording ->
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
                                contentDescription = stringResource(
                                    R.string.gallery_play,
                                    recording.name,
                                ),
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
                            IconButton(onClick = { viewModel.shareRecording(recording) }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(
                                        R.string.gallery_share,
                                        recording.name,
                                    ),
                                )
                            }
                            IconButton(onClick = { pendingDelete = recording }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(
                                        R.string.gallery_delete,
                                        recording.name,
                                    ),
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
            title = { Text(stringResource(R.string.gallery_delete_title)) },
            text = { Text(stringResource(R.string.gallery_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = recording
                        pendingDelete = null
                        viewModel.deleteRecording(toDelete)
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaybackDialog(recording: RecordingItem, onDismiss: () -> Unit) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(recording.uri) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

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
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setMediaController(MediaController(context))
                            setVideoURI(recording.uri)
                            setOnPreparedListener { start() }
                            videoViewRef = this
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

private fun formatGalleryDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    if (bytes < 1024L * 1024L) return "${bytes / 1024L} KB"
    return "%.1f MB".format(bytes.toDouble() / (1024L * 1024L))
}
