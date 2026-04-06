package com.mjaydedecker.workoutassistant.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.Exercise
import com.mjaydedecker.workoutassistant.data.model.WorkoutDay
import com.mjaydedecker.workoutassistant.data.repository.ExerciseRepository
import com.mjaydedecker.workoutassistant.data.repository.WorkoutDayRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val videoUrls: List<String> = emptyList(),
    val imageRefs: List<String> = emptyList(),
    val workoutDays: List<WorkoutDay> = emptyList(),
    val showWorkoutDayPicker: Boolean = false,
    val showSetsDialog: Boolean = false,
    val selectedWorkoutDayId: Long? = null,
    val setsInput: String = "3",
    val setsError: String? = null,
    val isAdding: Boolean = false
)

class ExerciseDetailViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseId: Long,
    val preselectedWorkoutDayId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack

    val workoutDays: StateFlow<List<WorkoutDay>> = workoutDayRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(exerciseId) ?: return@launch
            val videos = parseJsonArray(exercise.videoUrls)
            val images = parseJsonArray(exercise.imageRefs)
            _uiState.update {
                it.copy(
                    exercise = exercise,
                    videoUrls = videos,
                    imageRefs = images
                )
            }
        }
    }

    fun onAddToWorkoutDayClicked() {
        if (preselectedWorkoutDayId != null) {
            // Go straight to sets dialog
            _uiState.update {
                it.copy(
                    selectedWorkoutDayId = preselectedWorkoutDayId,
                    showSetsDialog = true,
                    setsInput = "3",
                    setsError = null
                )
            }
        } else {
            // Show workout day picker first
            _uiState.update { it.copy(showWorkoutDayPicker = true) }
        }
    }

    fun onWorkoutDaySelected(workoutDayId: Long) {
        _uiState.update {
            it.copy(
                selectedWorkoutDayId = workoutDayId,
                showWorkoutDayPicker = false,
                showSetsDialog = true,
                setsInput = "3",
                setsError = null
            )
        }
    }

    fun onSetsInputChange(value: String) {
        _uiState.update { it.copy(setsInput = value, setsError = null) }
    }

    fun onSetsDialogDismiss() {
        _uiState.update {
            it.copy(showSetsDialog = false, showWorkoutDayPicker = false, setsError = null)
        }
    }

    fun onWorkoutDayPickerDismiss() {
        _uiState.update { it.copy(showWorkoutDayPicker = false) }
    }

    fun confirmAddToWorkoutDay() {
        val state = _uiState.value
        val dayId = state.selectedWorkoutDayId ?: return
        val sets = state.setsInput.toIntOrNull()
        if (sets == null || sets < 1) {
            _uiState.update { it.copy(setsError = "Must be at least 1") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true) }
            workoutDayRepository.addExerciseToDay(dayId, exerciseId, sets)
            _uiState.update { it.copy(isAdding = false, showSetsDialog = false) }
            _navigateBack.emit(Unit)
        }
    }

    private fun parseJsonArray(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}

class ExerciseDetailViewModelFactory(
    private val exerciseRepository: ExerciseRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val exerciseId: Long,
    private val preselectedWorkoutDayId: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExerciseDetailViewModel(
            exerciseRepository, workoutDayRepository, exerciseId, preselectedWorkoutDayId
        ) as T
    }
}
