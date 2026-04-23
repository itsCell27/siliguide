package com.example.siliguide.core.image

import android.graphics.Bitmap
import kotlin.math.pow

object BlurDetector {

    fun isBlurry(bitmap: Bitmap, threshold: Double = 60.0): Boolean {
        val gray = toGrayscale(bitmap)
        val laplacian = applyLaplacian(gray)
        val variance = calculateVariance(laplacian)

        return variance < threshold
    }

    // Convert to grayscale
    private fun toGrayscale(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height

        val gray = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                gray[y][x] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }
        return gray
    }

    // Apply Laplacian filter
    private fun applyLaplacian(gray: Array<IntArray>): Array<IntArray> {
        val height = gray.size
        val width = gray[0].size

        val result = Array(height) { IntArray(width) }

        val kernel = arrayOf(
            intArrayOf(0, 1, 0),
            intArrayOf(1, -4, 1),
            intArrayOf(0, 1, 0)
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        sum += gray[y + ky][x + kx] * kernel[ky + 1][kx + 1]
                    }
                }

                result[y][x] = sum
            }
        }

        return result
    }

    // Optimized Variance calculation to prevent OOM
    private fun calculateVariance(data: Array<IntArray>): Double {
        var sum = 0.0
        var count = 0L

        for (row in data) {
            for (value in row) {
                sum += value
                count++
            }
        }

        if (count == 0L) return 0.0
        val mean = sum / count

        var sumSqDiff = 0.0
        for (row in data) {
            for (value in row) {
                val diff = value - mean
                sumSqDiff += diff * diff
            }
        }

        return sumSqDiff / count
    }
}