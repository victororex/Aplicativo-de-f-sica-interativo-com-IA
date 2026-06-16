package com.example.testes.data.api

import com.example.testes.BuildConfig
import com.example.testes.data.local.LocalBackend
import com.example.testes.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class QuestionRequest(
    val sessionId: Int? = null,
    val message: String,
    val subject: String? = null,
    val level: String = "universitario",
    val contextTitle: String? = null,
    val contextTopic: String? = null,
    val source: String? = null
)

data class AiResponse(
    val sessionId: Int?,
    val userMessage: String,
    val aiResponse: String,
    val usedRemoteAi: Boolean = false,
    val fallbackReason: String? = null,
    val responseTimeMs: Long = 0L
)

class ChatApiClient {
    private val fallbackMessage = "Nao consegui responder agora porque a conexao com a IA falhou. Tente novamente em instantes ou revise a aula de Analise Dimensional."

    suspend fun sendMessage(request: QuestionRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val startedAt = System.currentTimeMillis()
            if (BuildConfig.USE_REMOTE_AI) {
                val remoteResult = runCatching { sendRemoteMessage(request) }
                if (remoteResult.isSuccess) {
                    val remote = remoteResult.getOrThrow().copy(responseTimeMs = System.currentTimeMillis() - startedAt)
                    LocalBackend.saveChatExchange(
                        SessionManager.accessToken,
                        remote.userMessage,
                        remote.aiResponse
                    )
                    LocalBackend.recordChatEvent(
                        SessionManager.accessToken,
                        "chat_question_sent",
                        request.contextTopic ?: request.subject ?: "Analise Dimensional",
                        0
                    )
                    LocalBackend.recordChatEvent(
                        SessionManager.accessToken,
                        "chat_api_success",
                        request.contextTopic ?: request.subject ?: "Analise Dimensional",
                        ((remote.responseTimeMs + 999) / 1000).toInt()
                    )
                    LocalBackend.incrementChatStats(SessionManager.accessToken)
                    remote
                } else {
                    val json = LocalBackend.chat(SessionManager.accessToken, request.message)
                    val elapsed = System.currentTimeMillis() - startedAt
                    LocalBackend.recordChatEvent(
                        SessionManager.accessToken,
                        "chat_api_failed",
                        request.contextTopic ?: request.subject ?: "Analise Dimensional",
                        ((elapsed + 999) / 1000).toInt()
                    )
                    AiResponse(
                        sessionId = json.optIntOrNull("session_id"),
                        userMessage = json.optString("user_message"),
                        aiResponse = json.optString("ai_response").ifBlank { fallbackMessage },
                        usedRemoteAi = false,
                        fallbackReason = fallbackMessage,
                        responseTimeMs = elapsed
                    )
                }
            } else {
                val json = LocalBackend.chat(SessionManager.accessToken, request.message)
                val elapsed = System.currentTimeMillis() - startedAt
                AiResponse(
                    sessionId = json.optIntOrNull("session_id"),
                    userMessage = json.optString("user_message"),
                    aiResponse = json.optString("ai_response"),
                    usedRemoteAi = false,
                    responseTimeMs = elapsed
                )
            }
        }
    }

    suspend fun getHistory(): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val array = LocalBackend.chatHistory(SessionManager.accessToken)
            List(array.length()) { index ->
                val json = array.getJSONObject(index)
                ChatMessage(
                    id = json.optLong("time", System.currentTimeMillis() + index).toString(),
                    text = json.optString("text"),
                    isFromUser = json.optBoolean("from_user"),
                    timestamp = json.optLong("time", System.currentTimeMillis())
                )
            }
        }
    }

    suspend fun synthesizeSpeech(text: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {

            val payload = JSONObject().put("text", text)
            val connection = openPost("/chat/speech")
            writeJson(connection, payload)
            val code = connection.responseCode
            if (code !in 200..299) {
                val err = readBody(connection, code)
                throw IllegalStateException(remoteErrorMessage(err).ifBlank { "Voz personalizada indisponível (HTTP $code)." })
            }
            connection.inputStream.use { it.readBytes() }
        }
    }

    suspend fun voiceStatus(): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = BuildConfig.AI_API_BASE_URL.trimEnd('/')
            val connection = URL("$baseUrl/voice/status").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 4000
            val code = connection.responseCode
            val raw = readBody(connection, code)
            if (code !in 200..299) throw IllegalStateException("Status indisponível (HTTP $code).")
            JSONObject(raw)
        }
    }

    private fun sendRemoteMessage(request: QuestionRequest): AiResponse {
        val payload = JSONObject()
            .put("session_id", request.sessionId ?: JSONObject.NULL)
            .put("message", request.message)
            .put("subject", request.subject ?: "Analise Dimensional")
            .put("level", request.level)
            .put("context_title", request.contextTitle ?: JSONObject.NULL)
            .put("context_topic", request.contextTopic ?: JSONObject.NULL)
            .put("source", request.source ?: JSONObject.NULL)

        val connection = openPost("/chat/message")
        writeJson(connection, payload)
        val code = connection.responseCode
        val raw = readBody(connection, code)

        if (code !in 200..299) {
            throw IllegalStateException(remoteErrorMessage(raw))
        }

        val json = JSONObject(raw)
        return AiResponse(
            sessionId = json.optIntOrNull("session_id"),
            userMessage = json.optString("user_message", request.message),
            aiResponse = json.optString("ai_response", fallbackMessage).ifBlank { fallbackMessage },
            usedRemoteAi = true
        )
    }

    private fun openPost(path: String): HttpURLConnection {
        val baseUrl = BuildConfig.AI_API_BASE_URL.trimEnd('/')
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 4500
        connection.readTimeout = if (path == "/chat/speech") 180_000 else 18_000
        connection.doInput = true
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", if (path == "/chat/speech") "audio/wav" else "application/json")
        return connection
    }

    private fun writeJson(connection: HttpURLConnection, payload: JSONObject) {
        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(payload.toString())
        }
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
        }.orEmpty()
    }

    private fun remoteErrorMessage(raw: String): String {
        return runCatching {
            JSONObject(raw).optString("detail")
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "A IA online nao respondeu."
    }
}

private fun JSONObject.optIntOrNull(name: String): Int? {
    return if (isNull(name)) null else optInt(name)
}
