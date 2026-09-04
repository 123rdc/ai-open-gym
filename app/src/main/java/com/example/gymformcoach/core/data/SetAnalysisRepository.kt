package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow

class SetAnalysisRepository(database: AppDatabase) {
    private val dao = database.setAnalysisDao()

    suspend fun save(analysis: SetAnalysis) = dao.insert(analysis)

    fun observeForSession(sessionId: String): Flow<SetAnalysis?> = dao.getForSession(sessionId)
}
