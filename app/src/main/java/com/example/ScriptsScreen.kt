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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.db.Script
import com.example.scripts.ScriptsViewModel

@Composable
internal fun ScriptsScreen(
    modifier: Modifier,
    onUseScript: (Script) -> Unit,
) {
    val viewModel: ScriptsViewModel = viewModel()
    val scripts by viewModel.scripts.collectAsStateWithLifecycle()
    val operationError by viewModel.operationError.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(false) }
    var editingScript by remember { mutableStateOf<Script?>(null) }
    var pendingDelete by remember { mutableStateOf<Script?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.tab_scripts),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.scripts_privacy_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            operationError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (scripts.isEmpty()) {
                Text(
                    stringResource(R.string.scripts_empty),
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
                                    TextButton(onClick = { onUseScript(script) }) {
                                        Text(stringResource(R.string.use))
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.clearError()
                                            editingScript = script
                                            showEditor = true
                                        },
                                    ) {
                                        Text(stringResource(R.string.edit))
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.clearError()
                                            pendingDelete = script
                                        },
                                    ) {
                                        Text(stringResource(R.string.delete))
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
                viewModel.clearError()
                editingScript = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.scripts_add))
        }
    }

    pendingDelete?.let { script ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.scripts_delete_title)) },
            text = {
                Text(stringResource(R.string.scripts_delete_message, script.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = script
                        pendingDelete = null
                        viewModel.delete(toDelete)
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

    if (showEditor) {
        ScriptEditorDialog(
            initial = editingScript,
            onDismiss = { showEditor = false },
            onSave = { title, content ->
                viewModel.save(editingScript, title, content) {
                    showEditor = false
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
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.scripts_new else R.string.scripts_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.scripts_title_label)) },
                    singleLine = true,
                )
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.scripts_content_label)) },
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
