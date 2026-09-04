package com.example.gymformcoach.features.routines

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.RoutineExercise
import com.example.gymformcoach.core.data.RoutineRepository
import com.example.gymformcoach.features.workout.Workout
import kotlinx.coroutines.launch

class RoutineBuilderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoutineRepository(AppDatabase.getInstance(application))

    var routineName by mutableStateOf("")
    var routineDescription by mutableStateOf("")
    val exercises = mutableStateListOf<DraftRoutineExercise>()

    val canSave: Boolean
        get() = routineName.isNotBlank() && exercises.isNotEmpty()

    fun addExercise(workout: Workout) {
        if (exercises.none { it.exerciseId == workout.name }) {
            exercises.add(DraftRoutineExercise.fromWorkout(workout))
        }
    }

    fun removeExercise(index: Int) {
        exercises.removeAt(index)
    }

    fun moveUp(index: Int) {
        if (index > 0) exercises.add(index - 1, exercises.removeAt(index))
    }

    fun moveDown(index: Int) {
        if (index < exercises.size - 1) exercises.add(index + 1, exercises.removeAt(index))
    }

    fun saveRoutine(onSaved: (String) -> Unit) {
        if (!canSave) return
        val routineExercises = exercises.mapIndexed { index, draft ->
            RoutineExercise(
                routineId = "",
                exerciseId = draft.exerciseId,
                orderIndex = index,
                targetWeightKg = draft.targetWeightKg.toFloatOrNull() ?: 0f,
                targetReps = draft.targetReps.toIntOrNull() ?: 0,
                targetSets = draft.targetSets.toIntOrNull() ?: 0
            )
        }
        viewModelScope.launch {
            val id = repository.saveRoutine(routineName.trim(), routineDescription.trim(), routineExercises)
            onSaved(id)
        }
    }
}
