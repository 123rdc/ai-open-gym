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
        if (index > 0) {
            exercises.add(index - 1, exercises.removeAt(index))
            repairSupersets()
        }
    }

    fun moveDown(index: Int) {
        if (index < exercises.size - 1) {
            exercises.add(index + 1, exercises.removeAt(index))
            repairSupersets()
        }
    }

    /**
     * §4.3/§4.4: group a contiguous run of selected exercises. Cardio is excluded
     * (no meaningful round structure) and the caller supplies types by name since
     * the draft only carries the catalog name.
     */
    fun groupAsSuperset(indices: Set<Int>, cardioExerciseNames: Set<String>): Boolean {
        if (indices.size < 2) return false
        val sorted = indices.sorted()
        val contiguous = sorted.zipWithNext().all { (a, b) -> b == a + 1 }
        if (!contiguous) return false
        if (sorted.any { exercises[it].exerciseId in cardioExerciseNames }) return false

        val groupId = java.util.UUID.randomUUID().toString()
        sorted.forEach { exercises[it].supersetGroupId = groupId }
        return true
    }

    fun ungroupSuperset(groupId: String) {
        exercises.filter { it.supersetGroupId == groupId }.forEach { it.supersetGroupId = null }
    }

    /**
     * Reordering can pull a member out of its group's contiguous block. Drop the
     * whole group rather than persisting one that no longer sequences correctly.
     */
    private fun repairSupersets() {
        val positionsByGroup = exercises.withIndex()
            .mapNotNull { (index, ex) -> ex.supersetGroupId?.let { it to index } }
            .groupBy({ it.first }, { it.second })
        positionsByGroup.forEach { (groupId, positions) ->
            val contiguous = positions.zipWithNext().all { (a, b) -> b == a + 1 }
            if (!contiguous) ungroupSuperset(groupId)
        }
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
                targetSets = draft.targetSets.toIntOrNull() ?: 0,
                supersetGroupId = draft.supersetGroupId
            )
        }
        viewModelScope.launch {
            val id = repository.saveRoutine(routineName.trim(), routineDescription.trim(), routineExercises)
            onSaved(id)
        }
    }
}
