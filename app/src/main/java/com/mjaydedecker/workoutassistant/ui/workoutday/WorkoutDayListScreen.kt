package com.mjaydedecker.workoutassistant.ui.workoutday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDayListScreen(
    app: WorkoutAssistantApp,
    onDaySelected: (Long) -> Unit,
    onStartSession: (Long) -> Unit
) {
    val viewModel: WorkoutDayListViewModel = viewModel(
        factory = WorkoutDayListViewModelFactory(app.workoutDayRepository)
    )
    val days by viewModel.workoutDays.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()
    val pendingRename by viewModel.pendingRename.collectAsState()
    val showCreate by viewModel.showCreateDialog.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Workout Days") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreate() }) {
                Icon(Icons.Default.Add, contentDescription = "Add workout day")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (days.isEmpty()) {
                Text(
                    "No workout days yet. Tap + to add one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(days, key = { it.id }) { day ->
                        ListItem(
                            headlineContent = { Text(day.name) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onStartSession(day.id) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Start ${day.name}")
                                    }
                                    IconButton(onClick = { viewModel.requestRename(day) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename ${day.name}")
                                    }
                                    IconButton(onClick = { viewModel.requestDelete(day) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete ${day.name}")
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onDaySelected(day.id) }
                        )
                    }
                }
            }
        }

        if (showCreate) {
            CreateDayDialog(
                onConfirm = { viewModel.create(it) },
                onDismiss = { viewModel.hideCreate() }
            )
        }

        pendingRename?.let { day ->
            RenameDayDialog(
                currentName = day.name,
                onConfirm = { viewModel.confirmRename(it) },
                onDismiss = { viewModel.cancelRename() }
            )
        }

        pendingDelete?.let { day ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("Delete Workout Day") },
                text = { Text("\"${day.name}\" has existing workout sessions. Deleting it will not affect your history, but you will no longer be able to start this workout. Continue?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun RenameDayDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Workout Day") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateDayDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Workout Day") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
