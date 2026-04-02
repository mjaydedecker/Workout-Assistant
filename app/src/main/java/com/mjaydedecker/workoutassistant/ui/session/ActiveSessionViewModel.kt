package com.mjaydedecker.workoutassistant.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mjaydedecker.workoutassistant.data.model.AppSettings
import com.mjaydedecker.workoutassistant.data.model.SessionExercise
import com.mjaydedecker.workoutassistant.data.model.WorkoutSession
import com.mjaydedecker.workoutassistant.data.repository.SessionRepository
import com.mjaydedecker.workoutassistant.data.repository.SettingsRepository
import com.mjaydedecker.workoutassistant.data.repository.WorkoutDayRepository
import com.mjaydedecker.workoutassistant.util.AudioVibrationManager
import com.mjaydedecker.workoutassistant.util.TimerManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveSessionUiState(
    val session: WorkoutSession? = null,
    val exercises: List<SessionExercise> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val weightPromptForExerciseId: Long? = null,
    val showEndDialog: Boolean = false,
    val pendingRemoveExerciseId: Long? = null,
    val isLoading: Boolean = true,
    val isPaused: Boolean = false
)

class ActiveSessionViewModel(
    private val sessionRepository: SessionRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    settingsRepository: SettingsRepository,
    context: Context,
    private val workoutDayId: Long?
) : ViewModel() {

    val timerManager = TimerManager(viewModelScope)
    private val audioVibrationManager = AudioVibrationManager(context)

    private val _sessionId = MutableStateFlow<Long?>(null)
    private val _weightPrompt = MutableStateFlow<Long?>(null)
    private val _showEndDialog = MutableStateFlow(false)
    private val _pendingRemove = MutableStateFlow<Long?>(null)

    private val _isPaused = MutableStateFlow(false)
    private var pauseStartEpoch: Long = 0L

    private val _sessionEnded = MutableSharedFlow<Unit>()
    val sessionEnded: SharedFlow<Unit> = _sessionEnded

    private val settingsFlow = settingsRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val restTimeRemaining: StateFlow<Long> = timerManager.restTimeRemaining
    val inactivityTimeRemaining: StateFlow<Long> = timerManager.inactivityTimeRemaining

    val uiState: StateFlow<ActiveSessionUiState> = combine(
        _sessionId.flatMapLatest { id ->
            if (id == null) flowOf(null to emptyList())
            else combine(
                sessionRepository.getActiveSessionFlow(),
                sessionRepository.getExercisesForSession(id)
            ) { session, exercises -> session to exercises }
        },
        settingsFlow,
        _weightPrompt,
        combine(_showEndDialog, _pendingRemove, _isPaused) { showEnd, pendingRemove, isPaused ->
            Triple(showEnd, pendingRemove, isPaused)
        }
    ) { (session, exercises), settings, weightPrompt, (showEnd, pendingRemove, isPaused) ->
        ActiveSessionUiState(
            session = session,
            exercises = exercises,
            settings = settings,
            weightPromptForExerciseId = weightPrompt,
            showEndDialog = showEnd,
            pendingRemoveExerciseId = pendingRemove,
            isLoading = _sessionId.value == null,
            isPaused = isPaused
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveSessionUiState())

    init {
        timerManager.onRestExpired = {
            val settings = settingsFlow.value
            if (settings.restTimerSound) audioVibrationManager.playTimerAlert()
            if (settings.restTimerVibrate) audioVibrationManager.vibrate()
        }
        timerManager.onInactivityExpired = {
            val settings = settingsFlow.value
            if (settings.inactivityTimerSound) audioVibrationManager.playTimerAlert()
            if (settings.inactivityTimerVibrate) audioVibrationManager.vibrate()
        }

        viewModelScope.launch {
            val existingSession = sessionRepository.getActiveSession()
            if (existingSession != null) {
                _sessionId.value = existingSession.id
            } else if (workoutDayId != null) {
                val day = workoutDayRepository.getById(workoutDayId)
                val dayName = day?.name ?: "Workout"
                val newSessionId = sessionRepository.startSession(workoutDayId, dayName)
                _sessionId.value = newSessionId
            }
        }
    }

    fun markSetComplete(sessionExerciseId: Long) {
        viewModelScope.launch {
            val exercise = sessionRepository.getSessionExerciseById(sessionExerciseId) ?: return@launch
            if (exercise.completedSets >= exercise.targetSets) return@launch
            val updated = exercise.copy(completedSets = exercise.completedSets + 1)
            sessionRepository.updateSessionExercise(updated)

            val settings = settingsFlow.value
            timerManager.startOrReset(settings.restTimerSeconds, settings.inactivityTimerSeconds)

            if (updated.weightKg == null && exercise.completedSets == 0) {
                _weightPrompt.update { exercise.exerciseId }
            }
        }
    }

    fun decrementSet(sessionExerciseId: Long) {
        viewModelScope.launch {
            val exercise = sessionRepository.getSessionExerciseById(sessionExerciseId) ?: return@launch
            if (exercise.completedSets <= 0) return@launch
            sessionRepository.updateSessionExercise(exercise.copy(completedSets = exercise.completedSets - 1))
        }
    }

    fun updateWeight(sessionExerciseId: Long, weightKg: Double) {
        viewModelScope.launch {
            val exercise = sessionRepository.getSessionExerciseById(sessionExerciseId) ?: return@launch
            sessionRepository.updateSessionExercise(exercise.copy(weightKg = weightKg))
            sessionRepository.saveDefaultWeight(exercise.exerciseId, weightKg)
            _weightPrompt.update { null }
        }
    }

    fun dismissWeightPrompt() = _weightPrompt.update { null }

    fun requestRemoveExercise(sessionExerciseId: Long) = _pendingRemove.update { sessionExerciseId }

    fun confirmRemoveExercise() {
        viewModelScope.launch {
            _pendingRemove.value?.let { sessionRepository.deleteSessionExercise(it) }
            _pendingRemove.update { null }
        }
    }

    fun cancelRemoveExercise() = _pendingRemove.update { null }

    fun showEndDialog() = _showEndDialog.update { true }
    fun hideEndDialog() = _showEndDialog.update { false }

    fun pauseSession() {
        if (_isPaused.value) return
        timerManager.pause()
        pauseStartEpoch = System.currentTimeMillis() / 1000L
        _isPaused.update { true }
    }

    fun resumeSession() {
        if (!_isPaused.value) return
        val pausedSeconds = (System.currentTimeMillis() / 1000L) - pauseStartEpoch
        pauseStartEpoch = 0L
        _isPaused.update { false }
        timerManager.resume()
        viewModelScope.launch {
            val id = _sessionId.value ?: return@launch
            if (pausedSeconds > 0) sessionRepository.accumulatePause(id, pausedSeconds)
        }
    }

    fun endSession(notes: String?) {
        viewModelScope.launch {
            val id = _sessionId.value ?: return@launch
            // Flush any in-progress pause before ending
            if (_isPaused.value) {
                val pausedSeconds = (System.currentTimeMillis() / 1000L) - pauseStartEpoch
                if (pausedSeconds > 0) sessionRepository.accumulatePause(id, pausedSeconds)
                pauseStartEpoch = 0L
                _isPaused.update { false }
            }
            sessionRepository.endSession(id, notes?.ifBlank { null })
            timerManager.cancel()
            _showEndDialog.update { false }
            _sessionEnded.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerManager.cancel()
    }
}

class ActiveSessionViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context,
    private val workoutDayId: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ActiveSessionViewModel(
            sessionRepository, workoutDayRepository, settingsRepository, context, workoutDayId
        ) as T
    }
}
