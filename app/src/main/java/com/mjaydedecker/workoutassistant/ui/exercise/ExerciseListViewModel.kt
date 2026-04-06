package com.mjaydedecker.workoutassistant.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.Exercise
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseListUiState(
    val exercises: List<Exercise> = emptyList(),
    val pendingDeleteExercise: Exercise? = null
)

class ExerciseListViewModel(private val repository: ExerciseRepository) : ViewModel() {

    val exercises: StateFlow<List<Exercise>> = repository.getAllCustom()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingDelete = MutableStateFlow<Exercise?>(null)
    val pendingDelete: StateFlow<Exercise?> = _pendingDelete

    fun requestDelete(exercise: Exercise) {
        viewModelScope.launch {
            if (repository.isAssignedToDay(exercise.id)) {
                _pendingDelete.update { exercise }
            } else {
                repository.delete(exercise)
            }
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _pendingDelete.value?.let { repository.delete(it) }
            _pendingDelete.update { null }
        }
    }

    fun cancelDelete() {
        _pendingDelete.update { null }
    }
}

class ExerciseListViewModelFactory(private val repository: ExerciseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExerciseListViewModel(repository) as T
    }
}
