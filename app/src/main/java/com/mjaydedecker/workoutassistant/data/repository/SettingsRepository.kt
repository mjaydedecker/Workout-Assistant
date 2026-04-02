package com.mjaydedecker.workoutassistant.data.repository

import com.mjaydedecker.workoutassistant.data.db.dao.AppSettingsDao
import com.mjaydedecker.workoutassistant.data.db.entity.AppSettingsEntity
import com.mjaydedecker.workoutassistant.data.model.AppSettings
import com.mjaydedecker.workoutassistant.data.model.ThemeMode
import com.mjaydedecker.workoutassistant.data.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val appSettingsDao: AppSettingsDao) {

    fun getSettings(): Flow<AppSettings> =
        appSettingsDao.get().map { it?.toDomain() ?: AppSettings() }

    suspend fun save(settings: AppSettings) {
        appSettingsDao.upsert(settings.toEntity())
    }

    private fun AppSettingsEntity.toDomain() = AppSettings(
        restTimerSeconds = restTimerSeconds,
        inactivityTimerSeconds = inactivityTimerSeconds,
        restTimerSound = restTimerSound,
        restTimerVibrate = restTimerVibrate,
        inactivityTimerSound = inactivityTimerSound,
        inactivityTimerVibrate = inactivityTimerVibrate,
        keepScreenOn = keepScreenOn,
        themeMode = ThemeMode.valueOf(themeMode),
        weightUnit = WeightUnit.valueOf(weightUnit)
    )

    private fun AppSettings.toEntity() = AppSettingsEntity(
        id = 1,
        restTimerSeconds = restTimerSeconds,
        inactivityTimerSeconds = inactivityTimerSeconds,
        restTimerSound = restTimerSound,
        restTimerVibrate = restTimerVibrate,
        inactivityTimerSound = inactivityTimerSound,
        inactivityTimerVibrate = inactivityTimerVibrate,
        keepScreenOn = keepScreenOn,
        themeMode = themeMode.name,
        weightUnit = weightUnit.name
    )
}
