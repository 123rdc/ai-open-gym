package com.example.gymformcoach.core.coach

import kotlinx.serialization.Serializable

/**
 * §17D.2: a structured proposal, not prose. Mirrors the JSON schema the LLM is
 * asked to return - kept separate from every Room entity so a malformed or
 * malicious response can never deserialize directly into something the DB
 * layer trusts. Everything here passes through [PlanValidator] before a
 * single write happens.
 */
@Serializable
data class ProposedExercise(
    val exerciseId: String,
    val sets: Int,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val supersetGroup: String? = null,
    val why: String = ""
)

@Serializable
data class ProposedRoutine(
    val name: String,
    val progressionRule: String,
    val exercises: List<ProposedExercise>
)

@Serializable
data class ProposedWeeklyAssignment(val dayOfWeek: Int, val routineName: String)

@Serializable
data class TrainingPlanProposal(
    val planName: String,
    val rationale: String,
    val weeklyPlan: List<ProposedWeeklyAssignment>,
    val routines: List<ProposedRoutine>
)

/** §17D.3: a diff against the existing plan, never a silent replacement. */
sealed class PlanDiffItem {
    abstract val routineName: String
    abstract val why: String

    data class SwapExercise(
        override val routineName: String,
        val oldExerciseId: String,
        val newExerciseId: String,
        override val why: String
    ) : PlanDiffItem()

    data class AdjustVolume(
        override val routineName: String,
        val exerciseId: String,
        val newSets: Int,
        override val why: String
    ) : PlanDiffItem()

    data class AddExercise(
        override val routineName: String,
        val exerciseId: String,
        val sets: Int,
        val repRangeMin: Int,
        val repRangeMax: Int,
        override val why: String
    ) : PlanDiffItem()
}

data class PlanRevision(val summary: String, val items: List<PlanDiffItem>)
