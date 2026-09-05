package com.example.gymformcoach.features.routines

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gymformcoach.features.workout.Workout

class DraftRoutineExercise(
    val exerciseId: String,
    val imageUrl: String,
    val muscle: String,
    val category: String
) {
    var targetWeightKg by mutableStateOf("20")
    var targetReps by mutableStateOf("10")
    var targetSets by mutableStateOf("3")

    /** §4.1: shared by grouped exercises; null = standalone. */
    var supersetGroupId by mutableStateOf<String?>(null)

    companion object {
        fun fromWorkout(workout: Workout) = DraftRoutineExercise(
            exerciseId = workout.name,
            imageUrl = workout.imageUrl,
            muscle = workout.muscle,
            category = workout.category
        )
    }
}
