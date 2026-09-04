package com.example.gymformcoach.core.analysis

import kotlinx.coroutines.Dispatchers
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
