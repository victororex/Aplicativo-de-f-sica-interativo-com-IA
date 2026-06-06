package com.example.testes.data.api

import com.example.testes.model.AvatarItem
import com.example.testes.model.CampaignExercise
import com.example.testes.model.CampaignNode
import com.example.testes.model.DailyChallengeStatus
import com.example.testes.model.DailyQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LearningApiClient(
    private val baseUrl: String = "http://10.0.2.2:8000"
) {
    suspend fun getDailyChallenge(): Result<List<DailyQuestion>> =
        getArray("/learning/daily-challenge") { array ->
            List(array.length()) { index ->
                val json = array.getJSONObject(index)
                val options = json.getJSONArray("options")
                DailyQuestion(
                    id = json.getString("id"),
                    question = json.getString("question"),
                    options = List(options.length()) { optionIndex -> options.getString(optionIndex) },
                    correctIndex = json.getInt("correct_index"),
                    explanation = json.getString("explanation"),
                    subjectId = json.getString("subject_id"),
                    subjectName = json.getString("subject_name")
                )
            }
        }

    suspend fun getDailyChallengeStatus(): Result<DailyChallengeStatus> =
        getObject("/learning/daily-challenge/status") { json ->
            DailyChallengeStatus(
                completedToday = json.getBoolean("completed_today"),
                score = json.optNullableInt("score"),
                total = json.optNullableInt("total"),
                accuracyRate = json.optNullableInt("accuracy_rate"),
                completedAt = if (json.isNull("completed_at")) null else json.optString("completed_at")
            )
        }

    suspend fun submitDailyChallenge(score: Int, total: Int): Result<Int> =
        post("/learning/daily-challenge/submit", JSONObject().apply {
            put("score", score)
            put("total", total)
        }) { json -> json.getInt("accuracy_rate") }

    suspend fun submitCampaignStage(nodeId: String, score: Int, total: Int): Result<Int> =
        post("/learning/campaign/$nodeId/submit", JSONObject().apply {
            put("score", score)
            put("total", total)
        }) { json -> json.getInt("accuracy_rate") }

    suspend fun getCampaign(): Result<List<CampaignNode>> =
        getArray("/learning/campaign") { array ->
            List(array.length()) { index ->
                val json = array.getJSONObject(index)
                val backendExercises = json.optJSONArray("exercises")
                val usesCampaignProgress = backendExercises != null
                CampaignNode(
                    id = json.getString("id"),
                    title = json.getString("title"),
                    description = json.getString("description"),
                    subjectId = json.getString("subject_id"),
                    subjectName = json.getString("subject_name"),
                    progress = if (usesCampaignProgress) json.getDouble("progress").toFloat() else 0f,
                    isUnlocked = if (usesCampaignProgress) json.getBoolean("is_unlocked") else index == 0,
                    stageLabel = json.optString("stage_label", "Fase de estudo"),
                    visualType = json.optString("visual_type", "generic"),
                    exercises = backendExercises?.let { exercises ->
                        List(exercises.length()) { exerciseIndex ->
                            val exercise = exercises.getJSONObject(exerciseIndex)
                            val options = exercise.getJSONArray("options")
                            CampaignExercise(
                                id = exercise.getString("id"),
                                question = exercise.getString("question"),
                                options = List(options.length()) { optionIndex -> options.getString(optionIndex) },
                                correctIndex = exercise.getInt("correct_index"),
                                explanation = exercise.getString("explanation"),
                                visualType = exercise.optString("visual_type", json.optString("visual_type", "generic"))
                            )
                        }
                    } ?: emptyList(),
                    usesCampaignProgress = usesCampaignProgress
                )
            }
        }

    suspend fun getAvatarItems(): Result<List<AvatarItem>> =
        getArray("/learning/avatar/items") { array ->
            List(array.length()) { index ->
                val json = array.getJSONObject(index)
                AvatarItem(
                    id = json.getString("id"),
                    category = json.getString("category"),
                    name = json.getString("name")
                )
            }
        }

    private suspend fun <T> getArray(path: String, parser: (JSONArray) -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            parser(JSONArray(execute(path, "GET", null)))
        }
    }

    private suspend fun <T> getObject(path: String, parser: (JSONObject) -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            parser(JSONObject(execute(path, "GET", null)))
        }
    }

    private suspend fun <T> post(path: String, body: JSONObject, parser: (JSONObject) -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            parser(JSONObject(execute(path, "POST", body)))
        }
    }

    private fun execute(path: String, method: String, body: JSONObject?): String {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
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
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (connection.responseCode !in 200..299) {
            val message = runCatching {
                JSONObject(responseBody).optString("detail", responseBody)
            }.getOrDefault(responseBody)
            error("Erro ${connection.responseCode}: $message")
        }
        return responseBody
    }
}

private fun JSONObject.optNullableInt(name: String): Int? {
    return if (isNull(name)) null else optInt(name)
}
