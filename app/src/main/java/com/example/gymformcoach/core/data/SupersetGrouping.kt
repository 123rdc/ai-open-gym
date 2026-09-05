package com.example.gymformcoach.core.data

import java.util.UUID

/**
 * §4 superset grouping rules, kept as pure functions so they're testable without
 * Room or Compose.
 */
object SupersetGrouping {

    /**
     * §4.4: members must be contiguous in orderIndex, and cardio can't take part
     * (there's no meaningful round structure for it). Returns the exercises with
     * the group applied, or null if the selection is invalid.
     */
    fun group(
        exercises: List<RoutineExercise>,
        selectedIds: Set<String>,
        exerciseTypesByName: Map<String, ExerciseType>
    ): List<RoutineExercise>? {
        if (selectedIds.size < 2) return null

        val ordered = exercises.sortedBy { it.orderIndex }
        val selectedPositions = ordered.withIndex()
            .filter { it.value.id in selectedIds }
            .map { it.index }
        if (selectedPositions.size != selectedIds.size) return null

        val contiguous = selectedPositions.zipWithNext().all { (a, b) -> b == a + 1 }
        if (!contiguous) return null

        val anyCardio = ordered.filter { it.id in selectedIds }
            .any { exerciseTypesByName[it.exerciseId] == ExerciseType.CARDIO }
        if (anyCardio) return null

        val groupId = UUID.randomUUID().toString()
        return exercises.map {
            if (it.id in selectedIds) it.copy(supersetGroupId = groupId, updatedAt = System.currentTimeMillis())
            else it
        }
    }

    fun ungroup(exercises: List<RoutineExercise>, groupId: String): List<RoutineExercise> =
        exercises.map {
            if (it.supersetGroupId == groupId) it.copy(supersetGroupId = null, updatedAt = System.currentTimeMillis())
            else it
        }

    /**
     * Reordering can pull an exercise out of its group's contiguous block. Rather
     * than silently leaving a broken group, drop any member that is no longer
     * adjacent to the rest of its group (§4.3).
     */
    fun repairAfterReorder(exercises: List<RoutineExercise>): List<RoutineExercise> {
        val ordered = exercises.sortedBy { it.orderIndex }
        val brokenGroups = ordered
            .mapIndexedNotNull { index, ex -> ex.supersetGroupId?.let { it to index } }
            .groupBy({ it.first }, { it.second })
            .filterValues { positions -> positions.zipWithNext().any { (a, b) -> b != a + 1 } }
            .keys

        if (brokenGroups.isEmpty()) return exercises
        return exercises.map {
            if (it.supersetGroupId in brokenGroups) it.copy(supersetGroupId = null, updatedAt = System.currentTimeMillis())
            else it
        }
    }

    /**
     * §4.2: sequences a routine into rounds. A superset group of A,B with 3 sets
     * runs A1→B1→rest→A2→B2→rest→A3→B3→rest — round-based, not exercise-based,
     * and the rest only fires after the last member of a round.
     */
    fun buildSequence(exercises: List<RoutineExercise>): List<SessionStep> {
        val ordered = exercises.sortedBy { it.orderIndex }
        val steps = mutableListOf<SessionStep>()
        var index = 0
        while (index < ordered.size) {
            val current = ordered[index]
            val groupId = current.supersetGroupId
            if (groupId == null) {
                repeat(current.targetSets) { setIndex ->
                    steps += SessionStep(current, setIndex, restAfter = true)
                }
                index += 1
            } else {
                val members = ordered.drop(index).takeWhile { it.supersetGroupId == groupId }
                val rounds = members.maxOf { it.targetSets }
                for (round in 0 until rounds) {
                    members.forEachIndexed { memberIndex, member ->
                        if (round < member.targetSets) {
                            steps += SessionStep(
                                exercise = member,
                                setIndex = round,
                                restAfter = memberIndex == members.lastIndex
                            )
                        }
                    }
                }
                index += members.size
            }
        }
        return steps
    }
}

/** One logged unit of a guided session: an exercise, which set, and whether rest follows. */
data class SessionStep(
    val exercise: RoutineExercise,
    val setIndex: Int,
    val restAfter: Boolean
)
