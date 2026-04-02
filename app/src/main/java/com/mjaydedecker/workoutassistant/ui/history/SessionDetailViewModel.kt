package com.mjaydedecker.workoutassistant.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.SessionExercise
import com.mjaydedecker.workoutassistant.data.model.WorkoutSession
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val session: WorkoutSession? = null,
    val exercises: List<SessionExercise> = emptyList(),
    val isLoading: Boolean = true
)

class SessionDetailViewModel(
    private val repository: SessionRepository,
    private val sessionId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            val session = repository.getById(sessionId)
            val exercises = repository.getExercisesForSessionOnce(sessionId)
            _uiState.update { it.copy(session = session, exercises = exercises, isLoading = false) }
        }
    }
}

class SessionDetailViewModelFactory(
    private val repository: SessionRepository,
    private val sessionId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SessionDetailViewModel(repository, sessionId) as T
    }
}
