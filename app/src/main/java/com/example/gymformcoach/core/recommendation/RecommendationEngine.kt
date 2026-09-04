package com.example.gymformcoach.core.recommendation

import com.example.gymformcoach.features.workout.ExerciseCatalog
import com.example.gymformcoach.features.workout.Workout

interface RecommendationEngine {
    fun filterExercisesByEquipment(exercises: List<Workout>, equipment: Set<String>): List<Workout>
    fun suggestedSplitTemplate(splitPreference: String): List<String>
}

class DefaultRecommendationEngine : RecommendationEngine {

    override fun filterExercisesByEquipment(exercises: List<Workout>, equipment: Set<String>): List<Workout> {
        if (equipment.isEmpty() || equipment.contains("Full gym")) return exercises

        val allowedCategories = mutableSetOf<String>()
        if (equipment.contains("Dumbbells only")) allowedCategories.add("Dumbbell")
        if (equipment.contains("Barbell + rack")) allowedCategories.add("Free Weight")
        if (equipment.contains("Bodyweight only")) allowedCategories.add("Bodyweight")
        if (equipment.contains("Resistance bands")) allowedCategories.add("Cable")

        return exercises.filter { it.category in allowedCategories }
    }

    override fun suggestedSplitTemplate(splitPreference: String): List<String> {
        return when (splitPreference) {
            "Full body" -> listOf("Back", "Chest", "Legs", "Shoulders")
            "Upper-lower" -> listOf("Back", "Chest", "Shoulders", "Bicep", "Tricep")
            "Push-pull-legs" -> listOf("Chest", "Shoulders", "Tricep", "Back", "Bicep", "Legs")
            "Bro split" -> listOf("Chest", "Back", "Shoulders", "Legs", "Bicep", "Tricep")
            else -> ExerciseCatalog.bodyParts.map { it.first }
        }
    }

    // TODO: replace this rule-based filtering and template selection with an
    // LLM-driven recommendation call once a model endpoint is wired up.
}
