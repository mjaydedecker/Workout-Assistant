package com.mjaydedecker.workoutassistant.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.WorkoutSession
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import com.mjaydedecker.workoutassistant.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class HistoryView { LIST, CALENDAR }

class SessionHistoryViewModel(
    private val repository: SessionRepository,
    private val context: Context
) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> = repository.getAllCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Dates that have at least one completed session — used by the calendar to show dots. */
    val sessionDates: StateFlow<Set<LocalDate>> = sessions.map { list ->
        list.map { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _selectedView = MutableStateFlow(HistoryView.LIST)
    val selectedView: StateFlow<HistoryView> = _selectedView

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    /** Sessions on the currently selected calendar date. */
    val sessionsOnSelectedDate: StateFlow<List<WorkoutSession>> = _selectedDate.map { date ->
        if (date == null) emptyList()
        else sessions.value.filter {
            it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == date
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setView(view: HistoryView) = _selectedView.update { view }

    fun selectDate(date: LocalDate?) {
        _selectedDate.update { if (_selectedDate.value == date) null else date }
    }

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
