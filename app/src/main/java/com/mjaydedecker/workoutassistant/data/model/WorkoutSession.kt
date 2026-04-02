package com.mjaydedecker.workoutassistant.data.model

import java.time.Instant

data class WorkoutSession(
    val id: Long,
    val workoutDayId: Long?,
    val workoutDayName: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val durationSeconds: Long? = null,
    val notes: String? = null
)
