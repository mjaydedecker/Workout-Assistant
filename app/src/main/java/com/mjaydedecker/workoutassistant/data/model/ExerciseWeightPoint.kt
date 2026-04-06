package com.mjaydedecker.workoutassistant.data.model

import java.time.Instant

data class ExerciseWeightPoint(
    val exerciseName: String,
    val weightKg: Double,
    val startTime: Instant
)
