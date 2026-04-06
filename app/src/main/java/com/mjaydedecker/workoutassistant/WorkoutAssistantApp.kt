package com.mjaydedecker.workoutassistant

import android.app.Application
import com.mjaydedecker.workoutassistant.data.db.AppDatabase
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import com.mjaydedecker.workoutassistant.data.repository.SettingsRepository
import com.mjaydedecker.workoutassistant.data.repository.WorkoutDayRepository
import com.mjaydedecker.workoutassistant.util.LibrarySeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutAssistantApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getInstance(this) }

    val exerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao(), database.workoutDayExerciseDao())
    }

    val workoutDayRepository by lazy {
        WorkoutDayRepository(database.workoutDayDao(), database.workoutDayExerciseDao(), database.workoutSessionDao())
    }

    val sessionRepository by lazy {
        SessionRepository(
            database.workoutSessionDao(),
            database.sessionExerciseDao(),
            database.workoutDayExerciseDao(),
            database.exerciseDao(),
            database.exerciseDefaultWeightDao()
        )
    }

    val settingsRepository by lazy {
        SettingsRepository(database.appSettingsDao())
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            LibrarySeeder(this@WorkoutAssistantApp, database.exerciseDao()).seedIfNeeded()
        }
    }
}
