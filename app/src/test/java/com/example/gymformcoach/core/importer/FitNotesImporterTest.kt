package com.example.gymformcoach.core.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FitNotesImporterTest {
    private val importer = FitNotesImporter()

    private fun csv(vararg lines: String) = ByteArrayInputStream(lines.joinToString("\n").toByteArray())

    @Test
    fun `detects FitNotes header`() {
        assertTrue(importer.canParse("Date,Exercise,Category,Weight (kg),Weight (lbs),Reps,Distance,Distance Unit,Time,Comment"))
    }

    @Test
    fun `does not detect an unrelated header`() {
        assertTrue(!importer.canParse("title,start_time,exercise_title,weight_kg,reps"))
    }

    @Test
    fun `parses a weighted reps row`() {
        val result = importer.parse(
            csv(
                "Date,Exercise,Category,Weight (kg),Weight (lbs),Reps,Distance,Distance Unit,Time,Comment",
                "2023-05-01,Bench Press,Chest,60,,8,,,,"
            )
        )
        assertEquals(1, result.sets.size)
        val set = result.sets.first()
        assertEquals(60f, set.weightKg!!, 0.01f)
        assertEquals(8, set.reps)
        assertTrue(!set.isCardio)
    }

    @Test
    fun `converts lbs to kg when only lbs is populated`() {
        val result = importer.parse(
            csv(
                "Date,Exercise,Category,Weight (kg),Weight (lbs),Reps,Distance,Distance Unit,Time,Comment",
                "2023-05-01,Bench Press,Chest,,135,8,,,,"
            )
        )
        // 135 lbs ~= 61.2 kg
        assertEquals(61.23f, result.sets.first().weightKg!!, 0.1f)
    }

    @Test
    fun `rows with distance or time and no weight or reps import as cardio`() {
        val result = importer.parse(
            csv(
                "Date,Exercise,Category,Weight (kg),Weight (lbs),Reps,Distance,Distance Unit,Time,Comment",
                "2023-05-01,Running,Cardio,,,,5000,m,25:00,"
            )
        )
        assertTrue(result.sets.first().isCardio)
        assertEquals(1500, result.sets.first().durationSeconds) // 25:00 -> 1500s
    }

    @Test
    fun `malformed rows are counted not thrown`() {
        val result = importer.parse(
            csv(
                "Date,Exercise,Category,Weight (kg),Weight (lbs),Reps,Distance,Distance Unit,Time,Comment",
                "not-a-date,Bench Press,Chest,60,,8,,,,",
                "2023-05-01,Squat,Legs,80,,5,,,,"
            )
        )
        assertEquals(1, result.sets.size)
        assertEquals(1, result.malformedRowCount)
    }
}

class ImportPipelineMatchingTest {
    @Test
    fun `exercise name variants normalize to the same key`() {
        assertEquals(
            ImportPipeline.normalize("Bench Press (Barbell)"),
            ImportPipeline.normalize("Barbell Bench Press")
        )
    }

    @Test
    fun `unrelated exercise names do not match`() {
        assertNull(
            listOf("Squat").firstOrNull { ImportPipeline.normalize(it) == ImportPipeline.normalize("Deadlift") }
        )
    }
}
