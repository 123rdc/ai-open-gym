package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class BodyWeightRepository(database: AppDatabase) {
    private val dao = database.bodyWeightDao()

    fun getAll(): Flow<List<BodyWeightEntry>> = dao.getAll()

    fun getFrom(date: LocalDate): Flow<List<BodyWeightEntry>> = dao.getFrom(date.toEpochDay())

    suspend fun getLatest(): BodyWeightEntry? = dao.getLatestOnce()

    /**
     * §17A.1: same-date writes overwrite. The unique index on epochDay plus
     * REPLACE gives that without a read-modify-write race.
     */
    suspend fun record(
        weightKg: Float,
        date: LocalDate = LocalDate.now(),
        source: String = BodyWeightSource.MANUAL
    ) {
        dao.upsert(
            BodyWeightEntry(
                weightKg = weightKg,
                epochDay = date.toEpochDay(),
                source = source
            )
        )
    }

    suspend fun delete(date: LocalDate) = dao.delete(date.toEpochDay())
}
