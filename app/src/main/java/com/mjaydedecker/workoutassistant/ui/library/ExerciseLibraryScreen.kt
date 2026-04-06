package com.mjaydedecker.workoutassistant.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    app: WorkoutAssistantApp,
    onExerciseSelected: (exerciseId: Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onManageCustomExercises: () -> Unit = {}
) {
    val viewModel: ExerciseLibraryViewModel = viewModel(
        factory = ExerciseLibraryViewModelFactory(app.exerciseRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = viewModel.scrollIndex.value,
        initialFirstVisibleItemScrollOffset = viewModel.scrollOffset.value
    )

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            viewModel.saveScrollPosition(index, offset)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Library") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchChange(it) },
                placeholder = { Text("Search exercises…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading exercises…", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (uiState.groupedExercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (uiState.searchQuery.isBlank()) "No exercises in library yet."
                        else "No exercises match your search.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    uiState.groupedExercises.forEach { (muscle, exercises) ->
                        val isExpanded = muscle in expandedGroups || uiState.searchQuery.isNotBlank()

                        item(key = "header_$muscle") {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        muscle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        "${exercises.size} exercise${if (exercises.size == 1) "" else "s"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    Row {
                                        if (muscle == MY_EXERCISES_GROUP) {
                                            IconButton(onClick = onManageCustomExercises) {
                                                Icon(Icons.Default.Edit, contentDescription = "Manage My Exercises")
                                            }
                                        }
                                        Icon(
                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpanded) "Collapse $muscle" else "Expand $muscle"
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.clickable {
                                    viewModel.toggleGroup(muscle)
                                }
                            )
                            HorizontalDivider()
                        }

                        if (isExpanded) {
                            items(exercises, key = { "${muscle}_${it.id}" }) { exercise ->
                                ListItem(
                                    headlineContent = { Text(exercise.name) },
                                    supportingContent = {
                                        val details = listOfNotNull(
                                            exercise.equipment,
                                            exercise.force
                                        ).joinToString(" · ")
                                        if (details.isNotBlank()) Text(details)
                                    },
                                    modifier = Modifier
                                        .clickable { onExerciseSelected(exercise.id) }
                                        .padding(start = 8.dp)
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
