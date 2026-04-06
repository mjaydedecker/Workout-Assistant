package com.mjaydedecker.workoutassistant.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseWeightHistoryScreen(
    app: WorkoutAssistantApp,
    exerciseId: Long,
    onNavigateBack: () -> Unit
) {
    val viewModel: ExerciseWeightHistoryViewModel = viewModel(
        factory = ExerciseWeightHistoryViewModelFactory(
            app.sessionRepository,
            app.settingsRepository,
            exerciseId
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exerciseName.ifBlank { "Weight History" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.points.size < 2 -> Text(
                    text = "Not enough data to display a graph yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
                else -> WeightLineChart(
                    points = uiState.points,
                    weightLabel = uiState.weightLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WeightLineChart(
    points: List<WeightPoint>,
    weightLabel: String,
    modifier: Modifier = Modifier
) {
    val minWeight = points.minOf { it.weight }
    val maxWeight = points.maxOf { it.weight }
    val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)
    val firstDate = points.first().date
    val lastDate = points.last().date
    val dayRange = ChronoUnit.DAYS.between(firstDate, lastDate).coerceAtLeast(1L)
    val dateFmt = DateTimeFormatter.ofPattern("MMM d")
    val chartColor = Color(0xFF6650A4)
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        // Chart row: Y-axis labels on the left, canvas on the right
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

            // Y-axis labels: max at top, min at bottom
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("%.1f $weightLabel".format(maxWeight), style = labelStyle, color = labelColor)
                Text("%.1f $weightLabel".format(minWeight), style = labelStyle, color = labelColor)
            }

            // Chart canvas — full remaining width and height
            Canvas(modifier = Modifier.fillMaxSize()) {
                fun xOf(date: LocalDate): Float {
                    val days = ChronoUnit.DAYS.between(firstDate, date)
                    return (days.toFloat() / dayRange) * size.width
                }

                fun yOf(weight: Double): Float =
                    ((maxWeight - weight) / weightRange * size.height).toFloat()

                // Axes
                drawLine(Color.Gray, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 2f)
                drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)

                // Connecting lines
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = chartColor,
                        start = Offset(xOf(points[i].date), yOf(points[i].weight)),
                        end = Offset(xOf(points[i + 1].date), yOf(points[i + 1].weight)),
                        strokeWidth = 3f
                    )
                }

                // Data point dots
                points.forEach { p ->
                    drawCircle(chartColor, radius = 6f, center = Offset(xOf(p.date), yOf(p.weight)))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // X-axis date labels aligned under the chart area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dateFmt.format(firstDate), style = labelStyle, color = labelColor)
            if (dayRange > 3) {
                Text(
                    dateFmt.format(firstDate.plusDays(dayRange / 2)),
                    style = labelStyle,
                    color = labelColor
                )
            }
            Text(dateFmt.format(lastDate), style = labelStyle, color = labelColor)
        }
    }
}
