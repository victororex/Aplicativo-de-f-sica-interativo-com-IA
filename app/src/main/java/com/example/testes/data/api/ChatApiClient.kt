package com.example.testes.data.api

import com.example.testes.data.local.LocalBackend
import com.example.testes.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

class ChatApiClient {
    suspend fun sendMessage(request: QuestionRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val json = LocalBackend.chat(SessionManager.accessToken, request.message)
            AiResponse(
                sessionId = json.optIntOrNull("session_id"),
                userMessage = json.optString("user_message"),
                aiResponse = json.optString("ai_response")
            )
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
        Result.failure(IllegalStateException("Voz remota desativada na demo local."))
    }
}

private fun JSONObject.optIntOrNull(name: String): Int? {
    return if (isNull(name)) null else optInt(name)
}
