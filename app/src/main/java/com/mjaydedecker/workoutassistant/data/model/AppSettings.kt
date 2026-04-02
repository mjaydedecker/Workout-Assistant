package com.mjaydedecker.workoutassistant.data.model

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val restTimerSeconds: Int = 90,
    val inactivityTimerSeconds: Int = 300,
    val restTimerSound: Boolean = true,
    val restTimerVibrate: Boolean = true,
    val inactivityTimerSound: Boolean = true,
    val inactivityTimerVibrate: Boolean = true,
    val keepScreenOn: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
