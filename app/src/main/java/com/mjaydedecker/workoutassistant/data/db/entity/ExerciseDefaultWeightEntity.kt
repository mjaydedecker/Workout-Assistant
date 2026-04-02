package com.mjaydedecker.workoutassistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_default_weights")
data class ExerciseDefaultWeightEntity(
    @PrimaryKey val exerciseId: Long,
    val weightKg: Double
)
