package com.mjaydedecker.workoutassistant.data.repository

import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutDayDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutDayExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutSessionDao
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutDayEntity
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutDayExerciseEntity
import com.mjaydedecker.workoutassistant.data.model.Exercise
import com.mjaydedecker.workoutassistant.data.model.WorkoutDay
import com.mjaydedecker.workoutassistant.data.model.WorkoutDayExerciseItem
import com.mjaydedecker.workoutassistant.data.model.WorkoutDayWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class WorkoutDayRepository(
    private val workoutDayDao: WorkoutDayDao,
    private val workoutDayExerciseDao: WorkoutDayExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao
) {
    fun getAll(): Flow<List<WorkoutDay>> =
        workoutDayDao.getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): WorkoutDay? =
        workoutDayDao.getById(id)?.toDomain()

    suspend fun save(workoutDay: WorkoutDay): Long =
        workoutDayDao.upsert(WorkoutDayEntity(workoutDay.id, workoutDay.name))

    suspend fun isNameTaken(name: String, excludeId: Long = -1): Boolean =
        workoutDayDao.countByName(name, excludeId) > 0

    suspend fun hasExistingSessions(workoutDayId: Long): Boolean =
        workoutSessionDao.countForDay(workoutDayId) > 0

    suspend fun delete(workoutDay: WorkoutDay) =
        workoutDayDao.delete(WorkoutDayEntity(workoutDay.id, workoutDay.name))

    fun getWithExercises(workoutDayId: Long, exerciseFlow: Flow<List<Exercise>>): Flow<WorkoutDayWithExercises?> {
        val assignmentsFlow = workoutDayExerciseDao.getForDay(workoutDayId)
        return combine(assignmentsFlow, exerciseFlow) { assignments, exercises ->
            val exerciseMap = exercises.associateBy { it.id }
            val day = workoutDayDao.getById(workoutDayId)?.toDomain() ?: return@combine null
            val items = assignments.mapNotNull { assignment ->
                exerciseMap[assignment.exerciseId]?.let { exercise ->
                    WorkoutDayExerciseItem(assignment.id, exercise, assignment.orderIndex, assignment.sets)
                }
            }
            WorkoutDayWithExercises(day, items)
        }
    }

    suspend fun addExerciseToDay(workoutDayId: Long, exerciseId: Long, sets: Int = 3) {
        val maxIndex = workoutDayExerciseDao.getMaxOrderIndex(workoutDayId) ?: -1
        workoutDayExerciseDao.insert(
            WorkoutDayExerciseEntity(
                workoutDayId = workoutDayId,
                exerciseId = exerciseId,
                orderIndex = maxIndex + 1,
                sets = sets
            )
        )
    }

    suspend fun removeExerciseFromDay(assignmentId: Long) =
        workoutDayExerciseDao.deleteById(assignmentId)

    suspend fun updateExerciseSets(assignmentId: Long, sets: Int) =
        workoutDayExerciseDao.updateSets(assignmentId, sets)

    suspend fun reorderExercises(orderedAssignmentIds: List<Long>) {
        workoutDayExerciseDao.reorder(
            orderedAssignmentIds.mapIndexed { index, id -> id to index }
        )
    }

    suspend fun isExerciseInDay(workoutDayId: Long, exerciseId: Long): Boolean =
        workoutDayExerciseDao.getAssignment(workoutDayId, exerciseId) != null

    private fun WorkoutDayEntity.toDomain() = WorkoutDay(id, name)
}
