package com.example.testes.data.api

import com.example.testes.data.local.LocalBackend
import com.example.testes.model.Lesson
import com.example.testes.model.Progress
import com.example.testes.model.Subject
import com.example.testes.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ContentApiClient {
    suspend fun getCurrentUser(): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            parseUser(LocalBackend.currentUser(SessionManager.accessToken))
        }
    }

    suspend fun updateCurrentUser(
        name: String,
        email: String,
        phone: String?,
        privateAccount: Boolean,
        notificationsEnabled: Boolean
    ): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val user = parseUser(
                LocalBackend.updateCurrentUser(
                    SessionManager.accessToken,
                    name,
                    email,
                    phone,
                    privateAccount,
                    notificationsEnabled
                )
            )
            SessionManager.user?.let {
                SessionManager.saveSession(
                    AuthResponse(
                        accessToken = SessionManager.accessToken.orEmpty(),
                        tokenType = "local",
                        user = AuthUser(
                            id = user.id.toIntOrNull() ?: it.id,
                            name = user.name,
                            email = user.email,
                            phone = user.phone,
                            privateAccount = user.privateAccount,
                            notificationsEnabled = user.notificationsEnabled
                        )
                    )
                )
            }
            user
        }
    }

    suspend fun getSubjects(): Result<List<Subject>> = withContext(Dispatchers.IO) {
        runCatching {
            val array = LocalBackend.subjects(SessionManager.accessToken)
            List(array.length()) { index -> parseSubject(array.getJSONObject(index)) }
        }
    }

    suspend fun getLessons(subjectId: String): Result<List<Lesson>> = withContext(Dispatchers.IO) {
        runCatching {
            val array = LocalBackend.lessons(SessionManager.accessToken, subjectId)
            List(array.length()) { index -> parseLessonSummary(array.getJSONObject(index)) }
        }
    }

    suspend fun getLesson(lessonId: String): Result<Lesson> = withContext(Dispatchers.IO) {
        runCatching {
            parseLessonDetail(LocalBackend.lesson(SessionManager.accessToken, lessonId))
        }
    }

    suspend fun getProgress(): Result<Progress> = withContext(Dispatchers.IO) {
        runCatching {
            parseProgress(LocalBackend.progressSummary(SessionManager.accessToken))
        }
    }

    suspend fun completeLesson(lessonId: String): Result<Progress> = withContext(Dispatchers.IO) {
        runCatching {
            parseProgress(LocalBackend.completeLesson(SessionManager.accessToken, lessonId))
        }
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
