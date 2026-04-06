package com.mjaydedecker.workoutassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.mjaydedecker.workoutassistant.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Upsert
    suspend fun upsert(exercise: ExerciseEntity): Long

    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllLibrary(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name ASC")
    fun getAllCustom(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isCustom = 0 ORDER BY name ASC")
    fun getAllLibrary(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long = -1): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE isCustom = 0")
    suspend fun countLibraryExercises(): Int
}
