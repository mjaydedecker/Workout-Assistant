package com.mjaydedecker.workoutassistant.data.repository

import com.mjaydedecker.workoutassistant.data.db.dao.ExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutDayExerciseDao
import com.mjaydedecker.workoutassistant.data.db.entity.ExerciseEntity
import com.mjaydedecker.workoutassistant.data.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutDayExerciseDao: WorkoutDayExerciseDao
) {
    fun getAllCustom(): Flow<List<Exercise>> =
        exerciseDao.getAllCustom().map { list -> list.map { it.toDomain() } }

    fun getAllLibrary(): Flow<List<Exercise>> =
        exerciseDao.getAllLibrary().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Exercise? =
        exerciseDao.getById(id)?.toDomain()

    suspend fun save(exercise: Exercise): Long =
        exerciseDao.upsert(exercise.toEntity())

    suspend fun isNameTaken(name: String, excludeId: Long = -1): Boolean =
        exerciseDao.countByName(name, excludeId) > 0

    suspend fun isAssignedToDay(exerciseId: Long): Boolean =
        workoutDayExerciseDao.countAssignmentsForExercise(exerciseId) > 0

    suspend fun delete(exercise: Exercise) =
        exerciseDao.delete(exercise.toEntity())

    private fun ExerciseEntity.toDomain() = Exercise(
        id = id,
        name = name,
        isCustom = isCustom,
        force = force,
        equipment = equipment,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles,
        description = description,
        videoUrls = videoUrls,
        imageRefs = imageRefs
    )

    private fun Exercise.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        isCustom = isCustom,
        force = force,
        equipment = equipment,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles,
        description = description,
        videoUrls = videoUrls,
        imageRefs = imageRefs
    )
}
