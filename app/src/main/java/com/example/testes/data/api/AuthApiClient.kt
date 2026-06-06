package com.example.testes.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val privateAccount: Boolean = false,
    val notificationsEnabled: Boolean = true
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val user: AuthUser
)

object SessionManager {
    var accessToken: String? = null
        private set

    var user: AuthUser? = null
        private set

    fun saveSession(response: AuthResponse) {
        accessToken = response.accessToken
        user = response.user
    }

    fun clear() {
        accessToken = null
        user = null
    }
}

class AuthApiClient(
    private val baseUrl: String = "http://10.0.2.2:8000"
) {
    suspend fun register(name: String, email: String, password: String): Result<AuthResponse> =
        postAuth(
            path = "/auth/register",
            body = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("password", password)
            }
        )

    suspend fun login(email: String, password: String): Result<AuthResponse> =
        postAuth(
            path = "/auth/login",
            body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
        )

    private suspend fun postAuth(path: String, body: JSONObject): Result<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$baseUrl$path")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (connection.responseCode !in 200..299) {
                val message = runCatching {
                    JSONObject(responseBody).optString("detail", responseBody)
                }.getOrDefault(responseBody)
                error("Erro ${connection.responseCode}: $message")
            }

            parseAuthResponse(JSONObject(responseBody))
        }
    }

    private fun parseAuthResponse(json: JSONObject): AuthResponse {
        val userJson = json.getJSONObject("user")
        return AuthResponse(
            accessToken = json.getString("access_token"),
            tokenType = json.optString("token_type", "bearer"),
            user = AuthUser(
                id = userJson.getInt("id"),
                name = userJson.getString("name"),
                email = userJson.getString("email"),
                phone = userJson.optStringOrNull("phone"),
                privateAccount = userJson.optBoolean("private_account", false),
                notificationsEnabled = userJson.optBoolean("notifications_enabled", true)
            )
        )
    }
}

fun JSONObject.optStringOrNull(name: String): String? {
    return if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }
}
