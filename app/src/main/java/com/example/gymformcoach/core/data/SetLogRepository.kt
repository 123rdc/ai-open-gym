package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow

class SetLogRepository(database: AppDatabase) {
    private val dao = database.setLogDao()

    suspend fun logSet(setLog: SetLog) = dao.insert(setLog)

    suspend fun logSets(setLogs: List<SetLog>) = dao.insertAll(setLogs)

    fun getForSession(exerciseSessionId: String): Flow<List<SetLog>> = dao.getForSession(exerciseSessionId)

    suspend fun getForSessionOnce(exerciseSessionId: String): List<SetLog> = dao.getForSessionOnce(exerciseSessionId)
}
