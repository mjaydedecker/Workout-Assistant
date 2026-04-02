package com.mjaydedecker.workoutassistant.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    app: WorkoutAssistantApp,
    onAddExercise: () -> Unit,
    onEditExercise: (Long) -> Unit
) {
    val viewModel: ExerciseListViewModel = viewModel(
        factory = ExerciseListViewModelFactory(app.exerciseRepository)
    )
    val exercises by viewModel.exercises.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Exercises") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExercise) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (exercises.isEmpty()) {
                Text(
                    "No exercises yet. Tap + to add one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = { Text("${exercise.defaultSets} sets") },
                            trailingContent = {
                                IconButton(onClick = { viewModel.requestDelete(exercise) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete ${exercise.name}"
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onEditExercise(exercise.id) }
                        )
                    }
                }
            }
        }

        pendingDelete?.let { exercise ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("Delete Exercise") },
                text = { Text("\"${exercise.name}\" is assigned to one or more workout days. Deleting it will remove it from those days. Continue?") },
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
