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

    // FIX 2: Variables to track which output is which
    private var predictionOutputIndex: Int = 0
    private var featureMapOutputIndex: Int = 1

    private val labels = listOf(
        "curling",
        "healthy",
        "spots",
        "yellowing"
    )

    init {
        try {
            val fileDescriptor = context.assets.openFd("models/leaf_classifier_model2.5_dual_output_v3small.tflite")
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

            // FIX 2 (Continued): Dynamically map the outputs based on shape
            val shape0 = tempInterpreter.getOutputTensor(0).shape()
            if (shape0.size == 2) {
                // Shape is [1, 4] -> Prediction Probabilities
                predictionOutputIndex = 0
                featureMapOutputIndex = 1
                numClasses = shape0[1]
            } else {
                // Shape is [1, 7, 7, 576] -> Feature Maps
                predictionOutputIndex = 1
                featureMapOutputIndex = 0
                numClasses = tempInterpreter.getOutputTensor(1).shape()[1]
            }

            interpreter = tempInterpreter
            Log.d("LeafClassifier", "Dual-Output Classifier loaded. Input: ${inputWidth}x${inputHeight}, Output: $numClasses classes")
        } catch (e: Exception) {
            Log.e("LeafClassifier", "Error loading model", e)
        }
    }

    fun predict(bitmap: Bitmap): Prediction {
        val currentInterpreter = interpreter
            ?: throw IllegalStateException("Interpreter is closed or not initialized")

        Log.d("LeafClassifier", "Starting dual prediction...")

        return try {
            val input = preprocess(bitmap)

            // FIX 3: Setup Multiple Outputs
            val inputs = arrayOf(input)

            // Buffer for Output 0: [1 batch, 4 classes]
            val predictionsBuffer = Array(1) { FloatArray(numClasses) }

            // Buffer for Output 1: [1 batch, 7 height, 7 width, 576 channels]
            val featureMapsBuffer = Array(1) { Array(7) { Array(7) { FloatArray(576) } } }

            // Map the buffers to the correct TFLite output indices
            val outputs = mutableMapOf<Int, Any>()
            outputs[predictionOutputIndex] = predictionsBuffer
            outputs[featureMapOutputIndex] = featureMapsBuffer

            // RUN DUAL INFERENCE
            currentInterpreter.runForMultipleInputsOutputs(inputs, outputs)

            // Extract Results
            val scores = predictionsBuffer[0]
            Log.d("LeafClassifier", "Raw scores: ${scores.toList()}")

            val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0

            val result = Prediction(
                label = labels[maxIndex],
                confidence = scores[maxIndex],
                allScores = scores,
                featureMaps = featureMapsBuffer // Passing the visual data!
            )
            Log.d("LeafClassifier", "Prediction: ${result.label} (${(result.confidence * 100).toInt()}%)")
            result

        } catch (e: Exception) {
            Log.e("LeafClassifier", "Error during prediction execution", e)
            Prediction("Error", 0f, FloatArray(numClasses))
        }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        // 1. Find the shortest edge to make a perfect square
        val dimension = Math.min(bitmap.width, bitmap.height)

        // 2. Calculate the starting X and Y to crop from the exact center
        val xOffset = (bitmap.width - dimension) / 2
        val yOffset = (bitmap.height - dimension) / 2

        // 3. Create the squared image
        val squaredBitmap = Bitmap.createBitmap(
            bitmap,
            xOffset,
            yOffset,
            dimension,
            dimension
        )

        // Ensure ARGB_8888 before reading pixels
        val argbBitmap = if (squaredBitmap.config != Bitmap.Config.ARGB_8888) {
            squaredBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            squaredBitmap
        }

        val resized = argbBitmap.scale(inputWidth, inputHeight)

        val input = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
        input.order(ByteOrder.nativeOrder())
        // rewind before filling so the model reads from position 0
        input.rewind()

        val intValues = IntArray(inputWidth * inputHeight)
        resized.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            if (pixel == intValues[0]) {
                Log.d("SiliGuide_Debug", "Pixel[0]: R=$r, G=$g, B=$b")
            }

            // Raw 0-255 — MobileNetV3Small expects unscaled pixel values
            input.putFloat(r)
            input.putFloat(g)
            input.putFloat(b)
        }

        // Clean up memory to prevent OutOfMemory (OOM) crashes
        if (squaredBitmap != bitmap) squaredBitmap.recycle()
        if (argbBitmap != squaredBitmap && argbBitmap != bitmap) argbBitmap.recycle()
        if (resized != argbBitmap && resized != bitmap) resized.recycle()

        // rewind before returning so interpreter reads from the start
        input.rewind()
        return input
    }

    fun close() {
        Log.d("LeafClassifier", "Closing interpreter")
        interpreter?.close()
        interpreter = null
    }
}