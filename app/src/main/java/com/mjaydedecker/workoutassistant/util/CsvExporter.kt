package com.mjaydedecker.workoutassistant.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mjaydedecker.workoutassistant.data.model.SessionExercise
import com.mjaydedecker.workoutassistant.data.model.WorkoutSession
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    fun export(
        context: Context,
        sessions: List<WorkoutSession>,
        exercisesMap: Map<Long, List<SessionExercise>>
    ): Uri {
        val sb = StringBuilder()
        sb.appendLine("Session Date,Session Start Time,Session End Time,Duration,Workout Day,Exercise,Sets Completed,Target Sets,Weight (kg),Notes")

        sessions.forEach { session ->
            val exercises = exercisesMap[session.id] ?: emptyList()
            val date = dateFormatter.format(session.startTime)
            val startTime = timeFormatter.format(session.startTime)
            val endTime = session.endTime?.let { timeFormatter.format(it) } ?: ""
            val duration = session.durationSeconds?.toHhMmSs() ?: ""
            val notes = session.notes?.escapeCsv() ?: ""

            if (exercises.isEmpty()) {
                sb.appendLine("$date,$startTime,$endTime,$duration,${session.workoutDayName.escapeCsv()},,,,,$notes")
            } else {
                exercises.forEach { exercise ->
                    val weight = exercise.weightKg?.let { "%.1f".format(it) } ?: ""
                    sb.appendLine("$date,$startTime,$endTime,$duration,${session.workoutDayName.escapeCsv()},${exercise.exerciseName.escapeCsv()},${exercise.completedSets},${exercise.targetSets},$weight,$notes")
                }
            }
        }

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(java.time.Instant.now())
        val fileName = "workout_history_$timestamp.csv"

        val exportDir = File(context.cacheDir, "csv_exports").also { it.mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(sb.toString())

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun share(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Workout History"))
    }

    private fun String.escapeCsv(): String {
        return if (contains(',') || contains('"') || contains('\n')) "\"${replace("\"", "\"\"")}\"" else this
    }
}
