package com.mjaydedecker.workoutassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutDayExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDayExerciseDao {
    @Insert
    suspend fun insert(entity: WorkoutDayExerciseEntity): Long

    @Query("DELETE FROM workout_day_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_day_exercises WHERE workoutDayId = :workoutDayId")
    suspend fun deleteAllForDay(workoutDayId: Long)

    @Query("SELECT * FROM workout_day_exercises WHERE workoutDayId = :workoutDayId ORDER BY orderIndex ASC")
    fun getForDay(workoutDayId: Long): Flow<List<WorkoutDayExerciseEntity>>

    @Query("SELECT * FROM workout_day_exercises WHERE workoutDayId = :workoutDayId ORDER BY orderIndex ASC")
    suspend fun getForDayOnce(workoutDayId: Long): List<WorkoutDayExerciseEntity>

    @Query("SELECT COUNT(*) FROM workout_day_exercises WHERE exerciseId = :exerciseId")
    suspend fun countAssignmentsForExercise(exerciseId: Long): Int

    @Query("UPDATE workout_day_exercises SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int)

    @Transaction
    suspend fun reorder(updates: List<Pair<Long, Int>>) {
        updates.forEach { (id, index) -> updateOrderIndex(id, index) }
    }

    @Query("SELECT MAX(orderIndex) FROM workout_day_exercises WHERE workoutDayId = :workoutDayId")
    suspend fun getMaxOrderIndex(workoutDayId: Long): Int?

    @Query("SELECT * FROM workout_day_exercises WHERE workoutDayId = :workoutDayId AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getAssignment(workoutDayId: Long, exerciseId: Long): WorkoutDayExerciseEntity?
}
