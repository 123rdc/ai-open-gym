package com.example.gymformcoach.core.data

import kotlinx.coroutines.flow.Flow

class ChatMessageRepository(database: AppDatabase) {
    private val dao = database.chatMessageDao()

    fun observeAll(): Flow<List<ChatMessage>> = dao.getAll()

    suspend fun save(message: ChatMessage) = dao.insert(message)

    /** Most recent [limit] messages, oldest first - the multi-turn window sent to the model (§4). */
    suspend fun getRecentOnce(limit: Int): List<ChatMessage> = dao.getRecentOnce(limit).asReversed()

    suspend fun deleteMessage(id: String) = dao.delete(id)

    suspend fun clear() = dao.clear()
}
