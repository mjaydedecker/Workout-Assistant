package com.mjaydedecker.workoutassistant.ui.workoutday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.WorkoutDay
import com.mjaydedecker.workoutassistant.data.repository.WorkoutDayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutDayListViewModel(private val repository: WorkoutDayRepository) : ViewModel() {

    val workoutDays: StateFlow<List<WorkoutDay>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingDelete = MutableStateFlow<WorkoutDay?>(null)
    val pendingDelete: StateFlow<WorkoutDay?> = _pendingDelete

    private val _pendingRename = MutableStateFlow<WorkoutDay?>(null)
    val pendingRename: StateFlow<WorkoutDay?> = _pendingRename

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    fun showCreate() = _showCreateDialog.update { true }
    fun hideCreate() = _showCreateDialog.update { false }

    fun create(name: String) {
        viewModelScope.launch {
            repository.save(WorkoutDay(id = 0L, name = name.trim()))
            _showCreateDialog.update { false }
        }
    }

    fun requestDelete(day: WorkoutDay) {
        viewModelScope.launch {
            if (repository.hasExistingSessions(day.id)) {
                _pendingDelete.update { day }
            } else {
                repository.delete(day)
            }
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _pendingDelete.value?.let { repository.delete(it) }
            _pendingDelete.update { null }
        }
    }

    fun cancelDelete() = _pendingDelete.update { null }

    fun requestRename(day: WorkoutDay) = _pendingRename.update { day }
    fun cancelRename() = _pendingRename.update { null }

    fun confirmRename(newName: String) {
        viewModelScope.launch {
            _pendingRename.value?.let { day ->
                repository.save(day.copy(name = newName.trim()))
            }
            _pendingRename.update { null }
        }
    }
}

class WorkoutDayListViewModelFactory(private val repository: WorkoutDayRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WorkoutDayListViewModel(repository) as T
    }
}
