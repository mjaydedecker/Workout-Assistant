package com.mjaydedecker.workoutassistant.data.repository

import com.mjaydedecker.workoutassistant.data.db.dao.ExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.ExerciseDefaultWeightDao
import com.mjaydedecker.workoutassistant.data.db.dao.SessionExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutDayExerciseDao
import com.mjaydedecker.workoutassistant.data.db.dao.WorkoutSessionDao
import com.mjaydedecker.workoutassistant.data.db.entity.ExerciseDefaultWeightEntity
import com.mjaydedecker.workoutassistant.data.db.entity.SessionExerciseEntity
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutSessionEntity
import com.mjaydedecker.workoutassistant.data.model.SessionExercise
import com.mjaydedecker.workoutassistant.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class SessionRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val workoutDayExerciseDao: WorkoutDayExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseDefaultWeightDao: ExerciseDefaultWeightDao
) {
    suspend fun getActiveSession(): WorkoutSession? =
        workoutSessionDao.getActiveSession()?.toDomain()

    fun getActiveSessionFlow(): Flow<WorkoutSession?> =
        workoutSessionDao.getActiveSessionFlow().map { it?.toDomain() }

    fun getAllCompleted(): Flow<List<WorkoutSession>> =
        workoutSessionDao.getAllCompleted().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): WorkoutSession? =
        workoutSessionDao.getById(id)?.toDomain()

    fun getExercisesForSession(sessionId: Long): Flow<List<SessionExercise>> =
        sessionExerciseDao.getForSession(sessionId).map { list -> list.map { it.toDomain() } }

    suspend fun getExercisesForSessionOnce(sessionId: Long): List<SessionExercise> =
        sessionExerciseDao.getForSessionOnce(sessionId).map { it.toDomain() }

    suspend fun startSession(workoutDayId: Long, workoutDayName: String): Long {
        val sessionId = workoutSessionDao.insert(
            WorkoutSessionEntity(
                workoutDayId = workoutDayId,
                workoutDayName = workoutDayName,
                startTime = Instant.now()
            )
        )
        val assignments = workoutDayExerciseDao.getForDayOnce(workoutDayId)
        assignments.forEachIndexed { index, assignment ->
            val exercise = exerciseDao.getById(assignment.exerciseId) ?: return@forEachIndexed
            val defaultWeight = exerciseDefaultWeightDao.getByExerciseId(exercise.id)?.weightKg
            sessionExerciseDao.insert(
                SessionExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    targetSets = exercise.defaultSets,
                    weightKg = defaultWeight,
                    orderIndex = index
                )
            )
        }
        return sessionId
    }

    suspend fun updateSessionExercise(sessionExercise: SessionExercise) {
        sessionExerciseDao.update(sessionExercise.toEntity())
    }

    suspend fun deleteSessionExercise(id: Long) =
        sessionExerciseDao.deleteById(id)

    suspend fun getSessionExerciseById(id: Long): SessionExercise? =
        sessionExerciseDao.getById(id)?.toDomain()

    suspend fun saveDefaultWeight(exerciseId: Long, weightKg: Double) {
        exerciseDefaultWeightDao.upsert(ExerciseDefaultWeightEntity(exerciseId, weightKg))
    }

    suspend fun endSession(sessionId: Long, notes: String?) {
        val session = workoutSessionDao.getById(sessionId) ?: return
        val endTime = Instant.now()
        val duration = endTime.epochSecond - session.startTime.epochSecond
        workoutSessionDao.update(
            session.copy(endTime = endTime, durationSeconds = duration, notes = notes)
        )
    }

    private fun WorkoutSessionEntity.toDomain() = WorkoutSession(
        id, workoutDayId, workoutDayName, startTime, endTime, durationSeconds, notes
    )

    private fun SessionExerciseEntity.toDomain() = SessionExercise(
        id, sessionId, exerciseId, exerciseName, targetSets, completedSets, weightKg, orderIndex
    )

    private fun SessionExercise.toEntity() = SessionExerciseEntity(
        id, sessionId, exerciseId, exerciseName, targetSets, completedSets, weightKg, orderIndex
    )
}
