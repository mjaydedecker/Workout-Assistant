package com.mjaydedecker.workoutassistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val restTimerSeconds: Int = 90,
    val inactivityTimerSeconds: Int = 300,
    val restTimerSound: Boolean = true,
    val restTimerVibrate: Boolean = true,
    val inactivityTimerSound: Boolean = true,
    val inactivityTimerVibrate: Boolean = true,
    val keepScreenOn: Boolean = true,
    val themeMode: String = "SYSTEM",
    val weightUnit: String = "KG"
)
