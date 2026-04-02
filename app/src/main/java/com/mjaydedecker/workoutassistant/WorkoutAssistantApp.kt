package com.mjaydedecker.workoutassistant

import android.app.Application
import com.mjaydedecker.workoutassistant.data.db.AppDatabase
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import com.mjaydedecker.workoutassistant.data.repository.SettingsRepository
import com.mjaydedecker.workoutassistant.data.repository.WorkoutDayRepository

class WorkoutAssistantApp : Application() {

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
}
