package com.mjaydedecker.workoutassistant.ui.session

import android.view.WindowManager
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp
import com.mjaydedecker.workoutassistant.ui.session.components.ExerciseCard
import com.mjaydedecker.workoutassistant.ui.session.components.RestTimerWidget
import com.mjaydedecker.workoutassistant.ui.theme.InactivityTimerColor
import com.mjaydedecker.workoutassistant.ui.theme.RestTimerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    app: WorkoutAssistantApp,
    workoutDayId: Long?,
    onSessionEnded: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ActiveSessionViewModel = viewModel(
        factory = ActiveSessionViewModelFactory(
            app.sessionRepository,
            app.workoutDayRepository,
            app.settingsRepository,
            context,
            workoutDayId
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val restTime by viewModel.restTimeRemaining.collectAsState()
    val inactivityTime by viewModel.inactivityTimeRemaining.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sessionEnded.collect { onSessionEnded() }
    }

    // Keep screen on while session is active
    val activity = context as? android.app.Activity
    DisposableEffect(uiState.settings.keepScreenOn) {
        if (uiState.settings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.session?.workoutDayName ?: "Workout") },
                actions = {
                    Button(
                        onClick = { viewModel.showEndDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("End Workout")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RestTimerWidget(
                            timeRemaining = restTime,
                            totalSeconds = uiState.settings.restTimerSeconds,
                            color = RestTimerColor,
                            label = "Rest"
                        )
                        Spacer(Modifier.width(16.dp))
                        RestTimerWidget(
                            timeRemaining = inactivityTime,
                            totalSeconds = uiState.settings.inactivityTimerSeconds,
                            color = InactivityTimerColor,
                            label = "Inactivity"
                        )
                    }
                    HorizontalDivider()
                }

                items(uiState.exercises, key = { it.id }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onMarkComplete = { viewModel.markSetComplete(exercise.id) },
                        onDecrement = { viewModel.decrementSet(exercise.id) },
                        onWeightChanged = { weight -> viewModel.updateWeight(exercise.id, weight) },
                        onRemove = { viewModel.requestRemoveExercise(exercise.id) }
                    )
                }
            }
        }

        // End session dialog
        if (uiState.showEndDialog) {
            EndSessionDialog(
                onConfirm = { notes -> viewModel.endSession(notes) },
                onDismiss = { viewModel.hideEndDialog() }
            )
        }

        // Remove exercise confirmation
        uiState.pendingRemoveExerciseId?.let { id ->
            val exercise = uiState.exercises.find { it.id == id }
            AlertDialog(
                onDismissRequest = { viewModel.cancelRemoveExercise() },
                title = { Text("Remove Exercise") },
                text = { Text("Remove \"${exercise?.exerciseName}\" from this session?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmRemoveExercise() }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelRemoveExercise() }) { Text("Cancel") }
                }
            )
        }

        // Weight input prompt
        uiState.weightPromptForExerciseId?.let { exerciseId ->
            val exercise = uiState.exercises.find { it.exerciseId == exerciseId }
            var weightInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.dismissWeightPrompt() },
                title = { Text("Weight Used") },
                text = {
                    Column {
                        Text("Enter the weight used for ${exercise?.exerciseName ?: "this exercise"}:")
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            weightInput.toDoubleOrNull()?.let { weight ->
                                val seId = uiState.exercises.find { it.exerciseId == exerciseId }?.id
                                if (seId != null) viewModel.updateWeight(seId, weight)
                            } ?: viewModel.dismissWeightPrompt()
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissWeightPrompt() }) { Text("Skip") }
                }
            )
        }
    }
}
