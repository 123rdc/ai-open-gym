package com.example.gymformcoach.core.analysis

import com.example.gymformcoach.core.data.SetAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory, process-lifetime signal for the post-set analysis pipeline's progress, keyed by
 * exerciseSessionId. The final result also lands in Room (SetAnalysis) - this bus only exists so
 * the results screen can show "analyzing" / "unavailable" states before that row exists.
 */
object PostSetAnalysisStatusBus {
    sealed class State {
        data object Loading : State()
        data class Success(val analysis: SetAnalysis) : State()
        data class Failure(val message: String) : State()
    }

    private val _states = MutableStateFlow<Map<String, State>>(emptyMap())
    val states: StateFlow<Map<String, State>> = _states.asStateFlow()

    fun set(sessionId: String, state: State) {
        _states.value = _states.value + (sessionId to state)
    }
}
