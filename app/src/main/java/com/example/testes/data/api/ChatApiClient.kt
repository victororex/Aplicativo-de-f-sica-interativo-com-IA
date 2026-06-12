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
    val level: String = "universitario"
)

data class AiResponse(
    val sessionId: Int?,
    val userMessage: String,
    val aiResponse: String,
    val usedRemoteAi: Boolean = false,
    val fallbackReason: String? = null
)

class ChatApiClient {
    suspend fun sendMessage(request: QuestionRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        runCatching {
            if (BuildConfig.USE_REMOTE_AI) {
                val remoteResult = runCatching { sendRemoteMessage(request) }
                if (remoteResult.isSuccess) {
                    val remote = remoteResult.getOrThrow()
                    LocalBackend.saveChatExchange(
                        SessionManager.accessToken,
                        remote.userMessage,
                        remote.aiResponse
                    )
                    remote
                } else {
                    val json = LocalBackend.chat(SessionManager.accessToken, request.message)
                    AiResponse(
                        sessionId = json.optIntOrNull("session_id"),
                        userMessage = json.optString("user_message"),
                        aiResponse = json.optString("ai_response"),
                        usedRemoteAi = false,
                        fallbackReason = "A IA online nao respondeu. Usei o tutor local para nao interromper o estudo."
                    )
                }
            } else {
                val json = LocalBackend.chat(SessionManager.accessToken, request.message)
                AiResponse(
                    sessionId = json.optIntOrNull("session_id"),
                    userMessage = json.optString("user_message"),
                    aiResponse = json.optString("ai_response"),
                    usedRemoteAi = false
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
            if (!BuildConfig.USE_REMOTE_AI) {
                throw IllegalStateException("Voz online desativada.")
            }

            val payload = JSONObject().put("text", text)
            val connection = openPost("/chat/speech")
            writeJson(connection, payload)
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Voz online indisponivel.")
            }
            connection.inputStream.use { it.readBytes() }
        }
    }

    private fun sendRemoteMessage(request: QuestionRequest): AiResponse {
        val payload = JSONObject()
            .put("session_id", request.sessionId ?: JSONObject.NULL)
            .put("message", request.message)
            .put("subject", request.subject ?: "Analise Dimensional")
            .put("level", request.level)

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
            aiResponse = json.optString("ai_response", LocalBackend.localTutorAnswer(request.message)),
            usedRemoteAi = true
        )
    }

    private fun openPost(path: String): HttpURLConnection {
        val baseUrl = BuildConfig.AI_API_BASE_URL.trimEnd('/')
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 4500
        connection.readTimeout = 18000
        connection.doInput = true
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", if (path == "/chat/speech") "audio/mpeg" else "application/json")
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
