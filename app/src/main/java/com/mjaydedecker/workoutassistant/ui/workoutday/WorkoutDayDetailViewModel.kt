package com.mjaydedecker.workoutassistant.ui.workoutday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.Exercise
import com.mjaydedecker.workoutassistant.data.model.WorkoutDay
import com.mjaydedecker.workoutassistant.data.model.WorkoutDayExerciseItem
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import com.mjaydedecker.workoutassistant.data.repository.WorkoutDayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutDayDetailUiState(
    val workoutDay: WorkoutDay? = null,
    val exercises: List<WorkoutDayExerciseItem> = emptyList(),
    val allExercises: List<Exercise> = emptyList(),
    val showAddExerciseSheet: Boolean = false
)

class WorkoutDayDetailViewModel(
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutDayId: Long
) : ViewModel() {

    private val _showAddSheet = MutableStateFlow(false)

    val uiState: StateFlow<WorkoutDayDetailUiState> = combine(
        workoutDayRepository.getWithExercises(workoutDayId, exerciseRepository.getAll()),
        exerciseRepository.getAll(),
        _showAddSheet
    ) { dayWithExercises, allExercises, showAddSheet ->
        WorkoutDayDetailUiState(
            workoutDay = dayWithExercises?.workoutDay,
            exercises = dayWithExercises?.exercises ?: emptyList(),
            allExercises = allExercises,
            showAddExerciseSheet = showAddSheet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutDayDetailUiState())

    fun showAddExercise() = _showAddSheet.update { true }
    fun hideAddExercise() = _showAddSheet.update { false }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            workoutDayRepository.addExerciseToDay(workoutDayId, exerciseId)
            _showAddSheet.update { false }
        }
    }

    fun removeExercise(assignmentId: Long) {
        viewModelScope.launch { workoutDayRepository.removeExerciseFromDay(assignmentId) }
    }

    fun onReorder(orderedAssignmentIds: List<Long>) {
        viewModelScope.launch { workoutDayRepository.reorderExercises(orderedAssignmentIds) }
    }

    fun renameDay(newName: String) {
        val current = uiState.value.workoutDay ?: return
        viewModelScope.launch {
            workoutDayRepository.save(current.copy(name = newName.trim()))
        }
    }
}

class WorkoutDayDetailViewModelFactory(
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutDayId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WorkoutDayDetailViewModel(workoutDayRepository, exerciseRepository, workoutDayId) as T
    }
}
