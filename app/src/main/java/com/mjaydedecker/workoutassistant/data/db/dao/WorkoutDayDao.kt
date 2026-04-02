package com.mjaydedecker.workoutassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.mjaydedecker.workoutassistant.data.db.entity.WorkoutDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDayDao {
    @Upsert
    suspend fun upsert(workoutDay: WorkoutDayEntity): Long

    @Delete
    suspend fun delete(workoutDay: WorkoutDayEntity)

    @Query("SELECT * FROM workout_days ORDER BY name ASC")
    fun getAll(): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_days WHERE id = :id")
    suspend fun getById(id: Long): WorkoutDayEntity?

    @Query("SELECT COUNT(*) FROM workout_days WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long = -1): Int
}
