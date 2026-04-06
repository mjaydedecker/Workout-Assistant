package com.mjaydedecker.workoutassistant.ui.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    app: WorkoutAssistantApp,
    exerciseId: Long,
    preselectedWorkoutDayId: Long?,
    onNavigateBack: () -> Unit
) {
    val viewModel: ExerciseDetailViewModel = viewModel(
        factory = ExerciseDetailViewModelFactory(
            app.exerciseRepository,
            app.workoutDayRepository,
            exerciseId,
            preselectedWorkoutDayId
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val workoutDays by viewModel.workoutDays.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val exercise = uiState.exercise

            if (exercise != null) {
                // Metadata section
                if (exercise.force != null || exercise.equipment != null ||
                    exercise.primaryMuscles != null || exercise.secondaryMuscles != null
                ) {
                    Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    exercise.force?.let { MetadataRow("Force", it) }
                    exercise.equipment?.let { MetadataRow("Equipment", it) }
                    exercise.primaryMuscles?.let { MetadataRow("Primary Muscles", it) }
                    exercise.secondaryMuscles?.let { MetadataRow("Secondary Muscles", it) }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                }

                // Instructions section
                if (exercise.description != null) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        exercise.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                }

                // Videos section
                if (uiState.videoUrls.isNotEmpty()) {
                    Text("Video Tutorials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    // Show up to 3 unique videos to avoid an overwhelming list
                    uiState.videoUrls.take(3).forEachIndexed { index, url ->
                        ListItem(
                            headlineContent = { Text("Video Tutorial ${index + 1}") },
                            supportingContent = { Text(url, maxLines = 1) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Play tutorial ${index + 1}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp)
                                .run {
                                    this.also {
                                        // handled via onClick below
                                    }
                                }
                        )
                        // Clickable overlay using a TextButton approach
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Tutorial ${index + 1}")
                        }
                        HorizontalDivider()
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Add to Workout Day button
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.onAddToWorkoutDayClicked() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Workout Day")
                }
            }
        }

        // Workout Day Picker bottom sheet
        if (uiState.showWorkoutDayPicker) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { viewModel.onWorkoutDayPickerDismiss() },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Select Workout Day",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (workoutDays.isEmpty()) {
                        Text(
                            "No workout days found. Create a workout day first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        workoutDays.forEach { day ->
                            ListItem(
                                headlineContent = { Text(day.name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .run {
                                        this
                                    }
                            )
                            TextButton(
                                onClick = { viewModel.onWorkoutDaySelected(day.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(day.name)
                            }
                            HorizontalDivider()
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Sets count dialog
        if (uiState.showSetsDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onSetsDialogDismiss() },
                title = { Text("Number of Sets") },
                text = {
                    Column {
                        Text("How many sets for this exercise?")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.setsInput,
                            onValueChange = { viewModel.onSetsInputChange(it) },
                            label = { Text("Sets") },
                            isError = uiState.setsError != null,
                            supportingText = uiState.setsError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmAddToWorkoutDay() },
                        enabled = !uiState.isAdding
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onSetsDialogDismiss() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.6f)
        )
    }
}
