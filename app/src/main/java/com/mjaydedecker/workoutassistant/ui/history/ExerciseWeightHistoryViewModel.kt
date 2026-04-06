package com.mjaydedecker.workoutassistant.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import com.mjaydedecker.workoutassistant.data.repository.SettingsRepository
import com.mjaydedecker.workoutassistant.util.WeightFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class WeightPoint(val date: LocalDate, val weight: Double)

data class ExerciseWeightHistoryUiState(
    val exerciseName: String = "",
    val points: List<WeightPoint> = emptyList(),
    val weightLabel: String = "kg",
    val isLoading: Boolean = true
)

class ExerciseWeightHistoryViewModel(
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
    exerciseId: Long
) : ViewModel() {

    val uiState: StateFlow<ExerciseWeightHistoryUiState> = combine(
        sessionRepository.getWeightHistory(exerciseId),
        settingsRepository.getSettings()
    ) { history, settings ->
        val unit = settings.weightUnit
        ExerciseWeightHistoryUiState(
            exerciseName = history.firstOrNull()?.exerciseName ?: "",
            points = history.map { point ->
                WeightPoint(
                    date = point.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                    weight = WeightFormatter.toDisplay(point.weightKg, unit)
                )
            },
            weightLabel = WeightFormatter.label(unit),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseWeightHistoryUiState())
}

class ExerciseWeightHistoryViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val exerciseId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExerciseWeightHistoryViewModel(sessionRepository, settingsRepository, exerciseId) as T
    }
}
