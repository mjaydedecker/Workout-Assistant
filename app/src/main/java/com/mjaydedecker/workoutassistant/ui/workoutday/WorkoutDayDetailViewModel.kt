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
    val allCustomExercises: List<Exercise> = emptyList(),
    val showAddExerciseSheet: Boolean = false
)

class WorkoutDayDetailViewModel(
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutDayId: Long
) : ViewModel() {

    private val _showAddSheet = MutableStateFlow(false)

    // Combine all exercises (library + custom) so the workout day detail can display names for both
    private val allExercisesFlow = combine(
        exerciseRepository.getAllCustom(),
        exerciseRepository.getAllLibrary()
    ) { custom, library -> custom + library }

    val uiState: StateFlow<WorkoutDayDetailUiState> = combine(
        workoutDayRepository.getWithExercises(workoutDayId, allExercisesFlow),
        exerciseRepository.getAllCustom(),
        _showAddSheet
    ) { dayWithExercises, customExercises, showAddSheet ->
        WorkoutDayDetailUiState(
            workoutDay = dayWithExercises?.workoutDay,
            exercises = dayWithExercises?.exercises ?: emptyList(),
            allCustomExercises = customExercises,
            showAddExerciseSheet = showAddSheet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutDayDetailUiState())

    fun showAddExercise() = _showAddSheet.update { true }
    fun hideAddExercise() = _showAddSheet.update { false }

    fun addExercise(exerciseId: Long, sets: Int) {
        viewModelScope.launch {
            workoutDayRepository.addExerciseToDay(workoutDayId, exerciseId, sets)
            _showAddSheet.update { false }
        }
    }

    fun updateSets(assignmentId: Long, sets: Int) {
        viewModelScope.launch {
            workoutDayRepository.updateExerciseSets(assignmentId, sets)
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
