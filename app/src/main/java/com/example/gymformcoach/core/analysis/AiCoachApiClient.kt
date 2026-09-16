package com.example.gymformcoach.core.analysis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to a user-configured OpenAI-compatible chat-completions endpoint. The URL, optional
 * API key, and model are all supplied by the user in Settings - this client makes no assumption
 * about where the endpoint is hosted.
 */
object AiCoachApiClient {

    sealed class ConnectionResult {
        data object Connected : ConnectionResult()
        data class Unreachable(val message: String) : ConnectionResult()
    }

    /** Exercises the exact same request path as a real analysis call, with a trivial prompt. */
    suspend fun testConnection(apiUrl: String, apiKey: String, model: String): ConnectionResult {
        if (apiUrl.isBlank()) return ConnectionResult.Unreachable("No API endpoint configured")
        val result = requestChatCompletion(
            apiUrl = apiUrl,
            apiKey = apiKey,
            model = model,
            systemPrompt = "Reply with only the single word: OK",
            userContent = "Connection test.",
            readTimeoutMs = 8000
        )
        return result.fold(
            onSuccess = { ConnectionResult.Connected },
            onFailure = { e -> ConnectionResult.Unreachable(e.message ?: "Could not reach endpoint") }
        )
    }

    suspend fun requestSetAnalysis(
        apiUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String
    ): Result<String> {
        if (apiUrl.isBlank()) {
            return Result.failure(IllegalStateException("No AI coach API endpoint configured"))
        }
        return requestChatCompletion(apiUrl, apiKey, model, systemPrompt, userContent, readTimeoutMs = 45000)
    }

    /** Coach Chat (§ chat screen). One event per incremental token/chunk; terminates the flow on [Done] or [Error]. */
    sealed class StreamEvent {
        data class Token(val text: String) : StreamEvent()
        data object Done : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    /**
     * Streams a chat completion (`stream: true`) so the chat screen can render tokens as they
     * arrive rather than waiting for the full response. Cancelling collection of the returned
     * flow (navigating away, sending a new message) disconnects the underlying HTTP call.
     */
    fun streamChatCompletion(
        apiUrl: String,
        apiKey: String,
        model: String,
        messages: List<Pair<String, String>> // role to content, in order
    ): Flow<StreamEvent> = callbackFlow {
        if (apiUrl.isBlank()) {
            trySend(StreamEvent.Error("No AI coach API endpoint configured"))
            close()
            return@callbackFlow
        }

        val connection = try {
            (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 60000
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
            }
        } catch (e: Exception) {
            trySend(StreamEvent.Error(e.message ?: "Could not reach endpoint"))
            close()
            return@callbackFlow
        }

        val body = JSONObject().apply {
            if (model.isNotBlank()) put("model", model)
            put("stream", true)
            put(
                "messages",
                JSONArray().apply {
                    messages.forEach { (role, content) ->
                        put(JSONObject().apply { put("role", role); put("content", content) })
                    }
                }
            )
        }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                trySend(StreamEvent.Error("API returned $code: ${errorBody ?: "no body"}"))
                close()
                return@callbackFlow
            }

            connection.inputStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isNotEmpty()) {
                        if (payload == "[DONE]") break
                        val delta = runCatching { extractDeltaContent(payload) }.getOrNull()
                        if (!delta.isNullOrEmpty()) trySend(StreamEvent.Token(delta))
                    }
                    line = reader.readLine()
                }
            }
            trySend(StreamEvent.Done)
        } catch (e: Exception) {
            trySend(StreamEvent.Error(e.message ?: "Connection lost"))
        } finally {
            close()
        }

        awaitClose { connection.disconnect() }
    }.flowOn(Dispatchers.IO)

    /** Accepts both OpenAI-style streaming deltas and Ollama's native `message.content` chunks. */
    private fun extractDeltaContent(payload: String): String? {
        val obj = JSONObject(payload)
        obj.optJSONArray("choices")?.let { choices ->
            if (choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                choice.optJSONObject("delta")?.optString("content", "")?.let { if (it.isNotEmpty()) return it }
                choice.optJSONObject("message")?.optString("content", "")?.let { if (it.isNotEmpty()) return it }
            }
        }
        obj.optJSONObject("message")?.optString("content", "")?.let { if (it.isNotEmpty()) return it }
        return null
    }

    private suspend fun requestChatCompletion(
        apiUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userContent: String,
        readTimeoutMs: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = readTimeoutMs
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
            }

            val body = JSONObject().apply {
                if (model.isNotBlank()) put("model", model)
                put("stream", false)
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                        put(JSONObject().apply { put("role", "user"); put("content", userContent) })
                    }
                )
            }

            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                connection.disconnect()
                return@withContext Result.failure(IOException("API returned $code: ${errorBody ?: "no body"}"))
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val content = JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
