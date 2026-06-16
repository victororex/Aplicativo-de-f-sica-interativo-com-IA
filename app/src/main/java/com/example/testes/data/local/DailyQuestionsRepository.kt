package com.example.testes.data.local

import android.content.Context
import com.example.testes.model.DailyDimensionalQuestion
import com.example.testes.model.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DailyQuestionsRepository(private val context: Context) {

    @Volatile
    private var cached: List<DailyDimensionalQuestion>? = null

    suspend fun getAllDailyQuestions(): List<DailyDimensionalQuestion> =
        withContext(Dispatchers.IO) {
            cached?.let { return@withContext it }
            val text = context.assets.open(DAILY_QUESTIONS_ASSET).bufferedReader().use { it.readText() }
            val arr = JSONObject(text).getJSONArray("questions")
            val parsed = List(arr.length()) { i ->
                val q = arr.getJSONObject(i)
                val rangeJson = q.getJSONArray("missionIndexRange")
                val tagsJson = q.optJSONArray("tags")
                DailyDimensionalQuestion(
                    id = q.getString("id"),
                    difficulty = Difficulty.valueOf(q.getString("difficulty")),
                    missionIndexRange = List(rangeJson.length()) { idx -> rangeJson.getInt(idx) },
                    statement = q.getString("statement"),
                    options = q.getJSONArray("options").let { opts ->
                        List(opts.length()) { idx -> opts.getString(idx) }
                    },
                    correctIndex = q.getInt("correctIndex"),
                    tags = if (tagsJson == null) emptyList()
                        else List(tagsJson.length()) { idx -> tagsJson.getString(idx) }
                )
            }
            cached = parsed
            parsed
        }

    companion object {
        const val DAILY_QUESTIONS_ASSET = "daily_dimensional_questions.json"
    }
}
