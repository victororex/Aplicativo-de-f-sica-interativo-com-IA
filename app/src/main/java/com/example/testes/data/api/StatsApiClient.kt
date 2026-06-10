package com.example.testes.data.api

import com.example.testes.data.local.LocalBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

class StatsApiClient {
    suspend fun getImprovementStats(): Result<ImprovementStatsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val json = LocalBackend.improvementStats(SessionManager.accessToken)
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
