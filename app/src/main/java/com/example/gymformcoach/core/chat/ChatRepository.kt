package com.example.gymformcoach.core.chat

import com.example.gymformcoach.core.analysis.AiCoachApiClient
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ChatMessage
import com.example.gymformcoach.core.data.ChatMessageRepository
import com.example.gymformcoach.core.data.ChatRole
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.flow.Flow

/**
 * Orchestrates Coach Chat (free-form Q&A, separate from §9's automatic post-set analysis and
 * §17D's plan Coach): persists messages locally and streams replies from the same configured
 * endpoint the rest of the AI coach features use.
 */
class ChatRepository(
    private val database: AppDatabase,
    private val preferenceManager: PreferenceManager
) {
    private val messages = ChatMessageRepository(database)

    fun observeMessages(): Flow<List<ChatMessage>> = messages.observeAll()

    suspend fun saveUserMessage(content: String): ChatMessage {
        val message = ChatMessage(role = ChatRole.USER, content = content)
        messages.save(message)
        return message
    }

    suspend fun saveAssistantMessage(content: String) {
        messages.save(ChatMessage(role = ChatRole.ASSISTANT, content = content))
    }

    /** Inline "coach offline" / error bubbles (§6) - never sent back to the model as a turn. */
    suspend fun saveSystemNotice(content: String) {
        messages.save(ChatMessage(role = ChatRole.SYSTEM, content = content))
    }

    suspend fun deleteMessage(id: String) = messages.deleteMessage(id)

    suspend fun clear() = messages.clear()

    fun isConfigured(): Boolean = preferenceManager.aiCoachApiUrl.isNotBlank()

    /** §4: last [HISTORY_LIMIT] turns, oldest first - trimmed rather than growing unbounded. */
    suspend fun streamReply(): Flow<AiCoachApiClient.StreamEvent> {
        val systemPrompt = ChatContextBuilder.build(database, preferenceManager)
        val history = messages.getRecentOnce(HISTORY_LIMIT)
            .filter { it.role != ChatRole.SYSTEM }
            .map { it.role to it.content }

        return AiCoachApiClient.streamChatCompletion(
            apiUrl = preferenceManager.aiCoachApiUrl,
            apiKey = preferenceManager.aiCoachApiKey,
            model = preferenceManager.aiCoachModel,
            messages = listOf(ChatRole.SYSTEM to systemPrompt) + history
        )
    }

    companion object {
        private const val HISTORY_LIMIT = 15
    }
}
