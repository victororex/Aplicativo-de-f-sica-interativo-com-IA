package com.example.testes.data.local

import android.content.Context
import com.example.testes.model.MissionContentBlock
import com.example.testes.model.MissionDetail
import com.example.testes.model.MissionQuestion
import com.example.testes.model.MissionTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MissionsRepository(private val context: Context) {

    private val cache = mutableMapOf<String, MissionTrack>()

    suspend fun loadTrack(assetFile: String): Result<MissionTrack> =
        withContext(Dispatchers.IO) {
            runCatching {
                cache[assetFile]?.let { return@runCatching it }
                val text = context.assets.open(assetFile).bufferedReader().use { it.readText() }
                val track = parseTrack(JSONObject(text))
                cache[assetFile] = track
                track
            }
        }

    fun getMission(track: MissionTrack, missionId: String): MissionDetail? =
        track.missions.firstOrNull { it.id == missionId }

    private fun parseTrack(json: JSONObject): MissionTrack {
        val missionsJson = json.getJSONArray("missions")
        val missions = List(missionsJson.length()) { i ->
            parseMission(missionsJson.getJSONObject(i))
        }.sortedBy { it.index }
        return MissionTrack(
            subjectId = json.getString("subjectId"),
            name = json.getString("name"),
            missions = missions
        )
    }

    private fun parseMission(json: JSONObject): MissionDetail = MissionDetail(
        index = json.getInt("index"),
        id = json.getString("id"),
        title = json.getString("title"),
        subtitle = json.optString("subtitle", ""),
        requirements = json.optJSONArray("requirements").toStringList(),
        objectives = json.optJSONArray("objectives").toStringList(),
        contentBlocks = json.optJSONArray("contentBlocks").mapObjects { parseContentBlock(it) },
        questions = json.optJSONArray("questions").mapObjects { parseQuestion(it) }
    )

    private fun parseContentBlock(json: JSONObject): MissionContentBlock {
        val id = json.getString("id")
        val title = json.optString("title", "")
        return when (json.optString("type", "text")) {
            "list" -> MissionContentBlock.BulletList(
                id = id,
                title = title,
                items = json.optJSONArray("items").toStringList()
            )
            else -> MissionContentBlock.Text(
                id = id,
                title = title,
                body = json.optString("body", "")
            )
        }
    }

    private fun parseQuestion(json: JSONObject): MissionQuestion {
        val id = json.getString("id")
        val statement = json.getString("statement")
        val explanation = json.optString("explanation", "")
        return when (json.optString("type", "multiple_choice")) {
            "multiple_choice" -> MissionQuestion.MultipleChoice(
                id = id,
                statement = statement,
                options = json.getJSONArray("options").toStringList(),
                correctIndex = json.getInt("correctIndex"),
                explanation = explanation
            )
            "true_false" -> MissionQuestion.TrueFalse(
                id = id,
                statement = statement,
                correct = json.getBoolean("correct"),
                explanation = explanation
            )
            "numeric" -> MissionQuestion.Numeric(
                id = id,
                statement = statement,
                answer = json.getDouble("answer"),
                tolerance = json.optDouble("tolerance", 0.0),
                unit = json.optString("unit", ""),
                explanation = explanation
            )
            "open" -> MissionQuestion.Open(
                id = id,
                statement = statement,
                placeholder = json.optString("placeholder", ""),
                explanation = explanation
            )
            "multi_step" -> MissionQuestion.MultiStep(
                id = id,
                statement = statement,
                steps = json.optJSONArray("steps").toStringList(),
                explanation = explanation
            )
            else -> MissionQuestion.Open(
                id = id,
                statement = statement,
                placeholder = "",
                explanation = explanation
            )
        }
    }

    companion object {
        const val DIMENSIONAL_ANALYSIS_ASSET = "missions_dimensional_analysis.json"
        const val DIMENSIONAL_SUBJECT_ID = "analise-dimensional"
        const val DIMENSIONAL_NAME = "Análise Dimensional"
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { i -> getString(i) }
}

private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return List(length()) { i -> transform(getJSONObject(i)) }
}
