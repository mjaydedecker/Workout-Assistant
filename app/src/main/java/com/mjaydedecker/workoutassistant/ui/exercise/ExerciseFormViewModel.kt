package com.mjaydedecker.workoutassistant.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.Exercise
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseFormState(
    val name: String = "",
    val defaultSets: String = "3",
    val nameError: String? = null,
    val setsError: String? = null,
    val isSaving: Boolean = false
)

class ExerciseFormViewModel(
    private val repository: ExerciseRepository,
    private val exerciseId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseFormState())
    val state: StateFlow<ExerciseFormState> = _state

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                repository.getById(exerciseId)?.let { exercise ->
                    _state.update { it.copy(name = exercise.name, defaultSets = exercise.defaultSets.toString()) }
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun onSetsChange(value: String) = _state.update { it.copy(defaultSets = value, setsError = null) }

    fun save() {
        val current = _state.value
        val setsInt = current.defaultSets.toIntOrNull()

        var nameError: String? = null
        var setsError: String? = null

        if (current.name.isBlank()) nameError = "Name is required"
        if (setsInt == null || setsInt < 1) setsError = "Must be at least 1"

        if (nameError != null || setsError != null) {
            _state.update { it.copy(nameError = nameError, setsError = setsError) }
            return
        }

        viewModelScope.launch {
            if (repository.isNameTaken(current.name.trim(), excludeId = exerciseId ?: -1L)) {
                _state.update { it.copy(nameError = "An exercise with this name already exists") }
                return@launch
            }
            _state.update { it.copy(isSaving = true) }
            repository.save(Exercise(id = exerciseId ?: 0L, name = current.name.trim(), defaultSets = setsInt!!))
            _navigateBack.emit(Unit)
        }
    }
}

class ExerciseFormViewModelFactory(
    private val repository: ExerciseRepository,
    private val exerciseId: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExerciseFormViewModel(repository, exerciseId) as T
    }
}
