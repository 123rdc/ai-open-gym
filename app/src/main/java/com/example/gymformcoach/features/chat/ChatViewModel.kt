package com.example.gymformcoach.features.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymformcoach.core.analysis.AiCoachApiClient
import com.example.gymformcoach.core.chat.ChatRepository
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ChatMessage
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val OFFLINE_MESSAGE = "Coach is offline — check your local server connection in Settings."

sealed class StreamingState {
    data object Idle : StreamingState()
    data object Connecting : StreamingState()
    data class Streaming(val partial: String) : StreamingState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(
        AppDatabase.getInstance(application),
        PreferenceManager(application)
    )

    val messages: StateFlow<List<ChatMessage>> = repository.observeMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streaming = MutableStateFlow<StreamingState>(StreamingState.Idle)
    val streaming: StateFlow<StreamingState> = _streaming.asStateFlow()

    private var activeJob: Job? = null

    /** §3: a new send cancels any reply still streaming for a prior message. */
    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            repository.saveUserMessage(trimmed)
            runReply()
        }
    }

    /** §6: retry affordance - re-requests a reply for the existing history, no duplicate user bubble. */
    fun retryLastReply() {
        activeJob?.cancel()
        activeJob = viewModelScope.launch { runReply() }
    }

    fun cancelStreaming() {
        activeJob?.cancel()
        activeJob = null
        _streaming.value = StreamingState.Idle
    }

    fun clearConversation() {
        activeJob?.cancel()
        activeJob = null
        _streaming.value = StreamingState.Idle
        viewModelScope.launch { repository.clear() }
    }

    private suspend fun runReply() {
        _streaming.value = StreamingState.Connecting

        if (!repository.isConfigured()) {
            repository.saveSystemNotice(OFFLINE_MESSAGE)
            _streaming.value = StreamingState.Idle
            return
        }

        val builder = StringBuilder()
        var failed = false
        try {
            repository.streamReply().collect { event ->
                when (event) {
                    is AiCoachApiClient.StreamEvent.Token -> {
                        builder.append(event.text)
                        _streaming.value = StreamingState.Streaming(builder.toString())
                    }
                    is AiCoachApiClient.StreamEvent.Error -> failed = true
                    is AiCoachApiClient.StreamEvent.Done -> Unit
                }
            }
        } catch (e: CancellationException) {
            // Navigated away or superseded by a new send - drop the partial reply, don't persist it.
            _streaming.value = StreamingState.Idle
            throw e
        }

        _streaming.value = StreamingState.Idle
        when {
            failed || builder.isEmpty() -> repository.saveSystemNotice(OFFLINE_MESSAGE)
            else -> repository.saveAssistantMessage(builder.toString())
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeJob?.cancel()
    }
}
