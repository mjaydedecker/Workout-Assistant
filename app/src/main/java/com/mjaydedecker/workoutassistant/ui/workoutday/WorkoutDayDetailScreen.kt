package com.mjaydedecker.workoutassistant.ui.workoutday

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp
import com.mjaydedecker.workoutassistant.data.model.WorkoutDayExerciseItem
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutDayDetailScreen(
    app: WorkoutAssistantApp,
    workoutDayId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToLibrary: ((workoutDayId: Long) -> Unit)? = null
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
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }

    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) {
            titleInput = uiState.workoutDay?.name ?: ""
        }
    }

    // Edit-sets dialog state
    var editSetsItem by remember { mutableStateOf<WorkoutDayExerciseItem?>(null) }
    var editSetsInput by remember { mutableStateOf("") }
    var editSetsError by remember { mutableStateOf<String?>(null) }

    // Add custom exercise with sets
    var addCustomExerciseId by remember { mutableStateOf<Long?>(null) }
    var addSetsInput by remember { mutableStateOf("3") }
    var addSetsError by remember { mutableStateOf<String?>(null) }

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
                title = {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (titleInput.isNotBlank()) viewModel.renameDay(titleInput)
                                isEditingTitle = false
                            })
                        )
                    } else {
                        Text(uiState.workoutDay?.name ?: "")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditingTitle) isEditingTitle = false else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditingTitle) {
                        IconButton(
                            onClick = {
                                if (titleInput.isNotBlank()) viewModel.renameDay(titleInput)
                                isEditingTitle = false
                            },
                            enabled = titleInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save name")
                        }
                    } else {
                        IconButton(onClick = { isEditingTitle = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename workout day")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (onNavigateToLibrary != null) {
                    onNavigateToLibrary(workoutDayId)
                } else {
                    viewModel.showAddExercise()
                }
            }) {
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
                            supportingContent = { Text("${item.sets} sets") },
                            leadingContent = {
                                IconButton(
                                    modifier = Modifier.draggableHandle(),
                                    onClick = {}
                                ) {
                                    Icon(Icons.Default.DragHandle, contentDescription = "Reorder ${item.exercise.name}")
                                }
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        editSetsItem = item
                                        editSetsInput = item.sets.toString()
                                        editSetsError = null
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit sets for ${item.exercise.name}")
                                    }
                                    IconButton(onClick = { pendingRemove = item }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove ${item.exercise.name}")
                                    }
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

        // Remove exercise confirmation dialog
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

        // Edit sets dialog
        editSetsItem?.let { item ->
            AlertDialog(
                onDismissRequest = { editSetsItem = null; editSetsError = null },
                title = { Text("Edit Sets") },
                text = {
                    Column {
                        Text("Sets for \"${item.exercise.name}\":")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editSetsInput,
                            onValueChange = { editSetsInput = it; editSetsError = null },
                            label = { Text("Sets") },
                            isError = editSetsError != null,
                            supportingText = editSetsError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val sets = editSetsInput.toIntOrNull()
                        if (sets == null || sets < 1) {
                            editSetsError = "Must be at least 1"
                        } else {
                            viewModel.updateSets(item.assignmentId, sets)
                            editSetsItem = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editSetsItem = null; editSetsError = null }) { Text("Cancel") }
                }
            )
        }

        // Custom exercise add-sets dialog (when adding a custom exercise from the sheet)
        addCustomExerciseId?.let { exerciseId ->
            AlertDialog(
                onDismissRequest = { addCustomExerciseId = null; addSetsError = null },
                title = { Text("Number of Sets") },
                text = {
                    Column {
                        Text("How many sets for this exercise?")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = addSetsInput,
                            onValueChange = { addSetsInput = it; addSetsError = null },
                            label = { Text("Sets") },
                            isError = addSetsError != null,
                            supportingText = addSetsError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val sets = addSetsInput.toIntOrNull()
                        if (sets == null || sets < 1) {
                            addSetsError = "Must be at least 1"
                        } else {
                            viewModel.addExercise(exerciseId, sets)
                            addCustomExerciseId = null
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { addCustomExerciseId = null; addSetsError = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Custom exercise bottom sheet (for adding custom exercises directly)
        if (uiState.showAddExerciseSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideAddExercise() },
                sheetState = sheetState
            ) {
                val assignedIds = uiState.exercises.map { it.exercise.id }.toSet()
                val available = uiState.allCustomExercises.filter { it.id !in assignedIds }
                if (available.isEmpty()) {
                    Text(
                        "No custom exercises available. Browse the Exercise Library to add exercises.",
                        modifier = Modifier.padding(PaddingValues(16.dp))
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(available, key = { it.id }) { exercise ->
                            ListItem(
                                headlineContent = { Text(exercise.name) },
                                modifier = Modifier.clickable {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        viewModel.hideAddExercise()
                                        addCustomExerciseId = exercise.id
                                        addSetsInput = "3"
                                        addSetsError = null
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
