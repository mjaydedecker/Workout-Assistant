package com.mjaydedecker.workoutassistant.data.model

data class Exercise(
    val id: Long,
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
