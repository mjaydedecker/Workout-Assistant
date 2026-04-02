package com.mjaydedecker.workoutassistant.data.model

data class SessionExercise(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val targetSets: Int,
    val completedSets: Int,
    val weightKg: Double?,
    val orderIndex: Int
) {
    val isCompleted: Boolean get() = completedSets >= targetSets
}
