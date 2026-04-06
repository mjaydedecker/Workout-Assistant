package com.mjaydedecker.workoutassistant.data.model

data class WorkoutDay(
    val id: Long,
    val name: String,
    val exerciseCount: Int = 0
)

data class WorkoutDayWithExercises(
    val workoutDay: WorkoutDay,
    val exercises: List<WorkoutDayExerciseItem>
)

data class WorkoutDayExerciseItem(
    val assignmentId: Long,
    val exercise: Exercise,
    val orderIndex: Int,
    val sets: Int = 3
)
