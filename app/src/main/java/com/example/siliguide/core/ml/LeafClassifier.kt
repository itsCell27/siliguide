package com.example.siliguide.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

class LeafClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    private var inputWidth: Int = 224
    private var inputHeight: Int = 224
    private var numClasses: Int = 4

    private val labels = listOf(
        "leaf_curling",
        "healthy",
        "leaf_spots",
        "yellowing"
    )

    init {
        try {
            val fileDescriptor = context.assets.openFd("models/siliguide_model2.3_v3small_finetuned.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel

            val buffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )

            inputStream.close()
            fileDescriptor.close()

            val options = Interpreter.Options()
            val tempInterpreter = Interpreter(buffer, options)

            val inputShape = tempInterpreter.getInputTensor(0).shape()
            inputHeight = inputShape[1]
            inputWidth = inputShape[2]

            val outputShape = tempInterpreter.getOutputTensor(0).shape()
            numClasses = outputShape[1]

            interpreter = tempInterpreter
            Log.d("LeafClassifier", "Model loaded. Input: ${inputWidth}x${inputHeight}, Output: $numClasses classes")
        } catch (e: Exception) {
            Log.e("LeafClassifier", "Error loading model", e)
        }
    }

    fun predict(bitmap: Bitmap): Prediction {
        val currentInterpreter = interpreter
            ?: throw IllegalStateException("Interpreter is closed or not initialized")

        Log.d("LeafClassifier", "Starting prediction...")
        val input = preprocess(bitmap)
        val output = Array(1) { FloatArray(numClasses) }

        currentInterpreter.run(input, output)

        val scores = output[0]
        Log.d("LeafClassifier", "Raw scores: ${scores.toList()}")

        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0

        val result = Prediction(
            label = labels[maxIndex],
            confidence = scores[maxIndex],
            allScores = scores
        )
        Log.d("LeafClassifier", "Prediction: ${result.label} (${(result.confidence * 100).toInt()}%)")
        return result
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        // FIX 3: Ensure ARGB_8888 before reading pixels
        val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        val resized = argbBitmap.scale(inputWidth, inputHeight)

        val input = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
        input.order(ByteOrder.nativeOrder())
        // FIX 3 (KEY FIX): rewind before filling so the model reads from position 0
        input.rewind()

        val intValues = IntArray(inputWidth * inputHeight)
        resized.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            // Raw 0-255 — MobileNetV3Small expects unscaled pixel values
            input.putFloat(r)
            input.putFloat(g)
            input.putFloat(b)
        }

        // Recycle intermediate bitmaps
        if (resized != bitmap && resized != argbBitmap) resized.recycle()
        if (argbBitmap != bitmap) argbBitmap.recycle()

        // FIX 3 (KEY FIX): rewind before returning so interpreter reads from the start
        input.rewind()
        return input
    }

    fun close() {
        Log.d("LeafClassifier", "Closing interpreter")
        interpreter?.close()
        interpreter = null
    }
}