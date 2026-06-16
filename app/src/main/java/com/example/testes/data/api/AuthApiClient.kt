package com.example.testes.data.api

import android.content.Context
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
    private const val PREFS = "fisica_interativa_session"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_USER = "user"
    private var appContext: Context? = null

    var accessToken: String? = null
        private set

    var user: AuthUser? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        val userRaw = prefs.getString(KEY_USER, null)
        if (token.isNullOrBlank() || userRaw.isNullOrBlank()) {
            clear(recordLogout = false)
            return
        }
        runCatching {
            accessToken = token
            user = parseUser(JSONObject(userRaw))
            LocalBackend.currentUser(token)
        }.onFailure {
            clear(recordLogout = false)
        }
    }

    val isLoggedIn: Boolean
        get() = !accessToken.isNullOrBlank() && user != null

    fun saveSession(response: AuthResponse) {
        accessToken = response.accessToken
        user = response.user
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_TOKEN, response.accessToken)
            ?.putString(KEY_USER, response.user.toJson().toString())
            ?.apply()
        runCatching { LocalBackend.startStudySession(response.accessToken) }
    }

    fun clear(recordLogout: Boolean = true) {
        accessToken?.let { token -> runCatching { LocalBackend.completeStudySession(token) } }
        if (recordLogout) accessToken?.let { token ->
            runCatching { LocalBackend.recordAuthEvent(token, "account_logged_out") }
        }
        accessToken = null
        user = null
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.clear()
            ?.apply()
    }

    private fun AuthUser.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("email", email)
            .put("phone", phone ?: JSONObject.NULL)
            .put("private_account", privateAccount)
            .put("notifications_enabled", notificationsEnabled)
    }

    private fun parseUser(json: JSONObject): AuthUser {
        return AuthUser(
            id = json.getInt("id"),
            name = json.getString("name"),
            email = json.getString("email"),
            phone = json.optStringOrNull("phone"),
            privateAccount = json.optBoolean("private_account", false),
            notificationsEnabled = json.optBoolean("notifications_enabled", true)
        )
    }
}

class AuthApiClient {
    suspend fun register(name: String, email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseAuthResponse(LocalBackend.register(name, email, password)).also {
                    LocalBackend.recordAuthEvent(it.accessToken, "account_logged_in")
                }
            }
        }

    suspend fun login(email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseAuthResponse(LocalBackend.login(email, password)).also {
                    LocalBackend.recordAuthEvent(it.accessToken, "account_logged_in")
                }
            }
        }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                LocalBackend.resetPassword(email, newPassword)
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
