package com.mjaydedecker.workoutassistant.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mjaydedecker.workoutassistant.data.db.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Upsert
    suspend fun upsert(entity: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun get(): Flow<AppSettingsEntity?>
}
