package com.mjaydedecker.workoutassistant.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp
import com.mjaydedecker.workoutassistant.util.WeightFormatter
import com.mjaydedecker.workoutassistant.util.toHhMmSs
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    app: WorkoutAssistantApp,
    sessionId: Long,
    onNavigateBack: () -> Unit,
    onExerciseSelected: (exerciseId: Long) -> Unit = {}
) {
    val viewModel: SessionDetailViewModel = viewModel(
        factory = SessionDetailViewModelFactory(app.sessionRepository, app.settingsRepository, sessionId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val session = uiState.session

    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy").withZone(ZoneId.systemDefault())
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.workoutDayName ?: "Session Detail") },
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
            if (session != null) {
                DetailRow("Date", dateFmt.format(session.startTime))
                DetailRow("Start", timeFmt.format(session.startTime))
                session.endTime?.let { DetailRow("End", timeFmt.format(it)) }
                session.durationSeconds?.let { DetailRow("Duration", it.toHhMmSs()) }
                session.notes?.let { DetailRow("Notes", it) }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Exercises", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                uiState.exercises.forEach { exercise ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onExerciseSelected(exercise.exerciseId) },
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(exercise.exerciseName, style = MaterialTheme.typography.bodyLarge)
                            Row {
                                Text(
                                    "${exercise.completedSets} / ${exercise.targetSets} sets",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                exercise.weightKg?.let {
                                    Text(
                                        "  •  ${WeightFormatter.format(it, settings.weightUnit)} ${WeightFormatter.label(settings.weightUnit)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}
