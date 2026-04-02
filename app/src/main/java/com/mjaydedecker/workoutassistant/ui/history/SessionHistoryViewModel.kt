package com.mjaydedecker.workoutassistant.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.WorkoutSession
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import com.mjaydedecker.workoutassistant.util.CsvExporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionHistoryViewModel(
    private val repository: SessionRepository,
    private val context: Context
) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> = repository.getAllCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportCsv() {
        viewModelScope.launch {
            val sessionList = sessions.value
            val exercisesMap = sessionList.associate { session ->
                session.id to repository.getExercisesForSessionOnce(session.id)
            }
            val uri = CsvExporter.export(context, sessionList, exercisesMap)
            CsvExporter.share(context, uri)
        }
    }
}

class SessionHistoryViewModelFactory(
    private val repository: SessionRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SessionHistoryViewModel(repository, context) as T
    }
}
