package com.mjaydedecker.workoutassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mjaydedecker.workoutassistant.data.db.entity.ExerciseDefaultWeightEntity

@Dao
interface ExerciseDefaultWeightDao {
    @Upsert
    suspend fun upsert(entity: ExerciseDefaultWeightEntity)

    @Query("SELECT * FROM exercise_default_weights WHERE exerciseId = :exerciseId")
    suspend fun getByExerciseId(exerciseId: Long): ExerciseDefaultWeightEntity?
}
