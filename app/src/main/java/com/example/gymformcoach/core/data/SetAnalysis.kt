package com.example.gymformcoach.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "set_analyses",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseSession::class,
            parentColumns = ["id"],
            childColumns = ["exerciseSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseSessionId")]
)
data class SetAnalysis(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseSessionId: String,
    val mainIssue: String,
    val trendSummary: String,
    val correctives: List<String>,
    val syncStatus: String = SyncStatus.SYNCED,
    val serverId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
