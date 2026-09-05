package com.example.gymformcoach.features.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Routine
import com.example.gymformcoach.core.data.RoutineExercise
import com.example.gymformcoach.core.data.RoutineRepository
import com.example.gymformcoach.core.data.SessionStep
import com.example.gymformcoach.core.data.SupersetGrouping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoutineRepository(AppDatabase.getInstance(application))

    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine.asStateFlow()

    private val _exercises = MutableStateFlow<List<RoutineExercise>>(emptyList())
    val exercises: StateFlow<List<RoutineExercise>> = _exercises.asStateFlow()

    /**
     * §4.2: the routine flattened into individual sets, round-sequenced for
     * supersets (A1->B1->rest->A2->B2->rest...) rather than exercise-by-exercise.
     * This is what the guided session actually steps through.
     */
    val sessionSteps: StateFlow<List<SessionStep>> = _exercises
        .map { SupersetGrouping.buildSequence(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var loadedRoutineId: String? = null

    fun load(routineId: String) {
        if (loadedRoutineId == routineId) return
        loadedRoutineId = routineId
        viewModelScope.launch { repository.getRoutine(routineId).collect { _routine.value = it } }
        viewModelScope.launch { repository.getRoutineExercises(routineId).collect { _exercises.value = it } }
    }
}
