package com.mjaydedecker.workoutassistant.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerManager(private val scope: CoroutineScope) {

    private val _restTimeRemaining = MutableStateFlow(-1L)
    val restTimeRemaining: StateFlow<Long> = _restTimeRemaining.asStateFlow()

    private val _inactivityTimeRemaining = MutableStateFlow(-1L)
    val inactivityTimeRemaining: StateFlow<Long> = _inactivityTimeRemaining.asStateFlow()

    private var restJob: Job? = null
    private var inactivityJob: Job? = null

    var onRestExpired: (() -> Unit)? = null
    var onInactivityExpired: (() -> Unit)? = null

    fun startOrReset(restSeconds: Int, inactivitySeconds: Int) {
        restJob?.cancel()
        inactivityJob?.cancel()

        restJob = scope.launch {
            _restTimeRemaining.value = restSeconds.toLong()
            while (_restTimeRemaining.value > 0) {
                delay(1000L)
                _restTimeRemaining.value -= 1
            }
            if (_restTimeRemaining.value == 0L) onRestExpired?.invoke()
        }

        inactivityJob = scope.launch {
            _inactivityTimeRemaining.value = inactivitySeconds.toLong()
            while (_inactivityTimeRemaining.value > 0) {
                delay(1000L)
                _inactivityTimeRemaining.value -= 1
            }
            if (_inactivityTimeRemaining.value == 0L) onInactivityExpired?.invoke()
        }
    }

    fun cancel() {
        restJob?.cancel()
        inactivityJob?.cancel()
        _restTimeRemaining.value = -1L
        _inactivityTimeRemaining.value = -1L
    }
}
