package com.mjaydedecker.workoutassistant.util

import android.content.Context
import com.mjaydedecker.workoutassistant.data.db.dao.ExerciseDao

class LibrarySeeder(
    private val context: Context,
    private val exerciseDao: ExerciseDao
) {
    companion object {
        private const val PREFS_NAME = "library_seeder_prefs"
        private const val KEY_SEEDED = "library_seeded_v1"
    }

    suspend fun seedIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return

        try {
            context.assets.open("exercises.csv").use { stream ->
                val exercises = CsvParser.parseExercises(stream)
                if (exercises.isNotEmpty()) {
                    exerciseDao.insertAllLibrary(exercises)
                }
            }
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        } catch (e: Exception) {
            // Seeding failed — will retry on next launch
        }
    }
}
