package com.example.gymformcoach.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: Exercise)

    @Insert
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT * FROM exercises WHERE bodyPart = :bodyPart ORDER BY name ASC")
    fun getByBodyPart(bodyPart: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Exercise?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}
