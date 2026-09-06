package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.db.Script
import com.example.db.ScriptDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ScriptsScreen(
    modifier: Modifier,
    onUseScript: (Script) -> Unit,
) {
    val context = LocalContext.current
    val database = remember(context) { ScriptDatabase.getDatabase(context) }
    val scripts by database.scriptDao().getAllScripts().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var editingScript by remember { mutableStateOf<Script?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                "Scripts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Scripts stay on this device and can be displayed over a real camera recording.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            if (scripts.isEmpty()) {
                Text(
                    "No scripts yet. Add one to use the teleprompter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(scripts, key = { it.id }) { script ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(script.title, fontWeight = FontWeight.Bold)
                                Text(
                                    script.content,
                                    maxLines = 3,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = { onUseScript(script) }) { Text("Use") }
                                    TextButton(
                                        onClick = {
                                            editingScript = script
                                            showEditor = true
                                        },
                                    ) {
                                        Text("Edit")
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                database.scriptDao().deleteScript(script)
                                            }
                                        },
                                    ) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
                editingScript = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add script")
        }
    }

    if (showEditor) {
        ScriptEditorDialog(
            initial = editingScript,
            onDismiss = { showEditor = false },
            onSave = { title, content ->
                scope.launch(Dispatchers.IO) {
                    val current = editingScript
                    if (current == null) {
                        database.scriptDao().insertScript(
                            Script(title = title, content = content),
                        )
                    } else {
                        database.scriptDao().updateScript(
                            current.copy(
                                title = title,
                                content = content,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    }
                    withContext(Dispatchers.Main) { showEditor = false }
                }
            },
        )
    }
}

@Composable
private fun ScriptEditorDialog(
    initial: Script?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial) { mutableStateOf(initial?.content.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New script" else "Edit script") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                )
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Script text") },
                    minLines = 6,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), content.trim()) },
                enabled = title.isNotBlank() && content.isNotBlank(),
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
