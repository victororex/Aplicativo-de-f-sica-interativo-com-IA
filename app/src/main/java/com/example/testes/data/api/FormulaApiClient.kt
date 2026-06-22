package com.example.testes.data.api

import com.example.testes.BuildConfig
import android.util.Log
import com.example.testes.model.FormulaAnalysis
import com.example.testes.model.FormulaGraph
import com.example.testes.model.FormulaStep
import com.example.testes.model.GraphPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

class FormulaApiClient {
    suspend fun analyze(image: File, question: String?): Result<FormulaAnalysis> =
        withContext(Dispatchers.IO) {
            runCatching {
                check(BuildConfig.USE_REMOTE_AI) {
                    "Ative USE_REMOTE_AI e inicie o backend para analisar formulas por imagem."
                }
                require(image.exists() && image.length() > 0) { "A imagem selecionada esta vazia." }

                val boundary = "FormulaBoundary${UUID.randomUUID().toString().replace("-", "")}"
                val connection = (URL("${BuildConfig.AI_API_BASE_URL.trimEnd('/')}/formula/analyze")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 120_000
                    doInput = true
                    doOutput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                connection.outputStream.buffered().use { output ->
                    if (!question.isNullOrBlank()) {
                        output.write("--$boundary\r\n".toByteArray())
                        output.write("Content-Disposition: form-data; name=\"question\"\r\n\r\n".toByteArray())
                        output.write(question.trim().toByteArray(StandardCharsets.UTF_8))
                        output.write("\r\n".toByteArray())
                    }
                    output.write("--$boundary\r\n".toByteArray())
                    output.write(
                        "Content-Disposition: form-data; name=\"image\"; filename=\"formula.jpg\"\r\n"
                            .toByteArray()
                    )
                    output.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                    image.inputStream().use { it.copyTo(output) }
                    output.write("\r\n--$boundary--\r\n".toByteArray())
                }

                val code = connection.responseCode
                val raw = readBody(connection, code)
                if (code !in 200..299) {
                    Log.w(TAG, "Formula API returned HTTP $code")
                    throw IllegalStateException(remoteError(raw, code))
                }
                parseAnalysis(JSONObject(raw)).also {
                    Log.i(TAG, "Formula API parsed ${it.steps.size} steps, graph=${it.graph != null}")
                }
            }
        }

    private fun parseAnalysis(json: JSONObject): FormulaAnalysis {
        val stepsJson = json.getJSONArray("steps")
        val graphJson = json.optJSONObject("graph")
        val warningsJson = json.optJSONArray("warnings")
        return FormulaAnalysis(
            contentType = json.optString("content_type", "exercise"),
            visualDescription = json.optString("visual_description"),
            structuredData = json.optJSONArray("structured_data")?.let { array ->
                List(array.length()) { index -> array.optString(index) }
            }.orEmpty(),
            ocrText = json.optString("ocr_text"),
            latex = json.optString("latex"),
            problemStatement = json.optString("problem_statement"),
            steps = List(stepsJson.length()) { index ->
                val step = stepsJson.getJSONObject(index)
                FormulaStep(
                    title = step.optString("title"),
                    explanation = step.optString("explanation"),
                    latex = if (step.isNull("latex")) null else step.optString("latex")
                )
            },
            finalAnswer = json.optString("final_answer"),
            graph = graphJson?.let { graph ->
                val pointsJson = graph.getJSONArray("points")
                FormulaGraph(
                    expression = graph.optString("expression"),
                    label = graph.optString("label"),
                    xMin = graph.optDouble("x_min").toFloat(),
                    xMax = graph.optDouble("x_max").toFloat(),
                    points = List(pointsJson.length()) { index ->
                        val point = pointsJson.getJSONObject(index)
                        GraphPoint(
                            x = point.getDouble("x").toFloat(),
                            y = point.getDouble("y").toFloat()
                        )
                    }
                )
            },
            narrationText = json.optString("narration_text"),
            warnings = if (warningsJson == null) {
                emptyList()
            } else {
                List(warningsJson.length()) { warningsJson.getString(it) }
            }
        )
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
        }.orEmpty()
    }

    private fun remoteError(raw: String, code: Int): String {
        val detail = runCatching { JSONObject(raw).optString("detail") }.getOrNull()
        return detail?.takeIf { it.isNotBlank() }
            ?: "Nao foi possivel analisar a imagem (HTTP $code)."
    }

    companion object {
        private const val TAG = "FormulaApiClient"
    }
}
