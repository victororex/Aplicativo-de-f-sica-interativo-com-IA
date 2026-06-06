package com.example.testes.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class QuestionRequest(
    val sessionId: Int? = null,
    val message: String,
    val subject: String? = null,
    val level: String = "universitario"
)

data class AiResponse(
    val sessionId: Int?,
    val userMessage: String,
    val aiResponse: String
)

class ChatApiClient(
    private val baseUrl: String = "http://10.0.2.2:8000"
) {
    suspend fun sendMessage(request: QuestionRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$baseUrl/chat/message")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                SessionManager.accessToken?.let { token ->
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val body = JSONObject().apply {
                put("session_id", request.sessionId)
                put("message", request.message)
                put("subject", request.subject)
                put("level", request.level)
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (connection.responseCode !in 200..299) {
                error("Erro ${connection.responseCode}: $responseBody")
            }

            val json = JSONObject(responseBody)
            AiResponse(
                sessionId = json.optIntOrNull("session_id"),
                userMessage = json.optString("user_message"),
                aiResponse = json.optString("ai_response")
            )
        }
    }

    suspend fun synthesizeSpeech(text: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$baseUrl/chat/speech")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "audio/mpeg")
                SessionManager.accessToken?.let { token ->
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val body = JSONObject().apply {
                put("text", text)
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val bytes = stream.use { it.readBytes() }
            if (connection.responseCode !in 200..299) {
                error("Nao consegui gerar a voz agora.")
            }
            bytes
        }
    }
}

private fun JSONObject.optIntOrNull(name: String): Int? {
    return if (isNull(name)) null else optInt(name)
}
