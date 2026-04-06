package com.mjaydedecker.workoutassistant.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.Exercise
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

const val MY_EXERCISES_GROUP = "My Exercises"

data class ExerciseLibraryUiState(
    val groupedExercises: Map<String, List<Exercise>> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class ExerciseLibraryViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val expandedGroups = MutableStateFlow<Set<String>>(emptySet())

    fun toggleGroup(muscle: String) {
        expandedGroups.update { if (muscle in it) it - muscle else it + muscle }
    }

    private val _scrollIndex = MutableStateFlow(0)
    private val _scrollOffset = MutableStateFlow(0)
    val scrollIndex: StateFlow<Int> = _scrollIndex
    val scrollOffset: StateFlow<Int> = _scrollOffset

    fun saveScrollPosition(index: Int, offset: Int) {
        _scrollIndex.value = index
        _scrollOffset.value = offset
    }

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        exerciseRepository.getAllLibrary(),
        exerciseRepository.getAllCustom(),
        _searchQuery
    ) { libraryExercises, customExercises, query ->
        val filteredLibrary = if (query.isBlank()) libraryExercises
        else libraryExercises.filter { it.name.contains(query, ignoreCase = true) }

        // Group by each primary muscle — an exercise with multiple muscles appears in each group
        val grouped = mutableMapOf<String, MutableList<Exercise>>()
        for (exercise in filteredLibrary) {
            val muscles = exercise.primaryMuscles
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf("Other")

            for (muscle in muscles) {
                grouped.getOrPut(muscle) { mutableListOf() }.add(exercise)
            }
        }

        // Sort groups alphabetically; sort exercises within each group alphabetically
        val sortedGrouped = grouped.entries
            .sortedBy { it.key }
            .associate { (muscle, list) -> muscle to list.sortedBy { it.name } }

        // Append "My Exercises" group at the end
        val filteredCustom = if (query.isBlank()) customExercises
            else customExercises.filter { it.name.contains(query, ignoreCase = true) }

        val finalGrouped = if (filteredCustom.isNotEmpty())
            sortedGrouped + (MY_EXERCISES_GROUP to filteredCustom.sortedBy { it.name })
        else
            sortedGrouped

        ExerciseLibraryUiState(
            groupedExercises = finalGrouped,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseLibraryUiState())

    fun onSearchChange(query: String) = _searchQuery.update { query }
}

class ExerciseLibraryViewModelFactory(
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExerciseLibraryViewModel(exerciseRepository) as T
    }
}
