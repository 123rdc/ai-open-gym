package com.example.gymformcoach.core.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanValidatorTest {

    private val catalog = setOf("Squat", "Bench Press", "Deadlift", "Pull-ups")

    private fun exercise(id: String, sets: Int = 3, min: Int = 8, max: Int = 12) =
        ProposedExercise(id, sets, min, max)

    @Test
    fun `drops a hallucinated exercise id not in the supplied catalog`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale", emptyList(),
            listOf(ProposedRoutine("Day 1", "LINEAR", listOf(exercise("Squat"), exercise("Made Up Exercise"))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertEquals(1, result.droppedExerciseCount)
        assertEquals(1, result.proposal.routines.first().exercises.size)
        assertEquals("Squat", result.proposal.routines.first().exercises.first().exerciseId)
    }

    @Test
    fun `drops a duplicate exercise within the same routine`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale", emptyList(),
            listOf(ProposedRoutine("Day 1", "NONE", listOf(exercise("Squat"), exercise("Squat"))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertEquals(1, result.droppedExerciseCount)
        assertEquals(1, result.proposal.routines.first().exercises.size)
    }

    @Test
    fun `drops an exercise with an inverted rep range`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale", emptyList(),
            listOf(ProposedRoutine("Day 1", "NONE", listOf(exercise("Squat", min = 12, max = 8))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertEquals(1, result.droppedExerciseCount)
        assertTrue(result.isEmpty)
        assertEquals(listOf("Day 1"), result.droppedRoutineNames)
    }

    @Test
    fun `clamps an out-of-bounds set count rather than dropping it`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale", emptyList(),
            listOf(ProposedRoutine("Day 1", "NONE", listOf(exercise("Squat", sets = 99))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertEquals(0, result.droppedExerciseCount)
        assertEquals(10, result.proposal.routines.first().exercises.first().sets)
    }

    @Test
    fun `drops a routine entirely when every exercise is invalid`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale", emptyList(),
            listOf(ProposedRoutine("Bad Day", "NONE", listOf(exercise("Not Real"))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertTrue(result.isEmpty)
        assertEquals(listOf("Bad Day"), result.droppedRoutineNames)
    }

    @Test
    fun `coerces an unrecognized progression rule to NONE rather than dropping the routine`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale", emptyList(),
            listOf(ProposedRoutine("Day 1", "MADE_UP_RULE", listOf(exercise("Squat"))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertEquals(1, result.coercedRuleCount)
        assertEquals("NONE", result.proposal.routines.first().progressionRule)
        assertEquals(1, result.proposal.routines.first().exercises.size)
    }

    @Test
    fun `drops a weekly-plan day pointing at an invalid day-of-week`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale",
            listOf(ProposedWeeklyAssignment(9, "Day 1"), ProposedWeeklyAssignment(1, "Day 1")),
            listOf(ProposedRoutine("Day 1", "NONE", listOf(exercise("Squat"))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertEquals(1, result.proposal.weeklyPlan.size)
        assertEquals(1, result.proposal.weeklyPlan.first().dayOfWeek)
    }

    @Test
    fun `drops a weekly-plan entry pointing at a routine that was dropped`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale",
            listOf(ProposedWeeklyAssignment(1, "Bad Day")),
            listOf(ProposedRoutine("Bad Day", "NONE", listOf(exercise("Not Real"))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertTrue(result.proposal.weeklyPlan.isEmpty())
    }

    @Test
    fun `a fully valid proposal passes through unchanged`() {
        val proposal = TrainingPlanProposal(
            "Test Plan", "rationale",
            listOf(ProposedWeeklyAssignment(1, "Day 1")),
            listOf(ProposedRoutine("Day 1", "LINEAR", listOf(exercise("Squat"), exercise("Bench Press"))))
        )
        val result = PlanValidator.validate(proposal, catalog)
        assertEquals(0, result.droppedExerciseCount)
        assertEquals(0, result.coercedRuleCount)
        assertTrue(result.droppedRoutineNames.isEmpty())
        assertEquals(2, result.proposal.routines.first().exercises.size)
    }
}
