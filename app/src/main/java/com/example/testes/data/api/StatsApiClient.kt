package com.example.testes.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ImprovementStatsResponse(
    val accuracyRate: Int,
    val studyQuality: String,
    val studiedSeconds: Int,
    val questionsAsked: Int,
    val completedPhases: Int,
    val totalPhases: Int
) {
    val studiedTimeLabel: String
        get() {
            val hours = studiedSeconds / 3600
            val minutes = (studiedSeconds % 3600) / 60
            return "${hours}h ${minutes}min"
        }
}

class StatsApiClient(
    private val baseUrl: String = "http://10.0.2.2:8000"
) {
    suspend fun getImprovementStats(): Result<ImprovementStatsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$baseUrl/stats/improvement")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                SessionManager.accessToken?.let { token ->
                    setRequestProperty("Authorization", "Bearer $token")
                }
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
            ImprovementStatsResponse(
                accuracyRate = json.optInt("accuracy_rate"),
                studyQuality = json.optString("study_quality", "Sem dados"),
                studiedSeconds = json.optInt("studied_seconds"),
                questionsAsked = json.optInt("questions_asked"),
                completedPhases = json.optInt("completed_phases"),
                totalPhases = json.optInt("total_phases", 20)
            )
        }
    }
}
