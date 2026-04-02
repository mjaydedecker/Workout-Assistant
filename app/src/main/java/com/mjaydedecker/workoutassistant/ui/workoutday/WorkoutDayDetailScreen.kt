package com.mjaydedecker.workoutassistant.ui.workoutday

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp
import com.mjaydedecker.workoutassistant.data.model.WorkoutDayExerciseItem
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutDayDetailScreen(
    app: WorkoutAssistantApp,
    workoutDayId: Long,
    onNavigateBack: () -> Unit
) {
    val viewModel: WorkoutDayDetailViewModel = viewModel(
        factory = WorkoutDayDetailViewModelFactory(
            app.workoutDayRepository,
            app.exerciseRepository,
            workoutDayId
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    var exercises by remember(uiState.exercises) { mutableStateOf(uiState.exercises) }
    var pendingRemove by remember { mutableStateOf<WorkoutDayExerciseItem?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        exercises = exercises.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    LaunchedEffect(reorderState) {
        snapshotFlow { reorderState.isAnyItemDragging }
            .drop(1)
            .filter { !it }
            .collect { viewModel.onReorder(exercises.map { it.assignmentId }) }
    }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.workoutDay?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddExercise() }) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise to day")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(exercises, key = { it.assignmentId }) { item ->
                    ReorderableItem(reorderState, key = item.assignmentId) { _ ->
                        ListItem(
                            headlineContent = { Text(item.exercise.name) },
                            supportingContent = { Text("${item.exercise.defaultSets} sets") },
                            leadingContent = {
                                IconButton(
                                    modifier = Modifier.draggableHandle(),
                                    onClick = {}
                                ) {
                                    Icon(Icons.Default.DragHandle, contentDescription = "Reorder ${item.exercise.name}")
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { pendingRemove = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove ${item.exercise.name}")
                                }
                            }
                        )
                    }
                }
            }

            if (exercises.isEmpty()) {
                Text(
                    "No exercises assigned. Tap + to add one.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        pendingRemove?.let { item ->
            AlertDialog(
                onDismissRequest = { pendingRemove = null },
                title = { Text("Remove Exercise") },
                text = { Text("Remove \"${item.exercise.name}\" from this workout day?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeExercise(item.assignmentId)
                        pendingRemove = null
                    }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRemove = null }) { Text("Cancel") }
                }
            )
        }

        if (uiState.showAddExerciseSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideAddExercise() },
                sheetState = sheetState
            ) {
                val assignedIds = uiState.exercises.map { it.exercise.id }.toSet()
                val available = uiState.allExercises.filter { it.id !in assignedIds }
                if (available.isEmpty()) {
                    Text(
                        "All exercises are already in this workout day.",
                        modifier = Modifier.padding(PaddingValues(16.dp))
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(available, key = { it.id }) { exercise ->
                            ListItem(
                                headlineContent = { Text(exercise.name) },
                                supportingContent = { Text("${exercise.defaultSets} sets") },
                                modifier = Modifier.clickable {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        viewModel.addExercise(exercise.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
