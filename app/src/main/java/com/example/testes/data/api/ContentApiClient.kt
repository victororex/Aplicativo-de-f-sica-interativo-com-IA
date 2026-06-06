package com.example.testes.data.api

import com.example.testes.model.Lesson
import com.example.testes.model.Progress
import com.example.testes.model.Subject
import com.example.testes.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ContentApiClient(
    private val baseUrl: String = "http://10.0.2.2:8000"
) {
    suspend fun getCurrentUser(): Result<User> = get("/auth/me") { json ->
        parseUser(json)
    }

    suspend fun updateCurrentUser(
        name: String,
        email: String,
        phone: String?,
        privateAccount: Boolean,
        notificationsEnabled: Boolean
    ): Result<User> =
        request(
            path = "/users/me",
            method = "PUT",
            body = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("phone", phone)
                put("private_account", privateAccount)
                put("notifications_enabled", notificationsEnabled)
            }
        ) { json -> parseUser(json) }

    suspend fun getSubjects(): Result<List<Subject>> = getArray("/content/subjects") { array ->
        List(array.length()) { index -> parseSubject(array.getJSONObject(index)) }
    }

    suspend fun getLessons(subjectId: String): Result<List<Lesson>> =
        getArray("/content/subjects/$subjectId/lessons") { array ->
            List(array.length()) { index -> parseLessonSummary(array.getJSONObject(index)) }
        }

    suspend fun getLesson(lessonId: String): Result<Lesson> =
        get("/content/lessons/$lessonId") { json -> parseLessonDetail(json) }

    suspend fun getProgress(): Result<Progress> =
        get("/progress/summary") { json -> parseProgress(json) }

    suspend fun completeLesson(lessonId: String): Result<Progress> =
        request(
            path = "/progress/lessons/$lessonId/complete",
            method = "POST",
            body = null
        ) { json -> parseProgress(json) }

    private suspend fun <T> get(path: String, parser: (JSONObject) -> T): Result<T> =
        request(path = path, method = "GET", body = null, parser = parser)

    private suspend fun <T> getArray(path: String, parser: (JSONArray) -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val response = execute(path = path, method = "GET", body = null)
            parser(JSONArray(response))
        }
    }

    private suspend fun <T> request(
        path: String,
        method: String,
        body: JSONObject?,
        parser: (JSONObject) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val response = execute(path = path, method = method, body = body)
            parser(JSONObject(response))
        }
    }

    private fun execute(path: String, method: String, body: JSONObject?): String {
        val url = URL("$baseUrl$path")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            SessionManager.accessToken?.let { token ->
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        if (body != null) {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }
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
        return responseBody
    }

    private fun parseSubject(json: JSONObject): Subject {
        return Subject(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.getString("description"),
            examFocus = json.getString("exam_focus"),
            totalLessons = json.getInt("total_lessons"),
            completedLessons = json.getInt("completed_lessons"),
            progress = json.getDouble("progress").toFloat(),
            isCompleted = json.getBoolean("is_completed")
        )
    }

    private fun parseUser(json: JSONObject): User {
        return User(
            id = json.getInt("id").toString(),
            name = json.getString("name"),
            email = json.getString("email"),
            phone = json.optStringOrNull("phone"),
            privateAccount = json.optBoolean("private_account", false),
            notificationsEnabled = json.optBoolean("notifications_enabled", true)
        )
    }

    private fun parseLessonSummary(json: JSONObject): Lesson {
        return Lesson(
            id = json.getString("id"),
            title = json.getString("title"),
            description = json.getString("description"),
            content = "",
            module = json.getString("subject_name"),
            subjectId = json.getString("subject_id"),
            examTags = json.getString("exam_tags"),
            isCompleted = json.getBoolean("is_completed")
        )
    }

    private fun parseLessonDetail(json: JSONObject): Lesson {
        return Lesson(
            id = json.getString("id"),
            title = json.getString("title"),
            description = json.getString("description"),
            content = json.getString("content"),
            module = json.getString("subject_name"),
            subjectId = json.getString("subject_id"),
            examTags = json.getString("exam_tags"),
            isCompleted = json.getBoolean("is_completed")
        )
    }

    private fun parseProgress(json: JSONObject): Progress {
        val completedArray = json.getJSONArray("completed_lessons")
        return Progress(
            userId = json.optInt("user_id").toString(),
            completedLessons = List(completedArray.length()) { index -> completedArray.getString(index) },
            currentModule = json.getString("current_module"),
            overallCompletion = json.getDouble("overall_completion").toFloat()
        )
    }
}
