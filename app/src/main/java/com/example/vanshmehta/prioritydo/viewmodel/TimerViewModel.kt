package com.example.vanshmehta.prioritydo.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vanshmehta.prioritydo.util.TimerNotifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerState(
    val totalMs: Long = DEFAULT_MS,
    val remainingMs: Long = DEFAULT_MS,
    val running: Boolean = false,
    val finished: Boolean = false,
    val taskId: String? = null,
    val taskTitle: String? = null
) {
    companion object { const val DEFAULT_MS = 25 * 60_000L }
}

/**
 * Remaining time is derived from SystemClock.elapsedRealtime() (monotonic, keeps counting in the
 * background), never by decrementing a counter, so late ticks cannot drift the result. Only one
 * ticker job exists at a time. The ViewModel is activity-scoped, so the timer keeps running while
 * the user navigates between screens or backgrounds the app. If Android kills the process the
 * session is discarded (nothing is persisted) and the timer starts fresh at the next launch.
 */
class TimerViewModel(private val app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var endElapsed = 0L
    private var ticker: Job? = null

    fun configure(durationMs: Long) {
        if (_state.value.running || durationMs <= 0) return
        _state.update { it.copy(totalMs = durationMs, remainingMs = durationMs, finished = false) }
    }

    fun link(taskId: String?, title: String?) {
        _state.update { it.copy(taskId = taskId, taskTitle = title) }
    }

    fun start() {
        val s = _state.value
        if (s.running || s.remainingMs <= 0) return
        endElapsed = SystemClock.elapsedRealtime() + s.remainingMs
        _state.update { it.copy(running = true, finished = false) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val rem = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                _state.update { it.copy(remainingMs = rem) }
                if (rem == 0L) {
                    val title = _state.value.taskTitle
                    _state.update { it.copy(running = false, finished = true) }
                    TimerNotifier.showDone(app, title)
                    break
                }
                delay(200)
            }
        }
    }

    fun pause() {
        if (!_state.value.running) return
        ticker?.cancel()
        val rem = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _state.update { it.copy(running = false, remainingMs = rem) }
    }

    fun reset() {
        ticker?.cancel()
        _state.update { it.copy(running = false, finished = false, remainingMs = it.totalMs) }
    }

    /** Used on logout: stops the timer and forgets the linked task. */
    fun clear() {
        ticker?.cancel()
        _state.value = TimerState()
    }
}
