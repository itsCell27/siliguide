package com.example.siliguide.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.core.graphics.scale
import kotlin.math.abs

class HeatmapGenerator(private val context: Context) {

    // We pass the specific CSV filename to this function
    private fun loadWeights(csvFilename: String): Array<FloatArray>? {
        return try {
            val inputStream = context.assets.open(csvFilename)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()

            // We can dynamically find the number of classes by looking at the first line
            val numClasses = lines[0].split(",").size

            val weightsMatrix = Array(576) { FloatArray(numClasses) }

            lines.forEachIndexed { rowIndex, line ->
                val values = line.split(",")
                for (colIndex in 0 until numClasses) {
                    weightsMatrix[rowIndex][colIndex] = values[colIndex].toFloat()
                }
            }
            Log.d("HeatmapGenerator", "Loaded $csvFilename successfully! (576 x $numClasses)")
            weightsMatrix
        } catch (e: Exception) {
            Log.e("HeatmapGenerator", "Failed to load $csvFilename", e)
            null
        }
    }

    suspend fun generateHeatmap(
        featureMaps: Array<Array<Array<FloatArray>>>?,
        predictedClassIndex: Int,
        csvFilename: String // <-- NEW: Tell it which weights to use!
    ): Bitmap? = withContext(Dispatchers.Default) {

        if (featureMaps == null) return@withContext null

        // 1. Load the correct weights for this specific model
        val weightsMatrix = loadWeights(csvFilename) ?: return@withContext null

        val grid = featureMaps[0]
        val cam = Array(7) { FloatArray(7) }
        var maxCam = 0f

        // 2. Matrix Multiplication & ReLU
        for (y in 0 until 7) {
            for (x in 0 until 7) {
                var sum = 0f
                for (channel in 0 until 576) {
                    sum += grid[y][x][channel] * weightsMatrix[channel][predictedClassIndex]
                }
                if (sum < 0) sum = 0f
                cam[y][x] = sum

                if (sum > maxCam) maxCam = sum
            }
        }

        // 3. Normalize and Convert to Colors
        val tinyBitmap = createBitmap(7, 7)

        for (y in 0 until 7) {
            for (x in 0 until 7) {
                val normalizedVal = if (maxCam > 0) cam[y][x] / maxCam else 0f
                val color = valueToJetColor(normalizedVal)
                tinyBitmap[x, y] = color
            }
        }

        // 4. Scale to 224x224
        val smoothHeatmap = tinyBitmap.scale(224, 224)
        tinyBitmap.recycle()

        return@withContext smoothHeatmap
    }

    private fun valueToJetColor(v: Float): Int {
        val r = (1.5 - abs((4 * v - 3).toDouble())).coerceIn(0.0, 1.0).toFloat()
        val g = (1.5 - abs((4 * v - 2).toDouble())).coerceIn(0.0, 1.0).toFloat()
        val b = (1.5 - abs((4 * v - 1).toDouble())).coerceIn(0.0, 1.0).toFloat()

        return Color.argb(
            255,
            (r * 255).toInt(),
            (g * 255).toInt(),
            (b * 255).toInt()
        )
    }
}