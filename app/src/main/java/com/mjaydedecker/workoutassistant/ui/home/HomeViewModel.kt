package com.mjaydedecker.workoutassistant.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.WorkoutSession
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import com.mjaydedecker.workoutassistant.data.repository.WorkoutDayRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val activeSession: WorkoutSession? = null,
    val exerciseCount: Int = 0,
    val workoutDayCount: Int = 0,
    val completedSessionCount: Int = 0
)

class HomeViewModel(
    sessionRepository: SessionRepository,
    exerciseRepository: ExerciseRepository,
    workoutDayRepository: WorkoutDayRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.getActiveSessionFlow(),
        exerciseRepository.getAll(),
        workoutDayRepository.getAll(),
        sessionRepository.getAllCompleted()
    ) { activeSession, exercises, days, completedSessions ->
        HomeUiState(
            activeSession = activeSession,
            exerciseCount = exercises.size,
            workoutDayCount = days.size,
            completedSessionCount = completedSessions.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}

class HomeViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutDayRepository: WorkoutDayRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(sessionRepository, exerciseRepository, workoutDayRepository) as T
    }
}
