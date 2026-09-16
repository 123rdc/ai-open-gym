package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

object ChatRole {
    const val USER = "user"
    const val ASSISTANT = "assistant"
    const val SYSTEM = "system"
}

/** Coach Chat (free-form Q&A) - a separate surface from the §9 post-set analysis and §17D plan Coach. */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
