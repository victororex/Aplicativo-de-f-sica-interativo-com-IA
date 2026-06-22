package com.example.testes.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FormulaImageException(message: String, cause: Throwable? = null) : Exception(message, cause)

object FormulaImageProcessor {
    private const val MAX_SOURCE_BYTES = 25L * 1024 * 1024
    private const val MAX_OUTPUT_BYTES = 6L * 1024 * 1024
    private const val MAX_DIMENSION = 2048

    suspend fun compress(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val declaredSize = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (declaredSize > MAX_SOURCE_BYTES) {
            throw FormulaImageException("A imagem original excede 25 MB.")
        }
        val stagingDirectory = File(context.cacheDir, "formula_staging").apply { mkdirs() }
        val staging = File.createTempFile("source-", ".image", stagingDirectory)
        try {
            resolver.openInputStream(uri)?.use { input ->
                staging.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_SOURCE_BYTES) {
                            throw FormulaImageException("A imagem original excede 25 MB.")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: throw FormulaImageException("Nao foi possivel abrir a imagem.")

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(staging.absolutePath, bounds)
            if (bounds.outWidth < 64 || bounds.outHeight < 64) {
                throw FormulaImageException("A imagem e pequena demais para reconhecer a formula.")
            }

            val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(staging.absolutePath, options)
                ?: throw FormulaImageException("O arquivo selecionado nao e uma imagem valida.")

            val rotation = ExifInterface(staging).rotationDegrees
            val rotated = if (rotation == 0) {
                decoded
            } else {
                Bitmap.createBitmap(
                    decoded,
                    0,
                    0,
                    decoded.width,
                    decoded.height,
                    Matrix().apply { postRotate(rotation.toFloat()) },
                    true
                ).also { decoded.recycle() }
            }
            val scale = minOf(
                1f,
                MAX_DIMENSION.toFloat() / maxOf(rotated.width, rotated.height).toFloat()
            )
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    rotated,
                    (rotated.width * scale).toInt(),
                    (rotated.height * scale).toInt(),
                    true
                ).also { rotated.recycle() }
            } else {
                rotated
            }
            val flattened = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.ARGB_8888)
            Canvas(flattened).apply {
                drawColor(Color.WHITE)
                drawBitmap(resized, 0f, 0f, null)
            }
            resized.recycle()
            val outputWidth = flattened.width
            val outputHeight = flattened.height

            val directory = File(context.cacheDir, "formula_uploads").apply { mkdirs() }
            val output = File.createTempFile("formula-", ".jpg", directory)
            try {
                var quality = 88
                do {
                    FileOutputStream(output, false).use { stream ->
                        if (!flattened.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                            throw FormulaImageException("Falha ao comprimir a imagem.")
                        }
                    }
                    quality -= 8
                } while (output.length() > MAX_OUTPUT_BYTES && quality >= 56)

                if (output.length() > MAX_OUTPUT_BYTES) {
                    output.delete()
                    throw FormulaImageException("Nao foi possivel reduzir a imagem para menos de 6 MB.")
                }
                Log.i(
                    TAG,
                    "Formula image prepared ${outputWidth}x${outputHeight}, ${output.length()} bytes"
                )
                output
            } catch (error: Throwable) {
                output.delete()
                throw error
            } finally {
                if (!flattened.isRecycled) flattened.recycle()
            }
        } finally {
            staging.delete()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > MAX_DIMENSION * 2 || height / sample > MAX_DIMENSION * 2) {
            sample *= 2
        }
        return sample
    }

    private const val TAG = "FormulaImageProcessor"
}
