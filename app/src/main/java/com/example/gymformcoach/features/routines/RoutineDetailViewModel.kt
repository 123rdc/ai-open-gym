package com.example.gymformcoach.features.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.Routine
import com.example.gymformcoach.core.data.RoutineExercise
import com.example.gymformcoach.core.data.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoutineDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoutineRepository(AppDatabase.getInstance(application))

    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine.asStateFlow()

    private val _exercises = MutableStateFlow<List<RoutineExercise>>(emptyList())
    val exercises: StateFlow<List<RoutineExercise>> = _exercises.asStateFlow()

    private var loadedRoutineId: String? = null

    fun load(routineId: String) {
        if (loadedRoutineId == routineId) return
        loadedRoutineId = routineId
        viewModelScope.launch { repository.getRoutine(routineId).collect { _routine.value = it } }
        viewModelScope.launch { repository.getRoutineExercises(routineId).collect { _exercises.value = it } }
    }
}
