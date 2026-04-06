package com.mjaydedecker.workoutassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mjaydedecker.workoutassistant.data.db.entity.SessionExerciseEntity
import com.mjaydedecker.workoutassistant.data.model.ExerciseWeightPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionExerciseDao {
    @Insert
    suspend fun insert(entity: SessionExerciseEntity): Long

    @Update
    suspend fun update(entity: SessionExerciseEntity)

    @Query("DELETE FROM session_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getForSession(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getForSessionOnce(sessionId: Long): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getById(id: Long): SessionExerciseEntity?

    @Query("""
        SELECT se.exerciseName, se.weightKg, ws.startTime
        FROM session_exercises se
        JOIN workout_sessions ws ON se.sessionId = ws.id
        WHERE se.exerciseId = :exerciseId
          AND se.weightKg IS NOT NULL
          AND ws.endTime IS NOT NULL
        ORDER BY ws.startTime ASC
    """)
    fun getWeightHistory(exerciseId: Long): Flow<List<ExerciseWeightPoint>>
}
