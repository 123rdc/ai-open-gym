package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow

class ExerciseRepository(database: AppDatabase) {
    private val dao = database.exerciseDao()

    fun getByBodyPart(bodyPart: String): Flow<List<Exercise>> = dao.getByBodyPart(bodyPart)

    fun getAll(): Flow<List<Exercise>> = dao.getAll()

    suspend fun findByName(name: String): Exercise? = dao.findByName(name)

    suspend fun addCustomExercise(exercise: Exercise) = dao.insert(exercise.copy(isCustom = true))
}
