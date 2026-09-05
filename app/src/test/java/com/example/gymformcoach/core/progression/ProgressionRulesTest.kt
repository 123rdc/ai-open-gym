package com.example.gymformcoach.core.progression

import com.example.gymformcoach.core.data.ExerciseType
import com.example.gymformcoach.core.data.LoadType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §20.5: five cases per rule - target hit, target missed, stall reaching deload,
 * no history, stale history. These catch essentially every progression bug worth
 * catching, and they run without a device because the rules are pure functions.
 */
class ProgressionRulesTest {

    private val config = ProgressionConfig()
    private val weightedSpec = ExerciseSpec(targetReps = 5, targetSets = 3)

    private fun daysAgo(n: Int): Long = System.currentTimeMillis() - n * 24L * 60 * 60 * 1000

    private fun session(weight: Float, reps: Int, sets: Int = 3, daysAgo: Int = 1) =
        SessionRecord(
            performedAt = daysAgo(daysAgo),
            sets = List(sets) { SetRecord(weightKg = weight, reps = reps) }
        )

    // ---------- LINEAR ----------

    @Test
    fun `linear advances when target hit`() {
        val target = LinearRule.nextTarget(listOf(session(60f, 5)), config, weightedSpec)
        assertNotNull(target)
        assertEquals(62.5f, target!!.weightKg!!, 0.01f)
        assertTrue(target.reasoning.contains("+2.5kg"))
    }

    @Test
    fun `linear repeats weight when target missed`() {
        val target = LinearRule.nextTarget(listOf(session(60f, 4)), config, weightedSpec)
        assertEquals(60f, target!!.weightKg!!, 0.01f)
        assertTrue(target.reasoning.contains("repeating"))
    }

    @Test
    fun `linear deloads after stall threshold`() {
        val history = listOf(session(60f, 4, daysAgo = 1), session(60f, 4, daysAgo = 3), session(60f, 4, daysAgo = 5))
        val target = LinearRule.nextTarget(history, config, weightedSpec)
        assertEquals(54f, target!!.weightKg!!, 0.01f)
        assertTrue(target.reasoning.contains("deload"))
    }

    @Test
    fun `linear returns null with no history`() {
        assertNull(LinearRule.nextTarget(emptyList(), config, weightedSpec))
    }

    @Test
    fun `linear eases back on stale history`() {
        val target = LinearRule.nextTarget(listOf(session(60f, 5, daysAgo = 120)), config, weightedSpec)
        assertEquals(54f, target!!.weightKg!!, 0.01f)
        assertTrue(target.reasoning.contains("days ago"))
    }

    // ---------- GREYSKULL ----------

    private fun amrapSession(weight: Float, normalReps: Int, amrapReps: Int, daysAgo: Int = 1) =
        SessionRecord(
            performedAt = daysAgo(daysAgo),
            sets = listOf(
                SetRecord(weightKg = weight, reps = normalReps),
                SetRecord(weightKg = weight, reps = normalReps),
                SetRecord(weightKg = weight, reps = amrapReps)
            )
        )

    @Test
    fun `greyskull double jumps on big amrap`() {
        val target = GreyskullRule.nextTarget(listOf(amrapSession(60f, 5, 11)), config, weightedSpec)
        assertEquals(65f, target!!.weightKg!!, 0.01f)
        assertTrue(target.isAmrapTopSet)
        assertTrue(target.reasoning.contains("double jump"))
    }

    @Test
    fun `greyskull normal increment when amrap meets but does not exceed threshold`() {
        val target = GreyskullRule.nextTarget(listOf(amrapSession(60f, 5, 6)), config, weightedSpec)
        assertEquals(62.5f, target!!.weightKg!!, 0.01f)
    }

    @Test
    fun `greyskull repeats when amrap short of target`() {
        val target = GreyskullRule.nextTarget(listOf(amrapSession(60f, 5, 3)), config, weightedSpec)
        assertEquals(60f, target!!.weightKg!!, 0.01f)
        assertTrue(target.reasoning.contains("repeating"))
    }

    @Test
    fun `greyskull deloads after stall threshold`() {
        val history = listOf(
            amrapSession(60f, 4, 4, daysAgo = 1),
            amrapSession(60f, 4, 4, daysAgo = 3),
            amrapSession(60f, 4, 4, daysAgo = 5)
        )
        val target = GreyskullRule.nextTarget(history, config, weightedSpec)
        assertEquals(54f, target!!.weightKg!!, 0.01f)
    }

    @Test
    fun `greyskull returns null with no history`() {
        assertNull(GreyskullRule.nextTarget(emptyList(), config, weightedSpec))
    }

    // ---------- DOUBLE PROGRESSION ----------

    private val rangeSpec = ExerciseSpec(targetReps = 12, targetSets = 3)

    @Test
    fun `double progression adds weight and resets reps at top of range`() {
        val target = DoubleProgressionRule.nextTarget(listOf(session(40f, 12)), config, rangeSpec)
        assertEquals(42.5f, target!!.weightKg!!, 0.01f)
        assertEquals(8, target.reps)
        assertTrue(target.reasoning.contains("back to 8 reps"))
    }

    @Test
    fun `double progression climbs reps within range`() {
        val target = DoubleProgressionRule.nextTarget(listOf(session(40f, 9)), config, rangeSpec)
        assertEquals(40f, target!!.weightKg!!, 0.01f)
        assertEquals(10, target.reps)
    }

    @Test
    fun `double progression caps reps at range max`() {
        val target = DoubleProgressionRule.nextTarget(
            listOf(
                SessionRecord(
                    daysAgo(1),
                    listOf(SetRecord(weightKg = 40f, reps = 12), SetRecord(weightKg = 40f, reps = 11))
                )
            ),
            config, rangeSpec
        )
        assertEquals(12, target!!.reps)
    }

    @Test
    fun `double progression returns null with no history`() {
        assertNull(DoubleProgressionRule.nextTarget(emptyList(), config, rangeSpec))
    }

    @Test
    fun `double progression eases back on stale history`() {
        val target = DoubleProgressionRule.nextTarget(listOf(session(40f, 12, daysAgo = 200)), config, rangeSpec)
        assertTrue(target!!.reasoning.contains("days ago"))
    }

    // ---------- TIME BASED ----------

    private val timedSpec = ExerciseSpec(
        exerciseType = ExerciseType.TIMED,
        loadType = LoadType.BODYWEIGHT,
        targetDurationSeconds = 60,
        targetSets = 3
    )

    private fun timedSession(seconds: Int, daysAgo: Int = 1) =
        SessionRecord(daysAgo(daysAgo), listOf(SetRecord(durationSeconds = seconds)))

    @Test
    fun `time based adds seconds when hold met`() {
        val target = TimeBasedRule.nextTarget(listOf(timedSession(60)), config, timedSpec)
        assertEquals(65, target!!.durationSeconds)
    }

    @Test
    fun `time based repeats when hold short`() {
        val target = TimeBasedRule.nextTarget(listOf(timedSession(47)), config, timedSpec)
        assertEquals(60, target!!.durationSeconds)
        assertTrue(target.reasoning.contains("47s"))
    }

    @Test
    fun `time based deloads after stall threshold`() {
        val history = listOf(timedSession(40, 1), timedSession(42, 3), timedSession(38, 5))
        val target = TimeBasedRule.nextTarget(history, config, timedSpec)
        assertEquals(54, target!!.durationSeconds)
        assertTrue(target.reasoning.contains("deload"))
    }

    @Test
    fun `time based returns null with no history`() {
        assertNull(TimeBasedRule.nextTarget(emptyList(), config, timedSpec))
    }

    @Test
    fun `time based logs actual held time not target`() {
        // §1.5: a 60s target where the user stopped at 47s logs 47 - the rule must
        // read the achieved duration, not assume the target was met.
        val target = TimeBasedRule.nextTarget(listOf(timedSession(47)), config, timedSpec)
        assertTrue(target!!.reasoning.contains("Held 47s"))
    }

    // ---------- BODYWEIGHT OVERRIDE (§5.2.6) ----------

    private val bodyweightSpec = ExerciseSpec(
        loadType = LoadType.BODYWEIGHT,
        targetReps = 10,
        targetSets = 3
    )

    private fun bodyweightSession(reps: Int, addedKg: Float? = null, daysAgo: Int = 1) =
        SessionRecord(
            daysAgo(daysAgo),
            List(3) { SetRecord(addedWeightKg = addedKg, reps = reps) }
        )

    @Test
    fun `bodyweight progresses reps not weight under any rule`() {
        val target = LinearRule.nextTarget(listOf(bodyweightSession(10)), config, bodyweightSpec)
        assertNull(target!!.weightKg)
        assertEquals(11, target.reps)
    }

    @Test
    fun `bodyweight adds a set past the rep ceiling`() {
        val target = LinearRule.nextTarget(listOf(bodyweightSession(20)), config, bodyweightSpec)
        assertEquals(config.repCeiling, target!!.reps)
        assertEquals(4, target.sets)
        assertTrue(target.reasoning.contains("adding a set"))
    }

    @Test
    fun `bodyweight with added load reverts to weight progression`() {
        val target = LinearRule.nextTarget(listOf(bodyweightSession(10, addedKg = 20f)), config, bodyweightSpec)
        assertNotNull(target!!.weightKg)
        assertEquals(22.5f, target.weightKg!!, 0.01f)
    }

    @Test
    fun `unilateral bodyweight steps reps by two`() {
        // §1.4: targets step by 2 so they always split evenly across both sides.
        val spec = bodyweightSpec.copy(isUnilateral = true)
        val target = LinearRule.nextTarget(listOf(bodyweightSession(10)), config, spec)
        assertEquals(12, target!!.reps)
    }

    // ---------- NONE ----------

    @Test
    fun `none rule pre-fills last session verbatim`() {
        val target = NoneRule.nextTarget(listOf(session(60f, 5)), config, weightedSpec)
        assertEquals(60f, target!!.weightKg!!, 0.01f)
        assertEquals(5, target.reps)
        assertEquals("Same as last session.", target.reasoning)
    }

    @Test
    fun `none rule returns null with no history`() {
        assertNull(NoneRule.nextTarget(emptyList(), config, weightedSpec))
    }
}
