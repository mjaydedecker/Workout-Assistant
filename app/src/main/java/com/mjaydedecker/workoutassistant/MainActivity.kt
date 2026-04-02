package com.mjaydedecker.workoutassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjaydedecker.workoutassistant.ui.navigation.NavGraph
import com.mjaydedecker.workoutassistant.ui.settings.SettingsViewModel
import com.mjaydedecker.workoutassistant.ui.settings.SettingsViewModelFactory
import com.mjaydedecker.workoutassistant.ui.theme.WorkoutAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WorkoutAssistantApp
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(app.settingsRepository)
            )
            val settings by settingsViewModel.uiState.collectAsState()
            val darkTheme = when (settings.themeMode) {
                com.mjaydedecker.workoutassistant.data.model.ThemeMode.DARK -> true
                com.mjaydedecker.workoutassistant.data.model.ThemeMode.LIGHT -> false
                com.mjaydedecker.workoutassistant.data.model.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            WorkoutAssistantTheme(darkTheme = darkTheme) {
                NavGraph(app = app)
            }
        }
    }
}
