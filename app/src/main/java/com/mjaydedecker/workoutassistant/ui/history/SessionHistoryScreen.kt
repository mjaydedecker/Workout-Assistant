package com.mjaydedecker.workoutassistant.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp
import com.mjaydedecker.workoutassistant.ui.history.components.WorkoutCalendar
import com.mjaydedecker.workoutassistant.util.toHhMmSs
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    app: WorkoutAssistantApp,
    onSessionSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val viewModel: SessionHistoryViewModel = viewModel(
        factory = SessionHistoryViewModelFactory(app.sessionRepository, context)
    )
    val sessions by viewModel.sessions.collectAsState()
    val selectedView by viewModel.selectedView.collectAsState()
    val sessionDates by viewModel.sessionDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sessionsOnDate by viewModel.sessionsOnSelectedDate.collectAsState()

    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy  HH:mm")
        .withZone(ZoneId.systemDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                actions = {
                    if (sessions.isNotEmpty()) {
                        IconButton(onClick = { viewModel.exportCsv() }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export to CSV")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // View toggle: List / Calendar
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = selectedView == HistoryView.LIST,
                    onClick = { viewModel.setView(HistoryView.LIST) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = { Text("List") }
                )
                SegmentedButton(
                    selected = selectedView == HistoryView.CALENDAR,
                    onClick = { viewModel.setView(HistoryView.CALENDAR) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = { Text("Calendar") }
                )
            }

            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "No completed workouts yet.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else when (selectedView) {

                HistoryView.LIST -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(sessions, key = { it.id }) { session ->
                            ListItem(
                                headlineContent = { Text(session.workoutDayName) },
                                supportingContent = {
                                    Text(
                                        dateFmt.format(session.startTime) +
                                            session.durationSeconds?.let { "  •  ${it.toHhMmSs()}" }.orEmpty()
                                    )
                                },
                                modifier = Modifier.clickable { onSessionSelected(session.id) }
                            )
                        }
                    }
                }

                HistoryView.CALENDAR -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            WorkoutCalendar(
                                sessionDates = sessionDates,
                                selectedDate = selectedDate,
                                onDayClick = { viewModel.selectDate(it) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        if (selectedDate != null && sessionsOnDate.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider()
                                Text(
                                    "Sessions on this day",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            items(sessionsOnDate, key = { it.id }) { session ->
                                ListItem(
                                    headlineContent = { Text(session.workoutDayName) },
                                    supportingContent = {
                                        Text(
                                            dateFmt.format(session.startTime) +
                                                session.durationSeconds?.let { "  •  ${it.toHhMmSs()}" }.orEmpty()
                                        )
                                    },
                                    modifier = Modifier.clickable { onSessionSelected(session.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
