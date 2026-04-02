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
    private var pausedRestRemaining: Long = -1L
    private var pausedInactivityRemaining: Long = -1L

    var onRestExpired: (() -> Unit)? = null
    var onInactivityExpired: (() -> Unit)? = null

    fun startOrReset(restSeconds: Int, inactivitySeconds: Int) {
        pausedRestRemaining = -1L
        pausedInactivityRemaining = -1L
        restJob?.cancel()
        inactivityJob?.cancel()
        // Set values synchronously so UI reflects the new duration immediately
        _restTimeRemaining.value = restSeconds.toLong()
        _inactivityTimeRemaining.value = inactivitySeconds.toLong()

        restJob = scope.launch {
            while (_restTimeRemaining.value > 0) {
                delay(1000L)
                _restTimeRemaining.value -= 1
            }
            if (_restTimeRemaining.value == 0L) onRestExpired?.invoke()
        }

        inactivityJob = scope.launch {
            while (_inactivityTimeRemaining.value > 0) {
                delay(1000L)
                _inactivityTimeRemaining.value -= 1
            }
            if (_inactivityTimeRemaining.value == 0L) onInactivityExpired?.invoke()
        }
    }

    fun pause() {
        pausedRestRemaining = _restTimeRemaining.value
        pausedInactivityRemaining = _inactivityTimeRemaining.value
        restJob?.cancel()
        inactivityJob?.cancel()
    }

    fun resume() {
        val restRemaining = pausedRestRemaining
        val inactivityRemaining = pausedInactivityRemaining
        pausedRestRemaining = -1L
        pausedInactivityRemaining = -1L

        if (restRemaining > 0) {
            _restTimeRemaining.value = restRemaining
            restJob?.cancel()
            restJob = scope.launch {
                while (_restTimeRemaining.value > 0) {
                    delay(1000L)
                    _restTimeRemaining.value -= 1
                }
                if (_restTimeRemaining.value == 0L) onRestExpired?.invoke()
            }
        }

        if (inactivityRemaining > 0) {
            _inactivityTimeRemaining.value = inactivityRemaining
            inactivityJob?.cancel()
            inactivityJob = scope.launch {
                while (_inactivityTimeRemaining.value > 0) {
                    delay(1000L)
                    _inactivityTimeRemaining.value -= 1
                }
                if (_inactivityTimeRemaining.value == 0L) onInactivityExpired?.invoke()
            }
        }
    }

    fun cancel() {
        restJob?.cancel()
        inactivityJob?.cancel()
        pausedRestRemaining = -1L
        pausedInactivityRemaining = -1L
        _restTimeRemaining.value = -1L
        _inactivityTimeRemaining.value = -1L
    }
}
