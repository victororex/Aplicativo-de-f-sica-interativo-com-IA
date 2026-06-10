package com.example.testes.data.api

import com.example.testes.data.local.LocalBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

class AuthApiClient {
    suspend fun register(name: String, email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseAuthResponse(LocalBackend.register(name, email, password))
            }
        }

    suspend fun login(email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseAuthResponse(LocalBackend.login(email, password))
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
