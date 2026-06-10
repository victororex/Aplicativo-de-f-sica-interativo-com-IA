package com.example.testes.data.api

import com.example.testes.data.local.LocalBackend
import com.example.testes.model.AvatarItem
import com.example.testes.model.CampaignExercise
import com.example.testes.model.CampaignNode
import com.example.testes.model.DailyChallengeStatus
import com.example.testes.model.DailyQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LearningApiClient {
    suspend fun getDailyChallenge(): Result<List<DailyQuestion>> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseDailyQuestions(LocalBackend.dailyChallenge())
            }
        }

    suspend fun getDailyChallengeStatus(): Result<DailyChallengeStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseDailyStatus(LocalBackend.dailyStatus(SessionManager.accessToken))
            }
        }

    suspend fun submitDailyChallenge(score: Int, total: Int): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                LocalBackend.submitDaily(SessionManager.accessToken, score, total).getInt("accuracy_rate")
            }
        }

    suspend fun submitCampaignStage(nodeId: String, score: Int, total: Int): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                LocalBackend.submitCampaign(SessionManager.accessToken, nodeId, score, total).getInt("accuracy_rate")
            }
        }

    suspend fun getCampaign(): Result<List<CampaignNode>> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseCampaign(LocalBackend.campaign(SessionManager.accessToken))
            }
        }

    suspend fun getAvatarItems(): Result<List<AvatarItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val array = LocalBackend.avatarItems()
            List(array.length()) { index ->
                val json = array.getJSONObject(index)
                AvatarItem(
                    id = json.getString("id"),
                    category = json.getString("category"),
                    name = json.getString("name")
                )
            }
        }
    }

    private fun parseDailyQuestions(array: JSONArray): List<DailyQuestion> {
        return List(array.length()) { index ->
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

    private fun parseDailyStatus(json: JSONObject): DailyChallengeStatus {
        return DailyChallengeStatus(
            completedToday = json.getBoolean("completed_today"),
            score = json.optNullableInt("score"),
            total = json.optNullableInt("total"),
            accuracyRate = json.optNullableInt("accuracy_rate"),
            completedAt = if (json.isNull("completed_at")) null else json.optString("completed_at")
        )
    }

    private fun parseCampaign(array: JSONArray): List<CampaignNode> {
        return List(array.length()) { index ->
            val json = array.getJSONObject(index)
            val localExercises = json.optJSONArray("exercises")
            val usesCampaignProgress = localExercises != null
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
                exercises = localExercises?.let { exercises ->
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
}

private fun JSONObject.optNullableInt(name: String): Int? {
    return if (isNull(name)) null else optInt(name)
}
