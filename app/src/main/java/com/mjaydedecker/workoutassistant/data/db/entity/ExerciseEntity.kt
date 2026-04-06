package com.mjaydedecker.workoutassistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean = true,
    val force: String? = null,
    val equipment: String? = null,
    val primaryMuscles: String? = null,
    val secondaryMuscles: String? = null,
    val description: String? = null,
    val videoUrls: String? = null,
    val imageRefs: String? = null
)
