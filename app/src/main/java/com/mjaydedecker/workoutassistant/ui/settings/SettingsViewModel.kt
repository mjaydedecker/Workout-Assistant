package com.mjaydedecker.workoutassistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.AppSettings
import com.mjaydedecker.workoutassistant.data.model.ThemeMode
import com.mjaydedecker.workoutassistant.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<AppSettings> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private fun update(block: AppSettings.() -> AppSettings) {
        viewModelScope.launch { repository.save(uiState.value.block()) }
    }

    fun setRestTimerSeconds(seconds: Int) = update { copy(restTimerSeconds = seconds.coerceAtLeast(1)) }
    fun setInactivityTimerSeconds(seconds: Int) = update { copy(inactivityTimerSeconds = seconds.coerceAtLeast(1)) }
    fun setRestTimerSound(enabled: Boolean) = update { copy(restTimerSound = enabled) }
    fun setRestTimerVibrate(enabled: Boolean) = update { copy(restTimerVibrate = enabled) }
    fun setInactivityTimerSound(enabled: Boolean) = update { copy(inactivityTimerSound = enabled) }
    fun setInactivityTimerVibrate(enabled: Boolean) = update { copy(inactivityTimerVibrate = enabled) }
    fun setKeepScreenOn(enabled: Boolean) = update { copy(keepScreenOn = enabled) }
    fun setThemeMode(mode: ThemeMode) = update { copy(themeMode = mode) }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(repository) as T
    }
}
